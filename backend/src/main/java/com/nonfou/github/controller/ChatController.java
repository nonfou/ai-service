package com.nonfou.github.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.common.Result;
import com.nonfou.github.config.ModelMappingProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.request.EmbeddingsRequest;
import com.nonfou.github.dto.request.ResponsesRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.ClaudeCountTokensResponse;
import com.nonfou.github.dto.response.ClaudeErrorResponse;
import com.nonfou.github.dto.response.ClaudeResponse;
import com.nonfou.github.dto.response.EmbeddingsResponse;
import com.nonfou.github.dto.response.ModelsResponse;
import com.nonfou.github.dto.response.ResponsesResponse;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.exception.ChatUpstreamException;
import com.nonfou.github.exception.BusinessException;
import com.nonfou.github.service.ChatWorkflowService;
import com.nonfou.github.service.CopilotProxyService;
import com.nonfou.github.exception.ApiErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 聊天 API Controller - 通用 AI API 代理服务器
 * 兼容多种主流 AI API 格式: OpenAI, Claude, Azure OpenAI 等
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class ChatController {

    private final ChatWorkflowService chatWorkflowService;
    private final CopilotProxyService copilotProxyService;
    private final ObjectMapper objectMapper;
    private final ModelMappingProperties modelMappingProperties;

    /**
     * OpenAI API 兼容接口: /chat/completions
     * 标准的 OpenAI Chat Completions API 格式
     */
    @PostMapping("/chat/completions")
    public Object openaiChatCompletions(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated ChatRequest request) {
        log.debug("OpenAI API 兼容接口调用: /v1/chat/completions");
        return handleChatRequest(authorization, request);
    }

    /**
     * Claude API 兼容接口: /messages
     * Anthropic Claude Messages API 格式
     */
    @PostMapping("/messages")
    public Object claudeMessages(
            @RequestHeader(value = "x-api-key", required = false) String xApiKey,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Validated ChatRequest request) {
        log.info("Claude API 兼容接口调用: /v1/messages, model={}", request.getModel());

        // Claude API 使用 x-api-key header，需要转换为 Authorization
        String authHeader = xApiKey != null ? "Bearer " + xApiKey : authorization;
        return handleClaudeRequest(authHeader, request);
    }

    /**
     * Claude API count_tokens 接口: /messages/count_tokens
     * 计算消息的 token 数量
     */
    @PostMapping("/messages/count_tokens")
    public Object claudeCountTokens(
            @RequestHeader(value = "x-api-key", required = false) String xApiKey,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Validated ChatRequest request) {
        log.info("Claude API count_tokens 接口调用: /v1/messages/count_tokens, model={}", request.getModel());

        // 简单估算 token 数量（实际应该使用 tokenizer）
        // 这里使用粗略估算：约 4 个字符 = 1 个 token
        int estimatedTokens = 0;
        if (request.getMessages() != null) {
            for (var message : request.getMessages()) {
                if (message.getContent() != null) {
                    Object content = message.getContent();
                    if (content instanceof String text) {
                        estimatedTokens += text.length() / 4 + 1;
                    } else {
                        // 复杂内容结构，粗略估算
                        estimatedTokens += 100;
                    }
                }
            }
        }

        return ClaudeCountTokensResponse.builder()
                .inputTokens(estimatedTokens)
                .build();
    }

    /**
     * OpenAI 模型列表接口: /models
     * 返回可用的模型列表
     */
    @RequestMapping("/models")
    public Object listModels() {
        log.debug("OpenAI API 兼容接口调用: /v1/models");
        try {
            ModelsResponse response = copilotProxyService.getModels();
            return response;
        } catch (ChatAuthorizationException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()));
        } catch (ChatUpstreamException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (ChatProcessingException e) {
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("获取模型列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * OpenAI Embeddings 接口: /embeddings
     * 创建嵌入向量
     */
    @PostMapping("/embeddings")
    public Object embeddings(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated EmbeddingsRequest request) {
        log.debug("OpenAI API 兼容接口调用: /v1/embeddings, model={}", request.getModel());
        try {
            EmbeddingsResponse response = chatWorkflowService.handleEmbeddings(authorization, request);
            return response;
        } catch (ChatAuthorizationException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()));
        } catch (ChatUpstreamException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (ChatProcessingException e) {
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("创建嵌入向量失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ============================================================
    // Responses API 支持（用于 Codex 系列模型）
    // ============================================================

    /**
     * Responses API 接口: /responses
     * 用于 Codex 系列模型（gpt-5.1-codex, gpt-5.1-codex-mini, gpt-5.1-codex-max）
     */
    @PostMapping("/responses")
    public Object responses(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated ResponsesRequest request) {
        log.info("Responses API 接口调用: /v1/responses, model={}, stream={}", request.getModel(), request.getStream());
        return handleResponsesRequest(authorization, request);
    }

    /**
     * 通用 Responses 处理方法
     */
    private Object handleResponsesRequest(String authorization, ResponsesRequest request) {
        normalizeResponsesModelName(request);

        // 如果请求流式响应，返回 SSE
        if (Boolean.TRUE.equals(request.getStream())) {
            return responsesStream(authorization, request);
        }

        // 非流式响应
        try {
            ResponsesResponse response = chatWorkflowService.handleResponses(authorization, request);
            return Result.success(response);
        } catch (ChatAuthorizationException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()));
        } catch (ChatUpstreamException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (ChatProcessingException e) {
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("Responses API调用失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 流式 Responses 接口
     */
    private SseEmitter responsesStream(String authorization, ResponsesRequest request) {
        try {
            return chatWorkflowService.handleResponsesStream(authorization, request);
        } catch (ChatAuthorizationException e) {
            log.warn("Responses 流式请求鉴权失败: {}", e.getMessage());
            return buildErrorEmitter(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()), "authorization_error");
        } catch (BusinessException e) {
            log.warn("Responses 流式请求业务异常: {}", e.getMessage());
            return buildErrorEmitter(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()), "business_error");
        } catch (ChatUpstreamException e) {
            log.warn("Responses 流式请求上游异常: {}", e.getMessage());
            return buildErrorEmitter(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()), "upstream_error");
        } catch (ChatProcessingException e) {
            log.error("Responses 流式请求处理异常: {}", e.getMessage(), e);
            return buildErrorEmitter(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), "processing_error");
        } catch (Exception e) {
            log.error("Responses 流式API调用失败: {}", e.getMessage(), e);
            return buildErrorEmitter(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Responses 服务暂时不可用，请稍后重试", "processing_error");
        }
    }

    private void normalizeResponsesModelName(ResponsesRequest request) {
        if (request == null || !StringUtils.hasText(request.getModel())) {
            return;
        }

        if (!modelMappingProperties.isEnabled()) {
            return;
        }

        Map<String, String> aliases = modelMappingProperties.getAliases();
        if (aliases == null || aliases.isEmpty()) {
            return;
        }

        String raw = request.getModel().trim();
        String key = raw.toLowerCase(Locale.ROOT);

        String normalized = modelMappingProperties.getAliases()
                .getOrDefault(key, raw);

        if (!normalized.equals(raw)) {
            log.debug("Responses 模型名规范化: {} -> {}", raw, normalized);
            request.setModel(normalized);
        }
    }

    /**
     * 通用聊天处理方法（OpenAI 格式）
     */
    private Object handleChatRequest(String authorization, ChatRequest request) {
        normalizeModelName(request);

        // 如果请求流式响应，返回 SSE
        if (Boolean.TRUE.equals(request.getStream())) {
            return chatStream(authorization, request);
        }

        // 非流式响应
        try {
            ChatResponse response = chatWorkflowService.handleChat(authorization, request);
            return Result.success(response);
        } catch (ChatAuthorizationException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()));
        } catch (ChatUpstreamException e) {
            return Result.error(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (ChatProcessingException e) {
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("API调用失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * Claude API 请求处理方法（Anthropic 格式）
     */
    private Object handleClaudeRequest(String authorization, ChatRequest request) {
        normalizeModelName(request);

        // 如果请求流式响应，返回 SSE（Claude 格式）
        if (Boolean.TRUE.equals(request.getStream())) {
            return claudeStream(authorization, request);
        }

        // 非流式响应
        try {
            ChatResponse response = chatWorkflowService.handleChat(authorization, request);
            // 转换为 Claude 格式
            return ClaudeResponse.fromChatResponse(response);
        } catch (ChatAuthorizationException e) {
            return ClaudeErrorResponse.fromStatusCode(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (BusinessException e) {
            return ClaudeErrorResponse.fromStatusCode(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()));
        } catch (ChatUpstreamException e) {
            return ClaudeErrorResponse.fromStatusCode(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (ChatProcessingException e) {
            return ClaudeErrorResponse.fromStatusCode(500, e.getMessage());
        } catch (Exception e) {
            log.error("Claude API调用失败: {}", e.getMessage(), e);
            return ClaudeErrorResponse.fromStatusCode(500, "服务暂时不可用，请稍后重试");
        }
    }

    /**
     * Claude 格式流式聊天接口
     */
    private SseEmitter claudeStream(String authorization, ChatRequest request) {
        try {
            return chatWorkflowService.handleClaudeStream(authorization, request);
        } catch (ChatAuthorizationException e) {
            log.warn("Claude 流式请求鉴权失败: {}", e.getMessage());
            return buildClaudeErrorEmitter(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (BusinessException e) {
            log.warn("Claude 流式请求业务异常: {}", e.getMessage());
            return buildClaudeErrorEmitter(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()));
        } catch (ChatUpstreamException e) {
            log.warn("Claude 流式请求上游异常: {}", e.getMessage());
            return buildClaudeErrorEmitter(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()));
        } catch (ChatProcessingException e) {
            log.error("Claude 流式请求处理异常: {}", e.getMessage(), e);
            return buildClaudeErrorEmitter(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        } catch (Exception e) {
            log.error("Claude 流式API调用失败: {}", e.getMessage(), e);
            return buildClaudeErrorEmitter(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 流式聊天接口
     */
    private SseEmitter chatStream(
            String authorization,
            ChatRequest request) {

        try {
            return chatWorkflowService.handleStream(authorization, request);
        } catch (ChatAuthorizationException e) {
            log.warn("流式请求鉴权失败: {}", e.getMessage());
            return buildErrorEmitter(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()), "authorization_error");
        } catch (BusinessException e) {
            log.warn("流式请求业务异常: {}", e.getMessage());
            return buildErrorEmitter(e.getCode(), friendlyMessage(e.getCode(), e.getMessage()), "business_error");
        } catch (ChatUpstreamException e) {
            log.warn("流式请求上游异常: {}", e.getMessage());
            return buildErrorEmitter(e.getStatusCode(), friendlyMessage(e.getStatusCode(), e.getMessage()), "upstream_error");
        } catch (ChatProcessingException e) {
            log.error("流式请求处理异常: {}", e.getMessage(), e);
            return buildErrorEmitter(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), "processing_error");
        } catch (Exception e) {
            log.error("流式API调用失败: {}", e.getMessage(), e);
            return buildErrorEmitter(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "聊天服务暂时不可用，请稍后重试", "processing_error");
        }
    }

    private SseEmitter buildErrorEmitter(int statusCode, String message, String errorType) {
        SseEmitter emitter = new SseEmitter();
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("code", statusCode);
        error.put("message", message);
        error.put("type", errorType);
        payload.put("error", error);

        try {
            log.warn("流式错误响应: status={}, type={}, message={}", statusCode, errorType, message);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(payload)));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
        return emitter;
    }

    /**
     * 构建 Claude 格式的错误 SSE 响应
     */
    private SseEmitter buildClaudeErrorEmitter(int statusCode, String message) {
        SseEmitter emitter = new SseEmitter();
        ClaudeErrorResponse errorResponse = ClaudeErrorResponse.fromStatusCode(statusCode, message);

        try {
            log.warn("Claude 流式错误响应: status={}, message={}", statusCode, message);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(errorResponse)));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
        return emitter;
    }

    private void normalizeModelName(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getModel())) {
            return;
        }

        if (!modelMappingProperties.isEnabled()) {
            return;
        }

        Map<String, String> aliases = modelMappingProperties.getAliases();
        if (aliases == null || aliases.isEmpty()) {
            return;
        }

        String raw = request.getModel().trim();
        String key = raw.toLowerCase(Locale.ROOT);

        String normalized = modelMappingProperties.getAliases()
                .getOrDefault(key, raw);

        if (!normalized.equals(raw)) {
            log.debug("模型名规范化: {} -> {}", raw, normalized);
            request.setModel(normalized);
        }
    }

    private String friendlyMessage(int code, String fallback) {
        return switch (code) {
            case ApiErrorCodes.UNAUTHORIZED -> "身份验证失败，请检查 API Key 是否正确且未过期";
            case ApiErrorCodes.PAYMENT_REQUIRED -> "余额不足，请先充值后再试";
            case ApiErrorCodes.FORBIDDEN -> "账户已被禁用或无权访问该模型";
            case ApiErrorCodes.NOT_FOUND -> "请求的模型不存在或暂未开放";
            case ApiErrorCodes.TOO_MANY_REQUESTS -> "请求过于频繁，请稍后重试";
            default -> fallback != null ? fallback : "请求处理失败，请稍后重试";
        };
    }
}
