package com.nonfou.github.service;

import com.nonfou.github.config.CopilotProxyProperties;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 请求编排器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWorkflowService {

    private final CopilotProxyService copilotProxyService;
    private final CopilotProxyProperties copilotProxyProperties;
    private final ApiKeyService apiKeyService;

    public ChatResponse handleChat(String authorization, ChatRequest request) {
        request.setStream(false);
        return copilotProxyService.chat(request, requireApiKey(authorization));
    }

    public SseEmitter handleStream(String authorization, ChatRequest request) {
        request.setStream(true);
        return copilotProxyService.chatStream(request, requireApiKey(authorization));
    }

    public SseEmitter handleClaudeStream(String authorization, ChatRequest request) {
        request.setStream(true);
        return copilotProxyService.claudeStream(request, requireApiKey(authorization));
    }

    public ClaudeResponse handleClaudeChat(String authorization, ClaudeRequest request) {
        request.setStream(false);
        applyWarmupModelOptimization(request);
        return copilotProxyService.claudeMessages(request, requireApiKey(authorization));
    }

    public SseEmitter handleClaudeStreamRequest(String authorization, ClaudeRequest request) {
        request.setStream(true);
        applyWarmupModelOptimization(request);
        return copilotProxyService.claudeMessagesStream(request, requireApiKey(authorization), null);
    }

    public ResponsesResponse handleResponses(String authorization, ResponsesRequest request) {
        request.setStream(false);
        return copilotProxyService.responses(request, requireApiKey(authorization));
    }

    public SseEmitter handleResponsesStream(String authorization, ResponsesRequest request) {
        request.setStream(true);
        return copilotProxyService.responsesStream(request, requireApiKey(authorization));
    }

    public EmbeddingsResponse handleEmbeddings(String authorization, EmbeddingsRequest request) {
        return copilotProxyService.embeddings(request, requireApiKey(authorization));
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

    /**
     * Claude Code 热身请求使用更便宜的小模型，避免无意义消耗上游配额。
     */
    private void applyWarmupModelOptimization(ClaudeRequest request) {
        boolean hasAnthropicBeta = request.getAnthropicBeta() != null && !request.getAnthropicBeta().isEmpty();
        boolean noTools = request.getTools() == null || request.getTools().isEmpty();

        if (!hasAnthropicBeta || !noTools) {
            return;
        }

        String warmupModel = copilotProxyProperties.getWarmupModel();
        if (warmupModel == null || warmupModel.isEmpty()) {
            return;
        }

        String originalModel = request.getModel();
        request.setModel(warmupModel);
        log.debug("Claude Code 热身请求优化: {} -> {}", originalModel, warmupModel);
    }
}
