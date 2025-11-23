package com.nonfou.github.exception;

/**
 * API 错误码常量定义，统一管理对外返回的业务状态码。
 */
public final class ApiErrorCodes {

    private ApiErrorCodes() {
        // utility class
    }

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int PAYMENT_REQUIRED = 402;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL_ERROR = 500;
}
