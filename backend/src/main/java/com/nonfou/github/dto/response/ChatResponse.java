package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Copilot 聊天响应
 */
@Data
public class ChatResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        /**
         * OpenAI 格式的 prompt token 详情（包含缓存信息）
         */
        @JsonProperty("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;

        /**
         * Anthropic 格式的缓存读取 token 数
         */
        @JsonProperty("cache_read_input_tokens")
        private Integer cacheReadInputTokens;

        /**
         * Anthropic 格式的缓存写入 token 数
         */
        @JsonProperty("cache_creation_input_tokens")
        private Integer cacheCreationInputTokens;
    }

    /**
     * Prompt token 详情（OpenAI 格式）
     */
    @Data
    public static class PromptTokensDetails {
        /**
         * 缓存命中的 token 数
         */
        @JsonProperty("cached_tokens")
        private Integer cachedTokens;
    }
}
