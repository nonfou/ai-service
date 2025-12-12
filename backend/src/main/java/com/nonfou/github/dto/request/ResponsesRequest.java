package com.nonfou.github.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Responses API 请求 DTO
 * 用于 Codex 系列模型（gpt-5.1-codex, gpt-5.1-codex-mini, gpt-5.1-codex-max）
 * 兼容 OpenAI Responses API 规范
 */
@Data
public class ResponsesRequest {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String model;

    /**
     * 输入内容（字符串或消息列表）
     */
    @NotNull(message = "输入内容不能为空")
    private Object input;

    /**
     * 是否流式输出
     */
    private Boolean stream = false;

    /**
     * 系统指令
     */
    private String instructions;

    /**
     * 最大输出 token 数
     */
    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

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
     * 工具列表（用于 Codex CLI 执行 shell 命令等）
     */
    private List<Object> tools;

    /**
     * 工具选择策略
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 是否允许并行工具调用
     */
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    /**
     * 最大工具调用次数
     */
    @JsonProperty("max_tool_calls")
    private Integer maxToolCalls;

    /**
     * 上一个响应的 ID，用于多轮对话
     */
    @JsonProperty("previous_response_id")
    private String previousResponseId;

    /**
     * 会话 ID 或会话对象
     */
    private Object conversation;

    /**
     * 是否在后台运行
     */
    private Boolean background;

    /**
     * 是否存储响应
     */
    private Boolean store;

    /**
     * 截断策略 (auto/disabled)
     */
    private String truncation;

    /**
     * 包含额外输出数据
     */
    private List<String> include;

    /**
     * 推理配置（用于 o-series 和 gpt-5 模型）
     */
    private Object reasoning;

    /**
     * 文本输出配置
     */
    private Object text;

    /**
     * 元数据（最多 16 个键值对）
     */
    private Map<String, String> metadata;

    /**
     * 服务层级 (auto/default/flex/priority)
     */
    @JsonProperty("service_tier")
    private String serviceTier;

    /**
     * 安全标识符
     */
    @JsonProperty("safety_identifier")
    private String safetyIdentifier;

    /**
     * 提示缓存键
     */
    @JsonProperty("prompt_cache_key")
    private String promptCacheKey;

    /**
     * 提示缓存保留策略
     */
    @JsonProperty("prompt_cache_retention")
    private String promptCacheRetention;

    /**
     * 流式选项
     */
    @JsonProperty("stream_options")
    private Object streamOptions;

    /**
     * 额外参数（用于透传未知字段）
     */
    private Map<String, Object> additionalParams;
}
