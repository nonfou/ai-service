package com.nonfou.github.service;

import com.nonfou.github.config.CopilotProxyProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.request.ClaudeRequest;
import com.nonfou.github.dto.request.EmbeddingsRequest;
import com.nonfou.github.dto.request.ResponsesRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.ClaudeResponse;
import com.nonfou.github.dto.response.EmbeddingsResponse;
import com.nonfou.github.dto.response.ResponsesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 最小化请求编排器。
 * 仅负责把对外兼容接口请求转发到 Copilot 上游，不再执行本地 API Key、计费或路由逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWorkflowService {

    private final CopilotProxyService copilotProxyService;
    private final CopilotProxyProperties copilotProxyProperties;

    public ChatResponse handleChat(String authorization, ChatRequest request) {
        request.setStream(false);
        return copilotProxyService.chat(request, null);
    }

    public SseEmitter handleStream(String authorization, ChatRequest request) {
        request.setStream(true);
        return copilotProxyService.chatStream(request, null);
    }

    public SseEmitter handleClaudeStream(String authorization, ChatRequest request) {
        request.setStream(true);
        return copilotProxyService.claudeStream(request, null);
    }

    public ClaudeResponse handleClaudeChat(String authorization, ClaudeRequest request) {
        request.setStream(false);
        applyWarmupModelOptimization(request);
        return copilotProxyService.claudeMessages(request, null);
    }

    public SseEmitter handleClaudeStreamRequest(String authorization, ClaudeRequest request) {
        request.setStream(true);
        applyWarmupModelOptimization(request);
        return copilotProxyService.claudeMessagesStream(request, null, null);
    }

    public ResponsesResponse handleResponses(String authorization, ResponsesRequest request) {
        request.setStream(false);
        return copilotProxyService.responses(request, null);
    }

    public SseEmitter handleResponsesStream(String authorization, ResponsesRequest request) {
        request.setStream(true);
        return copilotProxyService.responsesStream(request, null);
    }

    public EmbeddingsResponse handleEmbeddings(String authorization, EmbeddingsRequest request) {
        return copilotProxyService.embeddings(request, null);
    }

    /**
     * 最小代理模式下不再做本地 API Key 校验，外部调用是否放行由网关或网络边界控制。
     */
    public void validateApiKey(String authorization) {
        log.debug("跳过本地 API Key 校验，authorizationPresent={}", authorization != null && !authorization.isBlank());
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
