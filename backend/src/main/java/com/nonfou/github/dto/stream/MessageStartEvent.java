package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude message_start 事件
 * 流式响应开始时发送，包含消息元数据和初始 usage
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageStartEvent extends ClaudeStreamEvent {

    private Message message;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String id;
        private String type;
        private String role;
        private Object content;
        private String model;

        @JsonProperty("stop_reason")
        private String stopReason;

        @JsonProperty("stop_sequence")
        private String stopSequence;

        private Usage usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;

        @JsonProperty("cache_read_input_tokens")
        private Integer cacheReadInputTokens;

        @JsonProperty("cache_creation_input_tokens")
        private Integer cacheCreationInputTokens;
    }

    @Override
    public boolean hasUsage() {
        return message != null && message.getUsage() != null;
    }

    @Override
    public int getInputTokens() {
        if (message != null && message.getUsage() != null && message.getUsage().getInputTokens() != null) {
            return message.getUsage().getInputTokens();
        }
        return 0;
    }

    @Override
    public int getCacheReadInputTokens() {
        if (message != null && message.getUsage() != null && message.getUsage().getCacheReadInputTokens() != null) {
            return message.getUsage().getCacheReadInputTokens();
        }
        return 0;
    }

    @Override
    public int getCacheCreationInputTokens() {
        if (message != null && message.getUsage() != null && message.getUsage().getCacheCreationInputTokens() != null) {
            return message.getUsage().getCacheCreationInputTokens();
        }
        return 0;
    }
}
