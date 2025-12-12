package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude message_stop 事件
 * 消息结束时发送
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageStopEvent extends ClaudeStreamEvent {

    @Override
    public boolean isTerminal() {
        return true;
    }
}
