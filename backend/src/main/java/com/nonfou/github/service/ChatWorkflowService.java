package com.nonfou.github.service;

import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.request.ClaudeRequest;
import com.nonfou.github.dto.request.EmbeddingsRequest;
import com.nonfou.github.dto.request.ResponsesRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.ClaudeResponse;
import com.nonfou.github.dto.response.EmbeddingsResponse;
import com.nonfou.github.dto.response.ModelsResponse;
import com.nonfou.github.dto.response.ResponsesResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nonfou.github.enums.ApiEndpoint;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
import com.nonfou.github.util.TokenEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 请求编排器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWorkflowService {

    private final CopilotProxyService copilotProxyService;
    private final ApiKeyService apiKeyService;
    private final TokenUsageStatisticsService tokenUsageStatisticsService;
    private final TokenEstimator tokenEstimator;

    public ChatResponse handleChat(String authorization, ChatRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(false);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            ChatResponse response = copilotProxyService.chat(request, apiKey);
            TokenUsage tokenUsage = extractChatUsage(request, response);
            recordNonStream(apiKey, ApiEndpoint.CHAT_COMPLETIONS, response != null ? response.getModel() : request.getModel(),
                    tokenUsage, true, null, userAgent, startedAt);
            return response;
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.CHAT_COMPLETIONS, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public SseEmitter handleStream(String authorization, ChatRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(true);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            return copilotProxyService.chatStream(request, apiKey, result -> tokenUsageStatisticsService.record(
                    TokenUsageStatisticsService.TokenUsageRecordCommand.builder()
                            .apiKey(apiKey)
                            .endpoint(ApiEndpoint.CHAT_COMPLETIONS.getPath())
                            .model(request.getModel())
                            .requestType("stream")
                            .success(result.isSuccess())
                            .tokenUsage(result.getTokenUsage())
                            .firstTokenLatencyMs(result.getFirstTokenLatencyMs())
                            .durationMs(result.getDurationMs())
                            .errorMessage(result.getErrorMessage())
                            .userAgent(userAgent)
                            .build()
            ));
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.CHAT_COMPLETIONS, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public SseEmitter handleClaudeStream(String authorization, ChatRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(true);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            return copilotProxyService.claudeStream(request, apiKey, result -> tokenUsageStatisticsService.record(
                    TokenUsageStatisticsService.TokenUsageRecordCommand.builder()
                            .apiKey(apiKey)
                            .endpoint(ApiEndpoint.CHAT_COMPLETIONS.getPath())
                            .model(request.getModel())
                            .requestType("stream")
                            .success(result.isSuccess())
                            .tokenUsage(result.getTokenUsage())
                            .firstTokenLatencyMs(result.getFirstTokenLatencyMs())
                            .durationMs(result.getDurationMs())
                            .errorMessage(result.getErrorMessage())
                            .userAgent(userAgent)
                            .build()
            ));
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.CHAT_COMPLETIONS, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public ClaudeResponse handleClaudeChat(String authorization, ClaudeRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(false);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            ClaudeResponse response = copilotProxyService.claudeMessages(request, apiKey);
            TokenUsage tokenUsage = extractClaudeUsage(request, response);
            recordNonStream(apiKey, ApiEndpoint.MESSAGES, response != null ? response.getModel() : request.getModel(),
                    tokenUsage, true, null, userAgent, startedAt);
            return response;
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.MESSAGES, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public SseEmitter handleClaudeStreamRequest(String authorization, ClaudeRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(true);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            return copilotProxyService.claudeMessagesStream(request, apiKey, result -> tokenUsageStatisticsService.record(
                    TokenUsageStatisticsService.TokenUsageRecordCommand.builder()
                            .apiKey(apiKey)
                            .endpoint(ApiEndpoint.MESSAGES.getPath())
                            .model(request.getModel())
                            .requestType("stream")
                            .success(result.isSuccess())
                            .tokenUsage(result.getTokenUsage())
                            .firstTokenLatencyMs(result.getFirstTokenLatencyMs())
                            .durationMs(result.getDurationMs())
                            .errorMessage(result.getErrorMessage())
                            .userAgent(userAgent)
                            .build()
            ));
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.MESSAGES, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public ResponsesResponse handleResponses(String authorization, ResponsesRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(false);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            ResponsesResponse response = copilotProxyService.responses(request, apiKey);
            TokenUsage tokenUsage = extractResponsesUsage(request, response);
            recordNonStream(apiKey, ApiEndpoint.RESPONSES, response != null ? response.getModel() : request.getModel(),
                    tokenUsage, true, null, userAgent, startedAt);
            return response;
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.RESPONSES, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public SseEmitter handleResponsesStream(String authorization, ResponsesRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        request.setStream(true);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            return copilotProxyService.responsesStream(request, apiKey, result -> tokenUsageStatisticsService.record(
                    TokenUsageStatisticsService.TokenUsageRecordCommand.builder()
                            .apiKey(apiKey)
                            .endpoint(ApiEndpoint.RESPONSES.getPath())
                            .model(request.getModel())
                            .requestType("stream")
                            .success(result.isSuccess())
                            .tokenUsage(result.getTokenUsage())
                            .firstTokenLatencyMs(result.getFirstTokenLatencyMs())
                            .durationMs(result.getDurationMs())
                            .errorMessage(result.getErrorMessage())
                            .userAgent(userAgent)
                            .build()
            ));
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.RESPONSES, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public EmbeddingsResponse handleEmbeddings(String authorization, EmbeddingsRequest request) {
        ApiKey apiKey = requireApiKey(authorization);
        String userAgent = resolveCurrentUserAgent();
        long startedAt = System.currentTimeMillis();
        try {
            EmbeddingsResponse response = copilotProxyService.embeddings(request, apiKey);
            TokenUsage tokenUsage = extractEmbeddingsUsage(request, response);
            recordNonStream(apiKey, ApiEndpoint.EMBEDDINGS, response != null ? response.getModel() : request.getModel(),
                    tokenUsage, true, null, userAgent, startedAt);
            return response;
        } catch (RuntimeException ex) {
            recordNonStream(apiKey, ApiEndpoint.EMBEDDINGS, request.getModel(),
                    TokenUsage.builder().build(), false, ex.getMessage(), userAgent, startedAt);
            throw ex;
        }
    }

    public ModelsResponse handleModels(String authorization) {
        return copilotProxyService.getModels(requireApiKey(authorization));
    }

    public JsonNode handleUsage(String authorization) {
        return copilotProxyService.getUsage(requireApiKey(authorization));
    }

    /**
     * 校验并返回路由用 API Key
     */
    public void validateApiKey(String authorization) {
        requireApiKey(authorization);
    }

    private ApiKey requireApiKey(String authorization) {
        String rawApiKey = extractApiKey(authorization);
        if (!StringUtils.hasText(rawApiKey)) {
            throw new ChatAuthorizationException(HttpStatus.UNAUTHORIZED.value(), "缺少 API Key");
        }

        ApiKey apiKey = apiKeyService.getRoutingApiKey(rawApiKey);
        if (apiKey == null) {
            throw new ChatAuthorizationException(HttpStatus.UNAUTHORIZED.value(), "API Key 无效或已禁用");
        }

        apiKeyService.updateLastUsedTime(rawApiKey);
        return apiKey;
    }

    private String extractApiKey(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        return StringUtils.hasText(value) ? value : null;
    }

    private void recordNonStream(ApiKey apiKey, ApiEndpoint endpoint, String model, TokenUsage tokenUsage,
                                 boolean success, String errorMessage, String userAgent, long startedAt) {
        tokenUsageStatisticsService.record(TokenUsageStatisticsService.TokenUsageRecordCommand.builder()
                .apiKey(apiKey)
                .endpoint(endpoint.getPath())
                .model(model)
                .requestType("non_stream")
                .success(success)
                .tokenUsage(tokenUsage)
                .durationMs(Math.max(System.currentTimeMillis() - startedAt, 0))
                .errorMessage(errorMessage)
                .userAgent(userAgent)
                .build());
    }

    private TokenUsage extractChatUsage(ChatRequest request, ChatResponse response) {
        if (response != null && response.getUsage() != null) {
            return TokenUsage.builder()
                    .inputTokens(nullableInt(response.getUsage().getPromptTokens()))
                    .outputTokens(nullableInt(response.getUsage().getCompletionTokens()))
                    .cacheReadTokens(response.getUsage().getPromptTokensDetails() != null
                            ? nullableInt(response.getUsage().getPromptTokensDetails().getCachedTokens())
                            : nullableInt(response.getUsage().getCacheReadInputTokens()))
                    .cacheWriteTokens(nullableInt(response.getUsage().getCacheCreationInputTokens()))
                    .build();
        }
        return tokenEstimator.estimateUsage(request, extractChatResponseText(response));
    }

    private TokenUsage extractClaudeUsage(ClaudeRequest request, ClaudeResponse response) {
        if (response != null && response.getUsage() != null) {
            return TokenUsage.builder()
                    .inputTokens(nullableInt(response.getUsage().getInputTokens()))
                    .outputTokens(nullableInt(response.getUsage().getOutputTokens()))
                    .cacheReadTokens(nullableInt(response.getUsage().getCacheReadInputTokens()))
                    .cacheWriteTokens(nullableInt(response.getUsage().getCacheCreationInputTokens()))
                    .build();
        }
        return tokenEstimator.estimateClaudeUsage(request, extractClaudeResponseText(response));
    }

    private TokenUsage extractResponsesUsage(ResponsesRequest request, ResponsesResponse response) {
        if (response != null && response.getUsage() != null) {
            return TokenUsage.builder()
                    .inputTokens(nullableInt(response.getUsage().getInputTokens()))
                    .outputTokens(nullableInt(response.getUsage().getOutputTokens()))
                    .cacheReadTokens(response.getUsage().getInputTokensDetails() != null
                            ? nullableInt(response.getUsage().getInputTokensDetails().getCachedTokens())
                            : 0)
                    .cacheWriteTokens(0)
                    .build();
        }
        return tokenEstimator.estimateResponsesUsage(request, extractResponsesText(response));
    }

    private TokenUsage extractEmbeddingsUsage(EmbeddingsRequest request, EmbeddingsResponse response) {
        if (response != null && response.getUsage() != null) {
            return TokenUsage.builder()
                    .inputTokens(nullableInt(response.getUsage().getPromptTokens()))
                    .outputTokens(0)
                    .cacheReadTokens(0)
                    .cacheWriteTokens(0)
                    .build();
        }
        return tokenEstimator.estimateEmbeddingsUsage(request);
    }

    private String extractChatResponseText(ChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        ChatResponse.Choice choice = response.getChoices().get(0);
        return choice.getMessage() != null && choice.getMessage().getContent() != null
                ? choice.getMessage().getContent()
                : "";
    }

    private String extractClaudeResponseText(ClaudeResponse response) {
        if (response == null || response.getContent() == null) {
            return "";
        }
        return response.getContent().stream()
                .map(ClaudeResponse.ContentBlock::getText)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String extractResponsesText(ResponsesResponse response) {
        if (response == null || response.getOutput() == null) {
            return "";
        }
        return response.getOutput().stream()
                .filter(Objects::nonNull)
                .flatMap(item -> item.getContent() == null ? java.util.stream.Stream.empty() : item.getContent().stream())
                .map(ResponsesResponse.Content::getText)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private int nullableInt(Integer value) {
        return value != null ? value : 0;
    }

    private String resolveCurrentUserAgent() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

}
