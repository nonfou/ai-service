package com.nonfou.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.service.CostCalculatorService.CostResult;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
import com.nonfou.github.service.proxy.ModelProxy;
import com.nonfou.github.util.SessionUtil;
import com.nonfou.github.util.TokenEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenRouter 代理服务，用于把上游请求转发到 OpenRouter 官方 API。
 */
@Slf4j
@Service
public class OpenRouterProxyService implements ModelProxy {

    private final RestTemplate restTemplate;
    private final BackendAccountService backendAccountService;
    private final ObjectMapper objectMapper;
    private final TokenEstimator tokenEstimator;
    private final TaskExecutor streamTaskExecutor;

    @Autowired
    public OpenRouterProxyService(RestTemplate restTemplate,
                                  BackendAccountService backendAccountService,
                                  ObjectMapper objectMapper,
                                  TokenEstimator tokenEstimator,
                                  @Qualifier("streamTaskExecutor") TaskExecutor streamTaskExecutor) {
        this.restTemplate = restTemplate;
        this.backendAccountService = backendAccountService;
        this.objectMapper = objectMapper;
        this.tokenEstimator = tokenEstimator;
        this.streamTaskExecutor = streamTaskExecutor;
    }

    @Autowired
    private AccountSchedulerService accountScheduler;

    @Autowired
    private SessionStickinessService sessionStickinessService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private CostCalculatorService costCalculatorService;

    @Value("${backend.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${spring.application.name:cc-web}")
    private String appName;

    @Value("${server.domain:http://localhost:8080}")
    private String appUrl;

    @Value("${backend.streaming.timeout-ms:300000}")
    private long streamTimeoutMs;

    @Override
    public String getProvider() {
        return "openrouter";
    }

    /**
     * 同步聊天入口，供 ChatController 调用。
     */
    @Override
    public ChatResponse chat(ChatRequest request, ApiKey apiKey) {
        // 1. 生成会话哈希，便于做会话粘性路由
        String sessionHash = SessionUtil.generateSessionHash(
            apiKey.getApiKey(),
            request.getModel(),
            request.getMessages()
        );

        // 2. 依据会话哈希挑选最合适的后端账号
        BackendAccount account = accountScheduler.selectAccount(apiKey, request.getModel(), sessionHash);
        if (account == null) {
            throw new RuntimeException("No available OpenRouter account");
        }

        // 3. 检查用户配额是否仍可用
        quotaService.checkAndEnforceQuota(apiKey.getUserId());

        // 4. 组装 OpenRouter /chat/completions 请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel());
        requestBody.put("messages", request.getMessages());
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        requestBody.put("stream", false);

        try {
            // 5. 调用 OpenRouter
            log.info("Calling OpenRouter API: account={}, model={}, messages={}",
                    account.getAccountName(), request.getModel(), request.getMessages().size());

            Map<String, Object> responseMap = chatNonStream(
                    backendAccountService.getDecryptedToken(account.getId()),
                    requestBody
            );

            // 6. 将响应转成内部 ChatResponse
            ChatResponse chatResponse = objectMapper.convertValue(responseMap, ChatResponse.class);

            // 7. 如果返回 usage，则依据模型价格计算成本
            if (chatResponse.getUsage() != null) {
                TokenUsage usage = TokenUsage.builder()
                        .inputTokens(chatResponse.getUsage().getPromptTokens())
                        .outputTokens(chatResponse.getUsage().getCompletionTokens())
                        .cacheReadTokens(0)
                        .cacheWriteTokens(0)
                        .build();

                CostResult costResult = costCalculatorService.calculate(
                        usage, request.getModel(), account.getId(), apiKey.getUserId());

                // 8. 从配额中扣除本次调用成本
                quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());

                log.info("OpenRouter API : account={}, tokens={}, cost=${}",
                        account.getAccountName(),
                        chatResponse.getUsage().getTotalTokens(),
                        costResult.getTotalCost());
            }

            // 9. 标记账号调用成功
            backendAccountService.recordSuccess(account.getId());

            // 10. 记录会话与账号的粘性映射
            sessionStickinessService.saveMapping(sessionHash, account.getId());

            return chatResponse;

        } catch (Exception e) {
            // 11. 失败场景要计入 error 并抛出
            backendAccountService.recordError(account.getId(), e.getMessage());
            log.error("OpenRouter API call failed for account {}", account.getAccountName(), e);
            throw new RuntimeException("OpenRouter service error: " + e.getMessage());
        }
    }

    /**
     * Streaming chat entry point used by ChatController.
     */
    @Override
    public SseEmitter chatStream(ChatRequest request, ApiKey apiKey) {
        SseEmitter emitter = new SseEmitter(streamTimeoutMs);

        String sessionHash = SessionUtil.generateSessionHash(
                apiKey.getApiKey(),
                request.getModel(),
                request.getMessages()
        );

        BackendAccount account;
        try {
            account = accountScheduler.selectAccount(apiKey, request.getModel(), sessionHash);
            if (account == null) {
                emitter.completeWithError(new RuntimeException("No available OpenRouter account"));
                return emitter;
            }

            quotaService.checkAndEnforceQuota(apiKey.getUserId());
        } catch (Exception e) {
            log.error("Failed to select OpenRouter account for streaming request", e);
            emitter.completeWithError(e);
            return emitter;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModel());
        requestBody.put("messages", request.getMessages());
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        requestBody.put("stream", true);

        StreamingResources resources = new StreamingResources();
        registerEmitterCallbacks(emitter, resources);

        streamTaskExecutor.execute(() -> {
            AtomicReference<TokenUsage> usageRef = new AtomicReference<>();
            StringBuilder contentBuilder = new StringBuilder();

            try {
                String decryptedToken = backendAccountService.getDecryptedToken(account.getId());
                HttpURLConnection connection = buildOpenRouterConnection(decryptedToken);
                resources.connection = connection;

                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                connection.getOutputStream().write(requestBodyJson.getBytes(StandardCharsets.UTF_8));
                connection.getOutputStream().flush();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    String errorMessage = "OpenRouter returned HTTP " + responseCode;
                    backendAccountService.recordError(account.getId(), errorMessage);
                    emitter.completeWithError(new RuntimeException(errorMessage));
                    return;
                }

                resources.reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = resources.reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6);

                    if ("[DONE]".equals(data.trim())) {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        break;
                    }

                    emitter.send(SseEmitter.event().data(data));
                    handleStreamChunk(data, contentBuilder, usageRef);
                }

                finalizeOpenRouterStream(request, apiKey, account, sessionHash, contentBuilder.toString(), usageRef.get());
                emitter.complete();
            } catch (Exception e) {
                log.error("OpenRouter streaming loop failed", e);
                backendAccountService.recordError(account.getId(), e.getMessage());
                emitter.completeWithError(e);
            } finally {
                closeResources(resources);
            }
        });

        return emitter;
    }

    /**
     * 以底层账号调用 OpenRouter API。
     *
     * @param account     已选中的后端账号
     * @param requestBody OpenRouter 请求参数
     * @param stream      是否开启流式返回
     * @return 非流式返回响应 Map，流式返回 SseEmitter
     */
    public Object chat(BackendAccount account, Map<String, Object> requestBody, boolean stream) {
        String decryptedKey = backendAccountService.getDecryptedToken(account.getId());
        if (decryptedKey == null) {
            throw new RuntimeException("OpenRouter API key is missing");
        }

        try {
            if (stream) {
                return chatStream(decryptedKey, requestBody);
            } else {
                return chatNonStream(decryptedKey, requestBody);
            }
        } catch (Exception e) {
            log.error("OpenRouter request failed for accountId={}, error={}", account.getId(), e.getMessage(), e);
            backendAccountService.incrementErrorCount(account.getId(), e.getMessage());
            throw new RuntimeException("OpenRouter request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构建访问 OpenRouter 所需的通用请求头。
     */
    /**
     * 调用 OpenRouter 非流式聊天接口。
     */
    private Map<String, Object> chatNonStream(String apiKey, Map<String, Object> requestBody) throws Exception {
        HttpHeaders headers = createHeaders(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = baseUrl + "/chat/completions";
        log.debug("OpenRouter request url={}", url);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("OpenRouter responded successfully");
            return response.getBody();
        } else {
            throw new RuntimeException("OpenRouter request failed: " + response.getStatusCode());
        }
    }

    /**
     * 调用 OpenRouter 非流式聊天接口。
     */
    /**
     * 调用 OpenRouter 流式聊天接口。
     */
    private SseEmitter chatStream(String apiKey, Map<String, Object> requestBody) throws Exception {
        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时，和上游保持一致

        // OpenRouter SSE 需要显式打开 stream
        requestBody.put("stream", true);

        // 使用独立线程持续读取 SSE，避免阻塞调用方
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                String url = baseUrl + "/chat/completions";
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();

                // 设置 HTTP 请求头
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("HTTP-Referer", appUrl);
                connection.setRequestProperty("X-Title", appName);
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(300000);

                // 写入请求体
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                connection.getOutputStream().write(requestBodyJson.getBytes(StandardCharsets.UTF_8));
                connection.getOutputStream().flush();

                // 检查 HTTP 响应码
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    String errorMessage = "OpenRouter returned HTTP " + responseCode;
                    log.error(errorMessage);
                    emitter.completeWithError(new RuntimeException(errorMessage));
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);

                        // 读取结束标记
                        if ("[DONE]".equals(data.trim())) {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            break;
                        }

                        // 透传 SSE 数据
                        try {
                            // 确保数据是合法 JSON
                            objectMapper.readTree(data);
                            emitter.send(SseEmitter.event().data(data));
                        } catch (Exception e) {
                            log.warn("Invalid SSE payload {}", data);
                        }
                    }
                }

                emitter.complete();
                log.debug("OpenRouter streaming connection closed");

            } catch (Exception e) {
                log.error("OpenRouter streaming request failed", e);
                emitter.completeWithError(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    log.warn("Failed to close streaming connection", e);
                }
            }
        }).start();

        return emitter;
    }

    /**
     * 构建访问 OpenRouter 所需的通用请求头。
     */
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", appUrl);
        headers.set("X-Title", appName);
        return headers;
    }

    /**
     * 获取 OpenRouter 提供的模型列表。
     */
    public Map<String, Object> getModels(BackendAccount account) {
        String decryptedKey = backendAccountService.getDecryptedToken(account.getId());
        if (decryptedKey == null) {
            throw new RuntimeException("OpenRouter API key is missing");
        }

        try {
            HttpHeaders headers = createHeaders(decryptedKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = baseUrl + "/models";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("OpenRouter models request failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to fetch OpenRouter models", e);
            throw new RuntimeException("OpenRouter models request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定模型的价格信息。
     */
    public Map<String, Object> getModelPricing(String modelName) {
        try {
            // 尝试读取 OpenRouter 官方实时价格
            String url = baseUrl + "/models/" + modelName;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to load OpenRouter pricing for model={}", modelName, e);
        }

        return Map.of(
                "model", modelName,
                "pricing", Map.of(
                        "prompt", 0.0,
                        "completion", 0.0
                ),
                "note", "Pricing info unavailable, using defaults"
        );
    }

    /**
     * 调用 OpenRouter 模型接口以执行健康检查。
     */
    public boolean healthCheck(BackendAccount account) {
        String decryptedKey = backendAccountService.getDecryptedToken(account.getId());
        if (decryptedKey == null) {
            return false;
        }

        try {
            // 通过真实 API Key 调用 /models 确认链路可用
            HttpHeaders headers = createHeaders(decryptedKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = baseUrl + "/models";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            if (isHealthy) {
                backendAccountService.resetErrorCount(account.getId());
            }

            return isHealthy;
        } catch (Exception e) {
            log.warn("OpenRouter account operation failed accountId={}", account.getId(), e);
            backendAccountService.incrementErrorCount(account.getId(), e.getMessage());
            return false;
        }
    }

    private HttpURLConnection buildOpenRouterConnection(String token) throws Exception {
        String url = baseUrl + "/chat/completions";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("HTTP-Referer", appUrl);
        connection.setRequestProperty("X-Title", appName);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout((int) streamTimeoutMs);
        return connection;
    }

    private void handleStreamChunk(String data,
                                   StringBuilder contentBuilder,
                                   AtomicReference<TokenUsage> usageRef) {
        try {
            JsonNode node = objectMapper.readTree(data);
            if (node.has("usage")) {
                Map<String, Object> usageMap = objectMapper.convertValue(node.get("usage"), Map.class);
                usageRef.set(TokenUsage.from(usageMap));
            }

            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray()) {
                for (JsonNode choice : choices) {
                    JsonNode delta = choice.get("delta");
                    if (delta != null) {
                        JsonNode contentNode = delta.get("content");
                        if (contentNode != null && !contentNode.isNull()) {
                            contentBuilder.append(contentNode.asText(""));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("OpenRouter: {}", data);
        }
    }

    private void finalizeOpenRouterStream(ChatRequest request,
                                          ApiKey apiKey,
                                          BackendAccount account,
                                          String sessionHash,
                                          String fullContent,
                                          TokenUsage usage) {
        try {
            TokenUsage finalUsage = usage != null ? usage : tokenEstimator.estimateUsage(request, fullContent);
            if (finalUsage == null) {
                log.warn("OpenRouter usage record missing account={}, model={}",
                        account.getAccountName(), request.getModel());
            } else {
                CostResult costResult = costCalculatorService.calculate(
                        finalUsage, request.getModel(), account.getId(), apiKey.getUserId());
                quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());
                log.info("OpenRouter streaming usage: account={}, tokens(in/out)={}/{}, cost=${}",
                        account.getAccountName(),
                        finalUsage.getInputTokens(),
                        finalUsage.getOutputTokens(),
                        costResult.getTotalCost());
            }

            backendAccountService.recordSuccess(account.getId());
            sessionStickinessService.saveMapping(sessionHash, account.getId());
        } catch (Exception e) {
            log.error("OpenRouter streaming accounting failed for account={}", account.getAccountName(), e);
            throw new RuntimeException("OpenRouter service error: " + e.getMessage(), e);
        }
    }

    private void registerEmitterCallbacks(SseEmitter emitter, StreamingResources resources) {
        emitter.onCompletion(() -> closeResources(resources));
        emitter.onTimeout(() -> closeResources(resources));
        emitter.onError(error -> closeResources(resources));
    }

    private void closeResources(StreamingResources resources) {
        if (resources == null) {
            return;
        }

        try {
            if (resources.reader != null) {
                resources.reader.close();
            }
        } catch (Exception e) {
            log.warn("Failed to close OpenRouter stream reader", e);
        }

        if (resources.connection != null) {
            try {
                resources.connection.disconnect();
            } catch (Exception e) {
                log.warn("Failed to close OpenRouter connection", e);
            }
        }
    }

    private static class StreamingResources {
        private volatile HttpURLConnection connection;
        private volatile BufferedReader reader;
    }
}
