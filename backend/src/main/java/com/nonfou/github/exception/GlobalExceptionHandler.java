package com.nonfou.github.exception;

import com.nonfou.github.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.warn("业务异常: code={}, message={}, uri={}, ip={}",
            e.getCode(), e.getMessage(), request.getRequestURI(), ip);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Spring Security 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.warn("⚠️ 认证失败: uri={}, ip={}, error={}",
            request.getRequestURI(), ip, e.getMessage());
        return Result.error(401, "认证失败，请重新登录");
    }

    /**
     * 处理 Spring Security 授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        String ip = getClientIp(request);
        Long userId = getCurrentUserId();
        log.warn("⚠️ 权限不足: uri={}, ip={}, userId={}",
            request.getRequestURI(), ip, userId);
        return Result.error(403, "权限不足");
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public Result<Void> handleDatabaseException(Exception e, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.error("❌ 数据库异常: uri={}, ip={}, error={}",
            request.getRequestURI(), ip, e.getMessage(), e);
        return Result.error("系统繁忙，请稍后重试");
    }

    /**
     * 处理 HTTP 客户端异常
     */
    @ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class})
    public Result<Void> handleHttpException(Exception e, HttpServletRequest request) {
        log.error("❌ 外部服务调用失败: uri={}, error={}",
            request.getRequestURI(), e.getMessage());
        return Result.error("外部服务暂时不可用");
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("绑定异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理通用 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.error("❌ 未处理的运行时异常: uri={}, ip={}, error={}",
            request.getRequestURI(), ip, e.getMessage(), e);
        return Result.error("系统错误，请联系管理员");
    }

    /**
     * 处理其他异常 (兜底)
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.error("❌ 系统异常: uri={}, ip={}, error={}",
            request.getRequestURI(), ip, e.getMessage(), e);
        return Result.error("系统错误，请稍后重试");
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是多级代理,取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                return (Long) auth.getPrincipal();
            }
        } catch (Exception e) {
            // 忽略异常,返回null
        }
        return null;
    }
}
