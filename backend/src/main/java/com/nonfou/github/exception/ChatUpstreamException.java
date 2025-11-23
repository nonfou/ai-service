package com.nonfou.github.exception;

import lombok.Getter;

/**
 * 上游AI服务返回的可预期错误（例如模型不支持、参数无效等），用于向调用方返回友好的提示。
 */
@Getter
public class ChatUpstreamException extends RuntimeException {

    private final int statusCode;

    public ChatUpstreamException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ChatUpstreamException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
