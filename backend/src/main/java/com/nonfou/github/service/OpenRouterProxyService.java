package com.nonfou.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.service.CostCalculatorService.CostResult;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
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
 * OpenRouter 代理服务
 * 负责将请求转发到 OpenRouter API
 */
@Slf4j
@Service
public class OpenRouterProxyService {

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

    /**
     * 非流式聊天（新签名 - 用于 ChatController）
     */
    public ChatResponse chat(ChatRequest request, ApiKey apiKey) {
        // 1. 生成会话哈希（用于会话粘性）
        String sessionHash = SessionUtil.generateSessionHash(
            apiKey.getApiKey(),
            request.getModel(),
            request.getMessages()
        );

        // 2. 选择后端账户
        BackendAccount account = accountScheduler.selectAccount(apiKey, request.getModel(), sessionHash);
        if (account == null) {
            throw new RuntimeException("No available OpenRouter account");
        }

        // 3. 检查配额
        quotaService.checkAndEnforceQuota(apiKey.getUserId());

        // 4. 构建请求体
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
            // 5. 发送请求
            log.info("调用 OpenRouter API: account={}, model={}, messages={}",
                    account.getAccountName(), request.getModel(), request.getMessages().size());

            Map<String, Object> responseMap = chatNonStream(
                    backendAccountService.getDecryptedToken(account.getId()),
                    requestBody
            );

            // 6. 转换响应
            ChatResponse chatResponse = objectMapper.convertValue(responseMap, ChatResponse.class);

            // 7. 计算成本
            if (chatResponse.getUsage() != null) {
                TokenUsage usage = TokenUsage.builder()
                        .inputTokens(chatResponse.getUsage().getPromptTokens())
                        .outputTokens(chatResponse.getUsage().getCompletionTokens())
                        .cacheReadTokens(0)
                        .cacheWriteTokens(0)
                        .build();

                CostResult costResult = costCalculatorService.calculate(
                        usage, request.getModel(), account.getId(), apiKey.getUserId());

                // 8. 扣除配额
                quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());

                log.info("OpenRouter API 响应成功: account={}, tokens={}, cost=${}",
                        account.getAccountName(),
                        chatResponse.getUsage().getTotalTokens(),
                        costResult.getTotalCost());
            }

            // 9. 更新账户使用统计
            backendAccountService.recordSuccess(account.getId());

            // 10. 保存会话粘性
            sessionStickinessService.saveMapping(sessionHash, account.getId());

            return chatResponse;

        } catch (Exception e) {
            // 11. 记录失败
            backendAccountService.recordError(account.getId(), e.getMessage());
            log.error("调用 OpenRouter API 失败: account={}", account.getAccountName(), e);
            throw new RuntimeException("OpenRouter 服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 流式聊天（新签名 - 用于 ChatController）
     */
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
            log.error("准备 OpenRouter 流式请求失败", e);
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
                    String errorMessage = "OpenRouter 流式请求失败: HTTP " + responseCode;
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
                log.error("OpenRouter 流式请求异常", e);
                backendAccountService.recordError(account.getId(), e.getMessage());
                emitter.completeWithError(e);
            } finally {
                closeResources(resources);
            }
        });

        return emitter;
    }

    /**
     * 转发聊天请求到 OpenRouter
     *
     * @param account     后端账户
     * @param requestBody 请求体
     * @param stream      是否流式输出
     * @return 响应对象或 SseEmitter
     */
    public Object chat(BackendAccount account, Map<String, Object> requestBody, boolean stream) {
        String decryptedKey = backendAccountService.getDecryptedToken(account.getId());
        if (decryptedKey == null) {
            throw new RuntimeException("无法解密 OpenRouter API Key");
        }

        try {
            if (stream) {
                return chatStream(decryptedKey, requestBody);
            } else {
                return chatNonStream(decryptedKey, requestBody);
            }
        } catch (Exception e) {
            log.error("OpenRouter 请求失败: accountId={}, error={}", account.getId(), e.getMessage(), e);
            // 增加错误计数
            backendAccountService.incrementErrorCount(account.getId(), e.getMessage());
            throw new RuntimeException("OpenRouter 请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 非流式聊天
     */
    private Map<String, Object> chatNonStream(String apiKey, Map<String, Object> requestBody) throws Exception {
        HttpHeaders headers = createHeaders(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = baseUrl + "/chat/completions";
        log.debug("发送 OpenRouter 请求: url={}", url);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("OpenRouter 响应成功");
            return response.getBody();
        } else {
            throw new RuntimeException("OpenRouter 返回错误: " + response.getStatusCode());
        }
    }

    /**
     * 流式聊天
     */
    private SseEmitter chatStream(String apiKey, Map<String, Object> requestBody) throws Exception {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 确保请求体包含 stream: true
        requestBody.put("stream", true);

        // 在新线程中处理流式响应
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                String url = baseUrl + "/chat/completions";
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();

                // 设置请求头
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

                // 读取响应
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    String errorMessage = "OpenRouter 流式请求失败: HTTP " + responseCode;
                    log.error(errorMessage);
                    emitter.completeWithError(new RuntimeException(errorMessage));
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);

                        // 检查是否是结束标记
                        if ("[DONE]".equals(data.trim())) {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            break;
                        }

                        // 发送数据
                        try {
                            // 验证 JSON 格式
                            objectMapper.readTree(data);
                            emitter.send(SseEmitter.event().data(data));
                        } catch (Exception e) {
                            log.warn("跳过无效的 SSE 数据: {}", data);
                        }
                    }
                }

                emitter.complete();
                log.debug("OpenRouter 流式响应完成");

            } catch (Exception e) {
                log.error("OpenRouter 流式请求异常", e);
                emitter.completeWithError(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    log.warn("关闭连接时出错", e);
                }
            }
        }).start();

        return emitter;
    }

    /**
     * 创建请求头
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
     * 获取模型列表
     */
    public Map<String, Object> getModels(BackendAccount account) {
        String decryptedKey = backendAccountService.getDecryptedToken(account.getId());
        if (decryptedKey == null) {
            throw new RuntimeException("无法解密 OpenRouter API Key");
        }

        try {
            HttpHeaders headers = createHeaders(decryptedKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = baseUrl + "/models";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("获取模型列表失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取 OpenRouter 模型列表失败", e);
            throw new RuntimeException("获取模型列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取实时定价信息
     */
    public Map<String, Object> getModelPricing(String modelName) {
        try {
            // OpenRouter 的定价信息通常包含在模型详情中
            // 这里简化实现，实际可以从 OpenRouter API 获取
            String url = baseUrl + "/models/" + modelName;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("获取 OpenRouter 模型定价失败: model={}", modelName, e);
        }

        return Map.of(
                "model", modelName,
                "pricing", Map.of(
                        "prompt", 0.0,
                        "completion", 0.0
                ),
                "note", "定价信息未获取，使用默认值"
        );
    }

    /**
     * 健康检查
     */
    public boolean healthCheck(BackendAccount account) {
        String decryptedKey = backendAccountService.getDecryptedToken(account.getId());
        if (decryptedKey == null) {
            return false;
        }

        try {
            // 发送一个简单的模型列表请求来验证 API Key 是否有效
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
            log.warn("OpenRouter 健康检查失败: accountId={}", account.getId(), e);
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
            log.debug("忽略无法解析的 OpenRouter 流式片段: {}", data);
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
                log.warn("OpenRouter 流式响应缺少 usage，跳过计费: account={}, model={}",
                        account.getAccountName(), request.getModel());
            } else {
                CostResult costResult = costCalculatorService.calculate(
                        finalUsage, request.getModel(), account.getId(), apiKey.getUserId());
                quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());
                log.info("OpenRouter 流式响应完成: account={}, tokens(in/out)={}/{}, cost=${}",
                        account.getAccountName(),
                        finalUsage.getInputTokens(),
                        finalUsage.getOutputTokens(),
                        costResult.getTotalCost());
            }

            backendAccountService.recordSuccess(account.getId());
            sessionStickinessService.saveMapping(sessionHash, account.getId());
        } catch (Exception e) {
            log.error("OpenRouter 流式计费失败: account={}", account.getAccountName(), e);
            throw new RuntimeException("OpenRouter 计费失败: " + e.getMessage(), e);
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
            log.warn("关闭连接时出错", e);
        }

        if (resources.connection != null) {
            resources.connection.disconnect();
        }
    }

    private static class StreamingResources {
        private volatile HttpURLConnection connection;
        private volatile BufferedReader reader;
    }
}
