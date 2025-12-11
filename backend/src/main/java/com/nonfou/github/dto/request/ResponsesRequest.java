package com.nonfou.github.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * Responses API 请求 DTO
 * 用于 Codex 系列模型（gpt-5.1-codex, gpt-5.1-codex-mini, gpt-5.1-codex-max）
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
     * 温度参数
     */
    private Double temperature;

    /**
     * 额外参数
     */
    private Map<String, Object> additionalParams;
}
