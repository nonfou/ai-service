package com.nonfou.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.nonfou.github.config.CopilotProxyProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.exception.ChatUpstreamException;
import com.nonfou.github.service.proxy.ModelProxy;
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

    private final RestTemplate restTemplate;
    private final CopilotProxyProperties proxyProperties;
    private final ObjectMapper objectMapper;
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
        validateApiKey(apiKey);
        SseEmitter emitter = new SseEmitter(proxyProperties.getStreamTimeoutMs());
        streamTaskExecutor.execute(() -> executeStream(request, emitter));
        return emitter;
    }

    private void executeStream(ChatRequest request, SseEmitter emitter) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        AtomicBoolean completed = new AtomicBoolean(false);

        try {
            connection = openStreamingConnection();
            Map<String, Object> payload = buildPayload(request, true);
            writeRequestBody(connection, payload);

            int status = connection.getResponseCode();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                log.warn("Copilot Proxy 流式鉴权失败: HTTP {}", status);
                sendStreamError(emitter, status, "上游模型服务鉴权失败", "authorization_error");
                return;
            }

            if (status < 200 || status >= 300) {
                String errorBody = readErrorBody(connection);
                log.error("Copilot Proxy 流式调用失败: HTTP {}, body={}", status, errorBody);
                String friendly = extractFriendlyMessage(errorBody);
                sendStreamError(emitter, status,
                        friendly != null ? friendly : buildFriendlyMessage(status),
                        "processing_error");
                return;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6);
                emitter.send(SseEmitter.event().data(data));

                if ("[DONE]".equals(data.trim())) {
                    completed.set(true);
                    break;
                }
            }

            if (!completed.get()) {
                emitter.send(SseEmitter.event().data("[DONE]"));
            }
            emitter.complete();
        } catch (Exception ex) {
            log.error("Copilot Proxy 流式调用异常", ex);
            sendStreamError(emitter, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "上游模型服务流式接口异常", "processing_error");
        } finally {
            closeResources(reader, connection);
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
        URL url = new URL(baseUrl() + CHAT_COMPLETIONS_PATH);
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
}
