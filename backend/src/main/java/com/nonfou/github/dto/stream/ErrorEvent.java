package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Claude error 事件
 * 错误事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorEvent extends ClaudeStreamEvent {

    private Error error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        private String type;
        private String message;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
