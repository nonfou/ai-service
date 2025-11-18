package com.nonfou.github.service.proxy;

import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 抽象聊天代理，封装对下游模型提供方的调用能力。
 */
public interface ModelProxy {

    /**
     * 提供方标识，例如 copilot、openrouter
     */
    String getProvider();

    /**
     * 非流式聊天请求
     */
    ChatResponse chat(ChatRequest request, ApiKey apiKey);

    /**
     * 流式聊天请求
     */
    SseEmitter chatStream(ChatRequest request, ApiKey apiKey);
}
