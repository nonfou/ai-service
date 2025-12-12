package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude content_block_delta 事件
 * 内容块增量更新时发送
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentBlockDeltaEvent extends ClaudeStreamEvent {

    private Integer index;

    private Delta delta;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String type;

        /**
         * text_delta 类型时的文本内容
         */
        private String text;

        /**
         * input_json_delta 类型时的部分 JSON
         */
        @JsonProperty("partial_json")
        private String partialJson;

        /**
         * thinking_delta 类型时的思考内容
         */
        private String thinking;

        /**
         * signature_delta 类型时的签名
         */
        private String signature;
    }

    @Override
    public String getTextContent() {
        if (delta != null && "text_delta".equals(delta.getType())) {
            return delta.getText();
        }
        return null;
    }
}
