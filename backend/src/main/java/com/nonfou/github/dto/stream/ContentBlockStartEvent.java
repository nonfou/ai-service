package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude content_block_start 事件
 * 内容块开始时发送
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentBlockStartEvent extends ClaudeStreamEvent {

    private Integer index;

    @JsonProperty("content_block")
    private ContentBlock contentBlock;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentBlock {
        private String type;
        private String text;
        private String id;
        private String name;
        private Object input;
        private String thinking;
    }
}
