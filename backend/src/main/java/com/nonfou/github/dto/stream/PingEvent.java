package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude ping 事件
 * 心跳保活事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PingEvent extends ClaudeStreamEvent {
    // ping 事件没有额外字段
}
