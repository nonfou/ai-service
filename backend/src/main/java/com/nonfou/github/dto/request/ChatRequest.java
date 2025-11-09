package com.nonfou.github.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Copilot 聊天请求
 */
@Data
public class ChatRequest {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型不能为空")
    private String model;

    /**
     * 消息列表
     */
    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    /**
     * 是否流式响应
     */
    private Boolean stream = false;

    /**
     * 最大 tokens
     */
    private Integer maxTokens;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 其他参数
     */
    private Map<String, Object> additionalParams;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
