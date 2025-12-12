package com.nonfou.github.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions API 请求 DTO
 * 用于 /v1/chat/completions 接口
 * 参考: https://platform.openai.com/docs/api-reference/chat/create
 */
@Data
public class ChatRequest {

    /**
     * 模型名称
     * 如: gpt-4o, gpt-4-turbo, gpt-3.5-turbo
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
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature;

    /**
     * 核采样参数 (0-1)
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * 频率惩罚 (-2.0 到 2.0)
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /**
     * 存在惩罚 (-2.0 到 2.0)
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * 停止序列
     */
    private Object stop;

    /**
     * 工具定义列表
     */
    private List<Tool> tools;

    /**
     * 工具选择策略
     * none | auto | required | { type: "function", function: { name: "xxx" } }
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 是否并行调用工具
     */
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    /**
     * 响应格式
     */
    @JsonProperty("response_format")
    private Object responseFormat;

    /**
     * 随机种子
     */
    private Integer seed;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 流式选项
     */
    @JsonProperty("stream_options")
    private Object streamOptions;

    /**
     * 日志概率
     */
    private Boolean logprobs;

    /**
     * Top 日志概率数量
     */
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    /**
     * 生成数量
     */
    private Integer n;

    /**
     * 其他参数
     */
    private Map<String, Object> additionalParams;

    /**
     * OpenAI 消息格式
     */
    @Data
    public static class Message {
        /**
         * 角色: system, user, assistant, tool
         */
        private String role;

        /**
         * 消息内容
         * 可以是字符串或内容块列表
         */
        private Object content;

        /**
         * 消息名称（可选）
         */
        private String name;

        /**
         * 工具调用列表（assistant 消息）
         */
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        /**
         * 工具调用 ID（tool 消息）
         */
        @JsonProperty("tool_call_id")
        private String toolCallId;
    }

    /**
     * OpenAI 工具定义
     */
    @Data
    public static class Tool {
        /**
         * 工具类型，固定为 "function"
         */
        private String type = "function";

        /**
         * 函数定义
         */
        private Function function;
    }

    /**
     * OpenAI 函数定义
     */
    @Data
    public static class Function {
        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数描述
         */
        private String description;

        /**
         * 参数 JSON Schema
         */
        private Object parameters;

        /**
         * 是否严格模式
         */
        private Boolean strict;
    }

    /**
     * OpenAI 工具调用
     */
    @Data
    public static class ToolCall {
        /**
         * 工具调用 ID
         */
        private String id;

        /**
         * 类型，固定为 "function"
         */
        private String type = "function";

        /**
         * 函数调用详情
         */
        private FunctionCall function;
    }

    /**
     * OpenAI 函数调用
     */
    @Data
    public static class FunctionCall {
        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数参数（JSON 字符串）
         */
        private String arguments;
    }
}
