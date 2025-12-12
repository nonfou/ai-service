package com.nonfou.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.nonfou.github.config.CopilotProxyProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.request.EmbeddingsRequest;
import com.nonfou.github.dto.request.ResponsesRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.EmbeddingsResponse;
import com.nonfou.github.dto.response.ModelsResponse;
import com.nonfou.github.dto.response.ResponsesResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.exception.ChatUpstreamException;
import com.nonfou.github.service.proxy.ModelProxy;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
import com.nonfou.github.util.TokenEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Locale;

/**
 * Copilot 代理服务：负责将请求转发到外部 Copilot Relay（Node 服务），并兼容 OpenAI/Claude 协议。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotProxyService implements ModelProxy {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String RESPONSES_PATH = "/v1/responses";
    private static final String MODELS_PATH = "/v1/models";
    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    private final RestTemplate restTemplate;
    private final CopilotProxyProperties proxyProperties;
    private final ObjectMapper objectMapper;
    private final TokenEstimator tokenEstimator;
    @Qualifier("streamTaskExecutor")
    private final TaskExecutor streamTaskExecutor;

    @Override
    public String getProvider() {
        // 与 ChatWorkflowService.determineProvider 保持一致，继续使用 copilot 作为 provider 标识
        return "copilot";
    }

    /**
     * 非流式聊天请求，转发给 Copilot Relay 并返回统一的 ChatResponse。
     */
    @Override
    public ChatResponse chat(ChatRequest request, ApiKey apiKey) {
        validateApiKey(apiKey);
        Map<String, Object> payload = buildPayload(request, false);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders());

        try {
            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    baseUrl() + CHAT_COMPLETIONS_PATH,
                    HttpMethod.POST,
                    entity,
                    ChatResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (log.isDebugEnabled()) {
                    try {
                        log.debug("Copilot Proxy 响应: {}", objectMapper.writeValueAsString(response.getBody()));
                    } catch (Exception ignored) {
                    }
                }
                return response.getBody();
            }

            throw new ChatProcessingException("上游模型服务调用失败，HTTP " + response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            throw translateException(ex);
        } catch (ChatAuthorizationException | ChatProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Copilot Relay 调用异常", ex);
            throw new ChatProcessingException("聊天服务暂时不可用，请稍后重试", ex);
        }
    }

    /**
     * 流式聊天请求，透传 Copilot Relay 的 SSE。
     */
    @Override
    public SseEmitter chatStream(ChatRequest request, ApiKey apiKey) {
        return chatStream(request, apiKey, null);
    }

    /**
     * 流式聊天请求，带完成回调。
     */
    @Override
    public SseEmitter chatStream(ChatRequest request, ApiKey apiKey, StreamCompletionCallback callback) {
        validateApiKey(apiKey);
        SseEmitter emitter = new SseEmitter(proxyProperties.getStreamTimeoutMs());
        streamTaskExecutor.execute(() -> executeStream(request, emitter, callback));
        return emitter;
    }

    /**
     * Claude 格式流式聊天请求，将 OpenAI SSE 转换为 Anthropic SSE 格式。
     */
    @Override
    public SseEmitter claudeStream(ChatRequest request, ApiKey apiKey) {
        return claudeStream(request, apiKey, null);
    }

    /**
     * Claude 格式流式聊天请求，带完成回调。
     */
    @Override
    public SseEmitter claudeStream(ChatRequest request, ApiKey apiKey, StreamCompletionCallback callback) {
        validateApiKey(apiKey);
        SseEmitter emitter = new SseEmitter(proxyProperties.getStreamTimeoutMs());
        streamTaskExecutor.execute(() -> executeClaudeStream(request, emitter, callback));
        return emitter;
    }

    private void executeStream(ChatRequest request, SseEmitter emitter, StreamCompletionCallback callback) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        AtomicBoolean completed = new AtomicBoolean(false);
        int inputTokens = 0;
        int outputTokens = 0;
        int cacheReadTokens = 0;
        int cacheWriteTokens = 0;
        StringBuilder contentBuilder = new StringBuilder();

        // 预先估算输入 token（基于请求消息）
        int estimatedInputTokens = estimateInputTokens(request);

        try {
            connection = openStreamingConnection();
            Map<String, Object> payload = buildPayload(request, true);
            writeRequestBody(connection, payload);

            int status = connection.getResponseCode();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                log.warn("Copilot Proxy 流式鉴权失败: HTTP {}", status);
                sendStreamError(emitter, status, "上游模型服务鉴权失败", "authorization_error");
                invokeCallback(callback, TokenUsage.builder().build(), false, "上游模型服务鉴权失败");
                return;
            }

            if (status < 200 || status >= 300) {
                String errorBody = readErrorBody(connection);
                log.error("Copilot Proxy 流式调用失败: HTTP {}, body={}", status, errorBody);
                String friendly = extractFriendlyMessage(errorBody);
                sendStreamError(emitter, status,
                        friendly != null ? friendly : buildFriendlyMessage(status),
                        "processing_error");
                invokeCallback(callback, TokenUsage.builder().build(), false, friendly != null ? friendly : buildFriendlyMessage(status));
                return;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();
                emitter.send(SseEmitter.event().data(data));

                if ("[DONE]".equals(data)) {
                    completed.set(true);
                    break;
                }

                // 解析 token 使用信息和内容
                try {
                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode delta = choices.get(0).path("delta");
                        String content = safeText(delta.get("content"));
                        if (StringUtils.hasText(content)) {
                            contentBuilder.append(content);
                        }
                    }
                    JsonNode usage = chunk.path("usage");
                    if (!usage.isMissingNode()) {
                        if (usage.has("prompt_tokens")) {
                            inputTokens = usage.path("prompt_tokens").asInt(0);
                        }
                        if (usage.has("completion_tokens")) {
                            outputTokens = usage.path("completion_tokens").asInt(0);
                        }
                        // 解析缓存 token（Anthropic 格式）
                        if (usage.has("cache_read_input_tokens")) {
                            cacheReadTokens = usage.path("cache_read_input_tokens").asInt(0);
                        }
                        if (usage.has("cache_creation_input_tokens")) {
                            cacheWriteTokens = usage.path("cache_creation_input_tokens").asInt(0);
                        }
                        // 解析缓存 token（OpenAI 格式，prompt_tokens_details）
                        JsonNode promptDetails = usage.path("prompt_tokens_details");
                        if (!promptDetails.isMissingNode() && promptDetails.has("cached_tokens")) {
                            cacheReadTokens = promptDetails.path("cached_tokens").asInt(0);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (!completed.get()) {
                emitter.send(SseEmitter.event().data("[DONE]"));
            }
            emitter.complete();

            // 使用上游返回的 token 数，如果没有则使用估算值
            int finalInputTokens = inputTokens > 0 ? inputTokens : estimatedInputTokens;
            int finalOutputTokens = outputTokens > 0 ? outputTokens : tokenEstimator.estimateTextTokens(contentBuilder.toString());

            TokenUsage tokenUsage = TokenUsage.builder()
                    .inputTokens(finalInputTokens)
                    .outputTokens(finalOutputTokens)
                    .cacheReadTokens(cacheReadTokens)
                    .cacheWriteTokens(cacheWriteTokens)
                    .build();
            invokeCallback(callback, tokenUsage, true, null);
        } catch (Exception ex) {
            log.error("Copilot Proxy 流式调用异常", ex);
            sendStreamError(emitter, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "上游模型服务流式接口异常", "processing_error");
            invokeCallback(callback, TokenUsage.builder().build(), false, ex.getMessage());
        } finally {
            closeResources(reader, connection);
        }
    }

    /**
     * 执行 Claude 格式的流式请求
     * 将 OpenAI SSE 格式转换为 Anthropic SSE 格式
     */
    private void executeClaudeStream(ChatRequest request, SseEmitter emitter, StreamCompletionCallback callback) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String messageId = "msg_" + System.currentTimeMillis();
        String model = request.getModel();
        StringBuilder contentBuilder = new StringBuilder();
        int inputTokens = 0;
        int outputTokens = 0;
        int cacheReadTokens = 0;
        int cacheWriteTokens = 0;

        // 预先估算输入 token（基于请求消息）
        int estimatedInputTokens = estimateInputTokens(request);

        try {
            connection = openStreamingConnection();
            Map<String, Object> payload = buildPayload(request, true);
            writeRequestBody(connection, payload);

            int status = connection.getResponseCode();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                log.warn("Claude 流式鉴权失败: HTTP {}", status);
                sendClaudeStreamError(emitter, status, "上游模型服务鉴权失败");
                invokeCallback(callback, TokenUsage.builder().build(), false, "上游模型服务鉴权失败");
                return;
            }

            if (status < 200 || status >= 300) {
                String errorBody = readErrorBody(connection);
                log.error("Claude 流式调用失败: HTTP {}, body={}", status, errorBody);
                String friendly = extractFriendlyMessage(errorBody);
                sendClaudeStreamError(emitter, status, friendly != null ? friendly : buildFriendlyMessage(status));
                invokeCallback(callback, TokenUsage.builder().build(), false, friendly != null ? friendly : buildFriendlyMessage(status));
                return;
            }

            // 发送 message_start 事件
            sendClaudeMessageStart(emitter, messageId, model);

            // 发送 content_block_start 事件
            sendClaudeContentBlockStart(emitter);

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();

                if ("[DONE]".equals(data)) {
                    break;
                }

                try {
                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode delta = choices.get(0).path("delta");
                        String content = safeText(delta.get("content"));
                        if (StringUtils.hasText(content)) {
                            contentBuilder.append(content);
                            sendClaudeContentBlockDelta(emitter, content);
                        }
                    }

                    // 提取 usage 信息
                    JsonNode usage = chunk.path("usage");
                    if (!usage.isMissingNode()) {
                        if (usage.has("prompt_tokens")) {
                            inputTokens = usage.path("prompt_tokens").asInt(0);
                        }
                        if (usage.has("completion_tokens")) {
                            outputTokens = usage.path("completion_tokens").asInt(0);
                        }
                        // 解析缓存 token（Anthropic 格式）
                        if (usage.has("cache_read_input_tokens")) {
                            cacheReadTokens = usage.path("cache_read_input_tokens").asInt(0);
                        }
                        if (usage.has("cache_creation_input_tokens")) {
                            cacheWriteTokens = usage.path("cache_creation_input_tokens").asInt(0);
                        }
                        // 解析缓存 token（OpenAI 格式，prompt_tokens_details）
                        JsonNode promptDetails = usage.path("prompt_tokens_details");
                        if (!promptDetails.isMissingNode() && promptDetails.has("cached_tokens")) {
                            cacheReadTokens = promptDetails.path("cached_tokens").asInt(0);
                        }
                    }
                } catch (Exception parseEx) {
                    log.debug("解析流式数据失败: {}", data);
                }
            }

            // 发送 content_block_stop 事件
            sendClaudeContentBlockStop(emitter);

            // 发送 message_delta 事件（包含 stop_reason）
            sendClaudeMessageDelta(emitter);

            // 发送 message_stop 事件
            sendClaudeMessageStop(emitter);

            emitter.complete();

            // 使用上游返回的 token 数，如果没有则使用估算值
            int finalInputTokens = inputTokens > 0 ? inputTokens : estimatedInputTokens;
            int finalOutputTokens = outputTokens > 0 ? outputTokens : tokenEstimator.estimateTextTokens(contentBuilder.toString());

            TokenUsage tokenUsage = TokenUsage.builder()
                    .inputTokens(finalInputTokens)
                    .outputTokens(finalOutputTokens)
                    .cacheReadTokens(cacheReadTokens)
                    .cacheWriteTokens(cacheWriteTokens)
                    .build();
            invokeCallback(callback, tokenUsage, true, null);
        } catch (Exception ex) {
            log.error("Claude 流式调用异常", ex);
            sendClaudeStreamError(emitter, HttpStatus.INTERNAL_SERVER_ERROR.value(), "上游模型服务流式接口异常");
            invokeCallback(callback, TokenUsage.builder().build(), false, ex.getMessage());
        } finally {
            closeResources(reader, connection);
        }
    }

    private void invokeCallback(StreamCompletionCallback callback, TokenUsage tokenUsage, boolean success, String errorMessage) {
        if (callback != null) {
            try {
                callback.onComplete(tokenUsage, success, errorMessage);
            } catch (Exception e) {
                log.error("流式回调执行失败", e);
            }
        }
    }

    private void sendClaudeMessageStart(SseEmitter emitter, String messageId, String model) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "message_start");

            Map<String, Object> message = new HashMap<>();
            message.put("id", messageId);
            message.put("type", "message");
            message.put("role", "assistant");
            message.put("content", java.util.Collections.emptyList());
            message.put("model", model);
            message.put("stop_reason", null);
            message.put("stop_sequence", null);

            Map<String, Object> usage = new HashMap<>();
            usage.put("input_tokens", 0);
            usage.put("output_tokens", 0);
            message.put("usage", usage);

            event.put("message", message);

            emitter.send(SseEmitter.event()
                    .name("message_start")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("发送 message_start 事件失败", e);
        }
    }

    private void sendClaudeContentBlockStart(SseEmitter emitter) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "content_block_start");
            event.put("index", 0);

            Map<String, Object> contentBlock = new HashMap<>();
            contentBlock.put("type", "text");
            contentBlock.put("text", "");
            event.put("content_block", contentBlock);

            emitter.send(SseEmitter.event()
                    .name("content_block_start")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("发送 content_block_start 事件失败", e);
        }
    }

    private void sendClaudeContentBlockDelta(SseEmitter emitter, String text) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "content_block_delta");
            event.put("index", 0);

            Map<String, Object> delta = new HashMap<>();
            delta.put("type", "text_delta");
            delta.put("text", text);
            event.put("delta", delta);

            emitter.send(SseEmitter.event()
                    .name("content_block_delta")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("发送 content_block_delta 事件失败", e);
        }
    }

    private void sendClaudeContentBlockStop(SseEmitter emitter) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "content_block_stop");
            event.put("index", 0);

            emitter.send(SseEmitter.event()
                    .name("content_block_stop")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("发送 content_block_stop 事件失败", e);
        }
    }

    private void sendClaudeMessageDelta(SseEmitter emitter) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "message_delta");

            Map<String, Object> delta = new HashMap<>();
            delta.put("stop_reason", "end_turn");
            delta.put("stop_sequence", null);
            event.put("delta", delta);

            Map<String, Object> usage = new HashMap<>();
            usage.put("output_tokens", 0);
            event.put("usage", usage);

            emitter.send(SseEmitter.event()
                    .name("message_delta")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("发送 message_delta 事件失败", e);
        }
    }

    private void sendClaudeMessageStop(SseEmitter emitter) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "message_stop");

            emitter.send(SseEmitter.event()
                    .name("message_stop")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("发送 message_stop 事件失败", e);
        }
    }

    private void sendClaudeStreamError(SseEmitter emitter, int statusCode, String message) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "error");

            Map<String, Object> error = new HashMap<>();
            String errorType = switch (statusCode) {
                case 400 -> "invalid_request_error";
                case 401 -> "authentication_error";
                case 403 -> "permission_error";
                case 404 -> "not_found_error";
                case 429 -> "rate_limit_error";
                default -> "api_error";
            };
            error.put("type", errorType);
            error.put("message", message);
            event.put("error", error);

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(event)));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private Map<String, Object> buildPayload(ChatRequest request, boolean stream) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.getModel());
        payload.put("messages", request.getMessages());
        payload.put("stream", stream);

        if (request.getMaxTokens() != null) {
            payload.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            payload.put("temperature", request.getTemperature());
        }
        if (!CollectionUtils.isEmpty(request.getAdditionalParams())) {
            payload.putAll(request.getAdditionalParams());
        }

        return payload;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (StringUtils.hasText(proxyProperties.getApiKey())) {
            headers.set(HttpHeaders.AUTHORIZATION, formatBearer(proxyProperties.getApiKey()));
        }

        if (!CollectionUtils.isEmpty(proxyProperties.getExtraHeaders())) {
            proxyProperties.getExtraHeaders().forEach(headers::set);
        }

        return headers;
    }

    private HttpURLConnection openStreamingConnection() throws Exception {
        return openStreamingConnection(CHAT_COMPLETIONS_PATH);
    }

    private void writeRequestBody(HttpURLConnection connection, Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    private RuntimeException translateException(HttpStatusCodeException ex) {
        int statusCode = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        log.warn("Copilot Proxy 请求失败: status={}, body={}", statusCode, body);

        if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
            return new ChatAuthorizationException(statusCode, "上游模型服务鉴权失败");
        }

        String friendly = extractFriendlyMessage(body);
        if (ex.getStatusCode().is4xxClientError()) {
            return new ChatUpstreamException(statusCode,
                    friendly != null ? friendly : "请求参数不被上游接受，请检查模型或参数设置", ex);
        }

        if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return new ChatProcessingException("上游模型服务请求过于频繁，请稍后再试", ex);
        }

            return new ChatProcessingException(
                    friendly != null ? friendly : buildFriendlyMessage(statusCode), ex);
    }

    private String formatBearer(String value) {
        return value.startsWith("Bearer ") ? value : "Bearer " + value;
    }

    private void closeResources(BufferedReader reader, HttpURLConnection connection) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception ignored) {
        }
        if (connection != null) {
            connection.disconnect();
        }
    }

    private void sendStreamError(SseEmitter emitter, int statusCode, String message, String type) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("code", statusCode);
        error.put("message", message);
        error.put("type", type);
        payload.put("error", error);

        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(payload)));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    private String readErrorBody(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.length() > 0 ? builder.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractFriendlyMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            String code = safeText(errorNode.get("code"));
            String message = safeText(errorNode.get("message"));

            if (StringUtils.hasText(message) && message.trim().startsWith("{")) {
                try {
                    JsonNode nested = objectMapper.readTree(message);
                    JsonNode nestedError = nested.path("error");
                    if (!nestedError.isMissingNode()) {
                        if (!StringUtils.hasText(code)) {
                            code = safeText(nestedError.get("code"));
                        }
                        message = safeText(nestedError.get("message"));
                    }
                } catch (Exception ignored) {
                }
            }

            if (!errorNode.isMissingNode() && !StringUtils.hasText(message)) {
                message = responseBody;
            }

            if (!errorNode.isMissingNode()) {
                return translateFriendlyMessage(message, code);
            }

            if (root.has("message")) {
                return translateFriendlyMessage(safeText(root.get("message")), null);
            }

            return translateFriendlyMessage(responseBody, null);
        } catch (Exception e) {
            return translateFriendlyMessage(responseBody, null);
        }
    }

    private String translateFriendlyMessage(String rawMessage, String code) {
        String fallback = "上游模型服务暂不可用，请稍后重试";
        if (StringUtils.hasText(code) && "model_not_supported".equalsIgnoreCase(code)) {
            return "该模型暂未开放，请在控制台更换其他模型";
        }
        if (StringUtils.hasText(rawMessage)) {
            String text = rawMessage.trim();
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("model") && lower.contains("not supported")) {
                return "该模型暂未开放，请在控制台更换其他模型";
            }
            return text;
        }
        return fallback;
    }

    private String buildFriendlyMessage(int status) {
        return "上游模型服务调用失败，HTTP " + status;
    }

    private String safeText(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private void validateApiKey(ApiKey apiKey) {
        if (apiKey == null) {
            throw new ChatAuthorizationException(HttpStatus.UNAUTHORIZED.value(), "API Key 无效");
        }
        if (apiKey.getStatus() != null && apiKey.getStatus() == 0) {
            throw new ChatAuthorizationException(HttpStatus.FORBIDDEN.value(), "API Key 已被禁用");
        }
    }

    /**
     * 根据请求消息估算输入 token 数
     */
    private int estimateInputTokens(ChatRequest request) {
        if (request == null || request.getMessages() == null) {
            return 0;
        }
        return request.getMessages().stream()
                .map(ChatRequest.Message::getContent)
                .mapToInt(content -> tokenEstimator.estimateTextTokens(content != null ? content.toString() : ""))
                .sum();
    }

    private String baseUrl() {
        String base = proxyProperties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new ChatProcessingException("代理服务基础地址未配置");
        }
        String normalized = base.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    // ============================================================
    // Models API 支持
    // ============================================================

    /**
     * 获取可用模型列表
     */
    public ModelsResponse getModels() {
        HttpEntity<?> entity = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<ModelsResponse> response = restTemplate.exchange(
                    baseUrl() + MODELS_PATH,
                    HttpMethod.GET,
                    entity,
                    ModelsResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new ChatProcessingException("获取模型列表失败，HTTP " + response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            throw translateException(ex);
        } catch (ChatAuthorizationException | ChatProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("获取模型列表异常", ex);
            throw new ChatProcessingException("模型列表服务暂时不可用，请稍后重试", ex);
        }
    }

    // ============================================================
    // Embeddings API 支持
    // ============================================================

    /**
     * 创建嵌入向量
     */
    public EmbeddingsResponse embeddings(EmbeddingsRequest request, ApiKey apiKey) {
        validateApiKey(apiKey);
        Map<String, Object> payload = buildEmbeddingsPayload(request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders());

        try {
            ResponseEntity<EmbeddingsResponse> response = restTemplate.exchange(
                    baseUrl() + EMBEDDINGS_PATH,
                    HttpMethod.POST,
                    entity,
                    EmbeddingsResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (log.isDebugEnabled()) {
                    try {
                        log.debug("Copilot Proxy Embeddings 响应: {}", objectMapper.writeValueAsString(response.getBody()));
                    } catch (Exception ignored) {
                    }
                }
                return response.getBody();
            }

            throw new ChatProcessingException("创建嵌入向量失败，HTTP " + response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            throw translateException(ex);
        } catch (ChatAuthorizationException | ChatProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("创建嵌入向量异常", ex);
            throw new ChatProcessingException("嵌入向量服务暂时不可用，请稍后重试", ex);
        }
    }

    private Map<String, Object> buildEmbeddingsPayload(EmbeddingsRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.getModel());
        payload.put("input", request.getInput());

        if (request.getEncodingFormat() != null) {
            payload.put("encoding_format", request.getEncodingFormat());
        }
        if (request.getDimensions() != null) {
            payload.put("dimensions", request.getDimensions());
        }
        if (request.getUser() != null) {
            payload.put("user", request.getUser());
        }
        if (!CollectionUtils.isEmpty(request.getAdditionalParams())) {
            payload.putAll(request.getAdditionalParams());
        }

        return payload;
    }

    // ============================================================
    // Responses API 支持（用于 Codex 系列模型）
    // ============================================================

    /**
     * 非流式 Responses 请求，转发给 Copilot Relay 并返回 ResponsesResponse。
     */
    public ResponsesResponse responses(ResponsesRequest request, ApiKey apiKey) {
        validateApiKey(apiKey);
        Map<String, Object> payload = buildResponsesPayload(request, false);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders());

        try {
            ResponseEntity<ResponsesResponse> response = restTemplate.exchange(
                    baseUrl() + RESPONSES_PATH,
                    HttpMethod.POST,
                    entity,
                    ResponsesResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (log.isDebugEnabled()) {
                    try {
                        log.debug("Copilot Proxy Responses 响应: {}", objectMapper.writeValueAsString(response.getBody()));
                    } catch (Exception ignored) {
                    }
                }
                return response.getBody();
            }

            throw new ChatProcessingException("上游模型服务调用失败，HTTP " + response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            throw translateException(ex);
        } catch (ChatAuthorizationException | ChatProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Copilot Relay Responses 调用异常", ex);
            throw new ChatProcessingException("Responses 服务暂时不可用，请稍后重试", ex);
        }
    }

    /**
     * 流式 Responses 请求，透传 Copilot Relay 的 SSE。
     */
    public SseEmitter responsesStream(ResponsesRequest request, ApiKey apiKey) {
        validateApiKey(apiKey);
        SseEmitter emitter = new SseEmitter(proxyProperties.getStreamTimeoutMs());
        streamTaskExecutor.execute(() -> executeResponsesStream(request, emitter));
        return emitter;
    }

    private void executeResponsesStream(ResponsesRequest request, SseEmitter emitter) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        AtomicBoolean completed = new AtomicBoolean(false);

        try {
            connection = openStreamingConnection(RESPONSES_PATH);
            Map<String, Object> payload = buildResponsesPayload(request, true);
            writeRequestBody(connection, payload);

            int status = connection.getResponseCode();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                log.warn("Copilot Proxy Responses 流式鉴权失败: HTTP {}", status);
                sendStreamError(emitter, status, "上游模型服务鉴权失败", "authorization_error");
                return;
            }

            if (status < 200 || status >= 300) {
                String errorBody = readErrorBody(connection);
                log.error("Copilot Proxy Responses 流式调用失败: HTTP {}, body={}", status, errorBody);
                String friendly = extractFriendlyMessage(errorBody);
                sendStreamError(emitter, status,
                        friendly != null ? friendly : buildFriendlyMessage(status),
                        "processing_error");
                return;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            String currentEvent = null;
            while ((line = reader.readLine()) != null) {
                // Responses API 使用 event: xxx 和 data: xxx 格式
                if (line.startsWith("event: ")) {
                    currentEvent = line.substring(7).trim();
                    continue;
                }
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    // 透传事件和数据
                    if (currentEvent != null) {
                        emitter.send(SseEmitter.event().name(currentEvent).data(data));
                    } else {
                        emitter.send(SseEmitter.event().data(data));
                    }

                    // 检查是否完成
                    if ("response.completed".equals(currentEvent) || "response.failed".equals(currentEvent)) {
                        completed.set(true);
                    }
                    currentEvent = null;
                }
            }

            if (!completed.get()) {
                // 上游未发送 response.completed，手动补发以满足 Codex CLI 要求
                log.warn("Responses 流结束但未收到 response.completed 事件，手动补发");
                emitter.send(SseEmitter.event().name("response.completed").data("{\"type\":\"response.completed\"}"));
            }
            emitter.complete();
        } catch (Exception ex) {
            log.error("Copilot Proxy Responses 流式调用异常", ex);
            sendStreamError(emitter, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "上游模型服务流式接口异常", "processing_error");
        } finally {
            closeResources(reader, connection);
        }
    }

    private Map<String, Object> buildResponsesPayload(ResponsesRequest request, boolean stream) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.getModel());
        payload.put("input", request.getInput());
        payload.put("stream", stream);

        if (request.getInstructions() != null) {
            payload.put("instructions", request.getInstructions());
        }
        if (request.getMaxOutputTokens() != null) {
            payload.put("max_output_tokens", request.getMaxOutputTokens());
        }
        if (request.getTemperature() != null) {
            payload.put("temperature", request.getTemperature());
        }
        if (!CollectionUtils.isEmpty(request.getAdditionalParams())) {
            payload.putAll(request.getAdditionalParams());
        }

        return payload;
    }

    private HttpURLConnection openStreamingConnection(String path) throws Exception {
        URL url = new URL(baseUrl() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(proxyProperties.getConnectTimeoutMs());
        connection.setReadTimeout(proxyProperties.getReadTimeoutMs());
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(proxyProperties.getApiKey())) {
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, formatBearer(proxyProperties.getApiKey()));
        }
        if (!CollectionUtils.isEmpty(proxyProperties.getExtraHeaders())) {
            proxyProperties.getExtraHeaders().forEach(connection::setRequestProperty);
        }

        return connection;
    }
}
