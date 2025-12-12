package com.nonfou.github.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Copilot 聊天请求
 * 兼容 OpenAI Chat Completions API 和 Anthropic Claude Messages API
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
     * 最大 tokens (OpenAI: max_tokens, Claude: max_tokens)
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数 (0-2 for OpenAI, 0-1 for Claude)
     */
    private Double temperature;

    /**
     * 核采样参数 (0-1)
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * Top-K 采样 (Claude 特有)
     */
    @JsonProperty("top_k")
    private Integer topK;

    /**
     * 系统提示 (Claude Messages API 格式)
     * 可以是字符串或内容块列表
     */
    private Object system;

    /**
     * 工具列表
     */
    private List<Object> tools;

    /**
     * 工具选择策略
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 停止序列
     */
    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    /**
     * 停止序列 (OpenAI 格式)
     */
    private Object stop;

    /**
     * 元数据
     */
    private Map<String, String> metadata;

    /**
     * 流式选项 (OpenAI)
     */
    @JsonProperty("stream_options")
    private Object streamOptions;

    /**
     * 其他参数
     */
    private Map<String, Object> additionalParams;

    @Data
    public static class Message {
        private String role;
        /**
         * 消息内容可为字符串或内容块列表（OpenAI/Anthropic 兼容格式）
         */
        private Object content;
    }
}
