package com.nonfou.github.exception;

/**
 * 聊天流程通用异常，用于包装非授权类错误并提供友好提示。
 */
public class ChatProcessingException extends RuntimeException {

    public ChatProcessingException(String message) {
        super(message);
    }

    public ChatProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
