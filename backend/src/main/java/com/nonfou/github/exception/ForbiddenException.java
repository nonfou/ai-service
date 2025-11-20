package com.nonfou.github.exception;

/**
 * 禁止访问异常 (403)
 * <p>
 * 用于表示用户已认证但没有权限访问资源的情况
 * </p>
 */
public class ForbiddenException extends BusinessException {

    /**
     * 构造方法
     *
     * @param message 错误信息
     */
    public ForbiddenException(String message) {
        super(403, message);
    }

    /**
     * 构造方法 (带异常原因)
     *
     * @param message 错误信息
     * @param cause   异常原因
     */
    public ForbiddenException(String message, Throwable cause) {
        super(403, message, cause);
    }
}
