package com.nonfou.github.exception;

/**
 * 聊天接口授权异常，携带自定义状态码。
 */
public class ChatAuthorizationException extends RuntimeException {

    private final int statusCode;

    public ChatAuthorizationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
