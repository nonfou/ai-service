package com.nonfou.github.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude Messages API 请求 DTO
 * 用于 /v1/messages 接口
 * 参考: https://docs.anthropic.com/en/api/messages
 */
@Data
public class ClaudeRequest {

    /**
     * 模型名称
     * 如: claude-3-5-sonnet-20241022, claude-3-opus-20240229
     */
    @NotBlank(message = "模型名称不能为空")
    private String model;

    /**
     * 消息列表
     * 用户和助手消息交替出现
     */
    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    /**
     * 最大输出 token 数
     * 必填参数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 系统提示
     * 可以是字符串或内容块列表
     */
    private Object system;

    /**
     * 是否流式输出
     */
    private Boolean stream = false;

    /**
     * 温度参数 (0-1)
     * 控制输出随机性
     */
    private Double temperature;

    /**
     * 核采样参数 (0-1)
     * 与 temperature 互斥使用
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * Top-K 采样
     * Claude 特有参数
     */
    @JsonProperty("top_k")
    private Integer topK;

    /**
     * 停止序列列表
     */
    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    /**
     * 工具定义列表
     */
    private List<Tool> tools;

    /**
     * 工具选择策略
     * auto | any | tool (指定工具名)
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 元数据
     * 最多 16 个键值对
     */
    private Map<String, String> metadata;

    /**
     * 流式选项
     */
    @JsonProperty("stream_options")
    private Object streamOptions;

    /**
     * 额外参数
     */
    private Map<String, Object> additionalParams;

    /**
     * Claude 消息格式
     */
    @Data
    public static class Message {
        /**
         * 角色: user 或 assistant
         */
        private String role;

        /**
         * 消息内容
         * 可以是字符串或内容块列表
         */
        private Object content;
    }

    /**
     * Claude 工具定义
     */
    @Data
    public static class Tool {
        /**
         * 工具名称
         */
        private String name;

        /**
         * 工具描述
         */
        private String description;

        /**
         * 输入参数 JSON Schema
         */
        @JsonProperty("input_schema")
        private Object inputSchema;

        /**
         * 缓存控制
         */
        @JsonProperty("cache_control")
        private Object cacheControl;
    }
}
