package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude message_delta 事件
 * 消息更新时发送，包含 stop_reason 和最终 usage
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDeltaEvent extends ClaudeStreamEvent {

    private Delta delta;

    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        @JsonProperty("stop_reason")
        private String stopReason;

        @JsonProperty("stop_sequence")
        private String stopSequence;
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
        return usage != null;
    }

    @Override
    public int getOutputTokens() {
        if (usage != null && usage.getOutputTokens() != null) {
            return usage.getOutputTokens();
        }
        return 0;
    }

    @Override
    public int getInputTokens() {
        if (usage != null && usage.getInputTokens() != null) {
            return usage.getInputTokens();
        }
        return 0;
    }

    @Override
    public int getCacheReadInputTokens() {
        if (usage != null && usage.getCacheReadInputTokens() != null) {
            return usage.getCacheReadInputTokens();
        }
        return 0;
    }

    @Override
    public int getCacheCreationInputTokens() {
        if (usage != null && usage.getCacheCreationInputTokens() != null) {
            return usage.getCacheCreationInputTokens();
        }
        return 0;
    }
}
