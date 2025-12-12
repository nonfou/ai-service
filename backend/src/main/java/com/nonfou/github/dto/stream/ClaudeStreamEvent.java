package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Claude SSE 流式事件基类
 * 参考: https://docs.anthropic.com/en/api/messages-streaming
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = ClaudeStreamEvent.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MessageStartEvent.class, name = "message_start"),
        @JsonSubTypes.Type(value = ContentBlockStartEvent.class, name = "content_block_start"),
        @JsonSubTypes.Type(value = ContentBlockDeltaEvent.class, name = "content_block_delta"),
        @JsonSubTypes.Type(value = ContentBlockStopEvent.class, name = "content_block_stop"),
        @JsonSubTypes.Type(value = MessageDeltaEvent.class, name = "message_delta"),
        @JsonSubTypes.Type(value = MessageStopEvent.class, name = "message_stop"),
        @JsonSubTypes.Type(value = PingEvent.class, name = "ping"),
        @JsonSubTypes.Type(value = ErrorEvent.class, name = "error")
})
public class ClaudeStreamEvent {

    /**
     * 事件类型
     */
    private String type;

    /**
     * 原始 JSON 字符串（用于透传）
     */
    private transient String rawJson;

    /**
     * 检查是否包含 token 使用信息
     */
    public boolean hasUsage() {
        return false;
    }

    /**
     * 获取 input tokens（如果有）
     */
    public int getInputTokens() {
        return 0;
    }

    /**
     * 获取 output tokens（如果有）
     */
    public int getOutputTokens() {
        return 0;
    }

    /**
     * 获取缓存读取 tokens（如果有）
     */
    public int getCacheReadInputTokens() {
        return 0;
    }

    /**
     * 获取缓存创建 tokens（如果有）
     */
    public int getCacheCreationInputTokens() {
        return 0;
    }

    /**
     * 获取文本内容（如果有）
     */
    public String getTextContent() {
        return null;
    }

    /**
     * 是否为结束事件
     */
    public boolean isTerminal() {
        return "message_stop".equals(type) || "error".equals(type);
    }
}
