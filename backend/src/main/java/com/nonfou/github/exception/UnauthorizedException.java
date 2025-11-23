package com.nonfou.github.exception;

/**
 * 未授权异常 (401)
 * <p>
 * 用于表示用户未认证或认证失败的情况
 * </p>
 */
public class UnauthorizedException extends BusinessException {

    /**
     * 构造方法
     *
     * @param message 错误信息
     */
    public UnauthorizedException(String message) {
        super(ApiErrorCodes.UNAUTHORIZED, message);
    }

    /**
     * 构造方法 (带异常原因)
     *
     * @param message 错误信息
     * @param cause   异常原因
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(ApiErrorCodes.UNAUTHORIZED, message, cause);
}
}
