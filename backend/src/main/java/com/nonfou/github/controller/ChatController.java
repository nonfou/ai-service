package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.service.ChatWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天 API Controller - 委托给 ChatWorkflowService 处理核心逻辑
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatWorkflowService chatWorkflowService;

    /**
     * 聊天接口（非流式）
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated ChatRequest request) {

        try {
            ChatResponse response = chatWorkflowService.handleChat(authorization, request);
            return Result.success(response);
        } catch (ChatAuthorizationException e) {
            return Result.error(e.getStatusCode(), e.getMessage());
        } catch (ChatProcessingException e) {
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("API调用失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 兼容 OpenAI 格式的接口（根据 stream 字段决定返回类型）
     */
    @PostMapping("/v1/chat/completions")
    public Object chatCompletions(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated ChatRequest request) {

        if (Boolean.TRUE.equals(request.getStream())) {
            return chatCompletionsStream(authorization, request);
        }

        return chat(authorization, request);
    }

    /**
     * 流式聊天接口
     */
    public SseEmitter chatCompletionsStream(
            String authorization,
            ChatRequest request) {

        try {
            return chatWorkflowService.handleStream(authorization, request);
        } catch (ChatAuthorizationException e) {
            return buildErrorEmitter(e.getMessage());
        } catch (ChatProcessingException e) {
            return buildErrorEmitter(e.getMessage());
        } catch (Exception e) {
            log.error("流式API调用失败: {}", e.getMessage(), e);
            return buildErrorEmitter("聊天服务暂时不可用，请稍后重试");
        }
    }

    private SseEmitter buildErrorEmitter(String message) {
        SseEmitter emitter = new SseEmitter();
        emitter.completeWithError(new RuntimeException(message));
        return emitter;
    }
}
