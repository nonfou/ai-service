package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Responses API 响应 DTO
 * 用于 Codex 系列模型响应
 */
@Data
public class ResponsesResponse {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 对象类型
     */
    private String object;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 创建时间戳
     */
    @JsonProperty("created_at")
    private Long createdAt;

    /**
     * 响应状态
     */
    private String status;

    /**
     * 输出内容列表
     */
    private List<OutputItem> output;

    /**
     * Token 使用统计
     */
    private Usage usage;

    /**
     * 输出项
     */
    @Data
    public static class OutputItem {
        private String id;
        private String type;
        private String role;
        private String status;
        private List<Content> content;
        private List<Object> summary;
    }

    /**
     * 内容块
     */
    @Data
    public static class Content {
        private String type;
        private String text;
        private List<Object> annotations;
    }

    /**
     * Token 使用统计
     */
    @Data
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        @JsonProperty("input_tokens_details")
        private TokenDetails inputTokensDetails;

        @JsonProperty("output_tokens_details")
        private TokenDetails outputTokensDetails;
    }

    /**
     * Token 详情
     */
    @Data
    public static class TokenDetails {
        @JsonProperty("cached_tokens")
        private Integer cachedTokens;

        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;
    }
}
