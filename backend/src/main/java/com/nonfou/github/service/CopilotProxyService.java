package com.nonfou.github.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.entity.Model;
import com.nonfou.github.mapper.ModelMapper;
import com.nonfou.github.service.CostCalculatorService.CostResult;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
import com.nonfou.github.util.SessionUtil;
import com.nonfou.github.util.TokenEstimator;
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
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Copilot 代理服务 - 多账户增强版
 */
@Slf4j
@Service
public class CopilotProxyService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AccountSchedulerService accountScheduler;

    @Autowired
    private SessionStickinessService sessionStickinessService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private CostCalculatorService costCalculatorService;

    @Autowired
    private BackendAccountService backendAccountService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenEstimator tokenEstimator;

    @Autowired
    @Qualifier("streamTaskExecutor")
    private TaskExecutor streamTaskExecutor;

    @Value("${backend.copilot.base-url:https://api.githubcopilot.com}")
    private String copilotBaseUrl;

    @Value("${backend.streaming.timeout-ms:300000}")
    private long streamTimeoutMs;

    /**
     * 非流式聊天
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
            throw new RuntimeException("No available Copilot account");
        }

        // 3. 检查配额
        quotaService.checkAndEnforceQuota(apiKey.getUserId());

        // 4. 构建请求
        String endpoint = copilotBaseUrl + "/v1/chat/completions";
        HttpHeaders headers = buildHeaders(account);

        // 确保非流式
        ChatRequest modifiedRequest = new ChatRequest();
        modifiedRequest.setModel(request.getModel());
        modifiedRequest.setMessages(request.getMessages());
        modifiedRequest.setMaxTokens(request.getMaxTokens());
        modifiedRequest.setTemperature(request.getTemperature());
        modifiedRequest.setStream(false);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(modifiedRequest, headers);

        try {
            // 5. 发送请求
            log.info("调用 Copilot API: account={}, model={}, messages={}",
                    account.getAccountName(), request.getModel(), request.getMessages().size());

            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(endpoint, entity, ChatResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ChatResponse chatResponse = response.getBody();

                // 6. 计算成本
                if (chatResponse.getUsage() != null) {
                    TokenUsage usage = TokenUsage.builder()
                            .inputTokens(chatResponse.getUsage().getPromptTokens())
                            .outputTokens(chatResponse.getUsage().getCompletionTokens())
                            .cacheReadTokens(0)
                            .cacheWriteTokens(0)
                            .build();

                    CostResult costResult = costCalculatorService.calculate(
                            usage, request.getModel(), account.getId(), apiKey.getUserId());

                    // 7. 扣除配额
                    quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());

                    log.info("Copilot API 响应成功: account={}, tokens={}, cost=${}",
                            account.getAccountName(),
                            chatResponse.getUsage().getTotalTokens(),
                            costResult.getTotalCost());
                }

                // 8. 更新账户使用统计
                backendAccountService.recordSuccess(account.getId());

                // 9. 保存会话粘性
                sessionStickinessService.saveMapping(sessionHash, account.getId());

                return chatResponse;
            } else {
                // 10. 记录失败
                backendAccountService.recordError(account.getId(), "HTTP " + response.getStatusCode());
                throw new RuntimeException("Copilot API 响应异常: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // 11. 记录失败
            backendAccountService.recordError(account.getId(), e.getMessage());
            log.error("调用 Copilot API 失败: account={}", account.getAccountName(), e);
            throw new RuntimeException("AI 服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 流式聊天
     */
    public SseEmitter chatStream(ChatRequest request, ApiKey apiKey) {
        SseEmitter emitter = new SseEmitter(streamTimeoutMs);

        String sessionHash = SessionUtil.generateSessionHash(
                apiKey.getApiKey(),
                request.getModel(),
                request.getMessages()
        );

        BackendAccount account = accountScheduler.selectAccount(apiKey, request.getModel(), sessionHash);
        if (account == null) {
            emitter.completeWithError(new RuntimeException("No available Copilot account"));
            return emitter;
        }

        try {
            quotaService.checkAndEnforceQuota(apiKey.getUserId());
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        StreamingResources resources = new StreamingResources();
        registerEmitterCallbacks(emitter, resources);

        streamTaskExecutor.execute(() -> {
            AtomicReference<TokenUsage> usageRef = new AtomicReference<>();
            StringBuilder fullContent = new StringBuilder();

            try {
                HttpURLConnection connection = buildStreamingConnection(account);
                resources.connection = connection;

                ChatRequest modifiedRequest = new ChatRequest();
                modifiedRequest.setModel(request.getModel());
                modifiedRequest.setMessages(request.getMessages());
                modifiedRequest.setMaxTokens(request.getMaxTokens());
                modifiedRequest.setTemperature(request.getTemperature());
                modifiedRequest.setStream(true);

                String requestBody = objectMapper.writeValueAsString(modifiedRequest);
                connection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

                log.info("流式调用 Copilot API: account={}, model={}",
                        account.getAccountName(), request.getModel());

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
                    handleStreamChunk(data, fullContent, usageRef);
                }

                finalizeStreamingCost(request, apiKey, account, sessionHash, fullContent.toString(), usageRef.get());
                emitter.complete();
            } catch (Exception e) {
                log.error("流式调用 Copilot API 失败: account={}", account.getAccountName(), e);
                backendAccountService.recordError(account.getId(), e.getMessage());
                emitter.completeWithError(e);
            } finally {
                closeResources(resources);
            }
        });

        return emitter;
    }

    /**
     * 测试连接（使用调度器选择的账户）
     */
    public boolean testConnection(ApiKey apiKey) {
        try {
            BackendAccount account = accountScheduler.selectAccount(apiKey, "gpt-4", null);
            if (account == null) {
                return false;
            }

            String endpoint = copilotBaseUrl + "/v1/models";
            HttpHeaders headers = buildHeaders(account);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            if (success) {
                backendAccountService.recordSuccess(account.getId());
            } else {
                backendAccountService.recordError(account.getId(), "Connection test failed");
            }

            return success;
        } catch (Exception e) {
            log.error("Copilot API 连接测试失败", e);
            return false;
        }
    }

    /**
     * 获取可用模型列表（使用第一个可用账户）
     */
    public String getModels(ApiKey apiKey) {
        try {
            BackendAccount account = accountScheduler.selectAccount(apiKey, "gpt-4", null);
            if (account == null) {
                throw new RuntimeException("No available Copilot account");
            }

            String endpoint = copilotBaseUrl + "/v1/models";
            HttpHeaders headers = buildHeaders(account);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("获取 Copilot 模型列表失败", e);
            throw new RuntimeException("Failed to get models: " + e.getMessage());
        }
    }

    /**
     * 健康检查指定账户
     */
    public boolean healthCheck(Long accountId) {
        try {
            BackendAccount account = backendAccountService.getById(accountId);
            if (account == null || !"copilot".equals(account.getProvider())) {
                return false;
            }

            String endpoint = copilotBaseUrl + "/v1/models";
            HttpHeaders headers = buildHeaders(account);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, String.class);

            boolean healthy = response.getStatusCode() == HttpStatus.OK;

            if (healthy) {
                backendAccountService.updateHealth(accountId, "active", "健康检查通过");
            } else {
                backendAccountService.updateHealth(accountId, "error", "健康检查失败: HTTP " + response.getStatusCode());
            }

            return healthy;
        } catch (Exception e) {
            log.error("Copilot 账户健康检查失败: accountId={}", accountId, e);
            backendAccountService.updateHealth(accountId, "error", "健康检查异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(BackendAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String decryptedToken = backendAccountService.decryptToken(account.getAccessToken());
        headers.set("Authorization", "Bearer " + decryptedToken);

        return headers;
    }

    private HttpURLConnection buildStreamingConnection(BackendAccount account) throws Exception {
        String endpoint = copilotBaseUrl + "/v1/chat/completions";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + backendAccountService.decryptToken(account.getAccessToken()));
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout((int) streamTimeoutMs);
        return connection;
    }

    private void handleStreamChunk(String data,
                                   StringBuilder fullContent,
                                   AtomicReference<TokenUsage> usageRef) {
        try {
            JsonNode node = objectMapper.readTree(data);

            if (node.has("usage")) {
                Map<String, Object> usageMap = objectMapper.convertValue(node.get("usage"), MAP_TYPE);
                usageRef.set(TokenUsage.from(usageMap));
            }

            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray()) {
                for (JsonNode choice : choices) {
                    JsonNode delta = choice.get("delta");
                    if (delta != null) {
                        JsonNode contentNode = delta.get("content");
                        if (contentNode != null && !contentNode.isNull()) {
                            fullContent.append(contentNode.asText(""));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("忽略无法解析的 Copilot 流式片段: {}", data);
        }
    }

    private void finalizeStreamingCost(ChatRequest request,
                                       ApiKey apiKey,
                                       BackendAccount account,
                                       String sessionHash,
                                       String fullContent,
                                       TokenUsage usage) {
        try {
            TokenUsage finalUsage = usage != null ? usage : tokenEstimator.estimateUsage(request, fullContent);
            if (finalUsage == null) {
                log.warn("Copilot 流式响应缺少 usage 信息，跳过计费: account={}, model={}",
                        account.getAccountName(), request.getModel());
            } else {
                CostResult costResult = costCalculatorService.calculate(
                        finalUsage, request.getModel(), account.getId(), apiKey.getUserId());
                quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());
                log.info("Copilot 流式响应完成: account={}, tokens(in/out)={}/{}, cost=${}",
                        account.getAccountName(),
                        finalUsage.getInputTokens(),
                        finalUsage.getOutputTokens(),
                        costResult.getTotalCost());
            }

            backendAccountService.recordSuccess(account.getId());
            sessionStickinessService.saveMapping(sessionHash, account.getId());
        } catch (Exception e) {
            log.error("Copilot 流式计费失败: account={}", account.getAccountName(), e);
            throw new RuntimeException("Copilot 计费失败: " + e.getMessage(), e);
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
        } catch (Exception ignored) {
        }

        if (resources.connection != null) {
            resources.connection.disconnect();
        }
    }

    private static class StreamingResources {
        private volatile BufferedReader reader;
        private volatile HttpURLConnection connection;
    }
}
