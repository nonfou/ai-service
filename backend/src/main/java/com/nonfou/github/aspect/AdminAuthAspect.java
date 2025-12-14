package com.nonfou.github.aspect;

import com.nonfou.github.config.ServerRuntimeState;
import com.nonfou.github.exception.ForbiddenException;
import com.nonfou.github.exception.UnauthorizedException;
import com.nonfou.github.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;

/**
 * 管理员权限验证切面
 */
@Slf4j
@Aspect
@Component
public class AdminAuthAspect {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ServerRuntimeState serverRuntimeState;

    /**
     * 环绕通知,验证管理员权限
     */
    @Around("@annotation(com.nonfou.github.annotation.RequireAdmin) || " +
            "(@within(com.nonfou.github.annotation.RequireAdmin) && !@annotation(org.springframework.web.bind.annotation.PostMapping))")
    public Object checkAdminPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 检查是否是登录接口(通过方法名判断)
        String methodName = method.getName();
        if ("login".equals(methodName)) {
            log.debug("登录接口,跳过权限验证");
            return joinPoint.proceed();
        }

        // 获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        HttpServletRequest request = attributes.getRequest();

        // 获取Authorization header
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            log.warn("未提供认证令牌: {} {}", request.getMethod(), request.getRequestURI());
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        // 提取token
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        // 验证token有效性
        if (!jwtUtil.validateToken(token)) {
            log.warn("无效的认证令牌: {} {}", request.getMethod(), request.getRequestURI());
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        // 验证ADMIN角色
        String role = jwtUtil.getRoleFromToken(token);
        if (!"ADMIN".equals(role) && !"admin".equals(role) && !"super_admin".equals(role)) {
            String email = jwtUtil.getEmailFromToken(token);
            log.warn("非管理员尝试访问管理接口: user={}, role={}, uri={}",
                    email, role, request.getRequestURI());
            throw new ForbiddenException("权限不足，无法访问该资源");
        }

        // 检查令牌签发时间,服务重启后需重新登录
        Date issuedAt = jwtUtil.getIssuedAt(token);
        Instant serverStartTime = serverRuntimeState.getStartTime();

        if (issuedAt == null) {
            log.warn("认证令牌缺少签发时间: uri={}", request.getRequestURI());
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        if (serverStartTime != null && issuedAt.toInstant().isBefore(serverStartTime)) {
            log.info("检测到服务重启前旧令牌,强制要求重新登录: uri={}", request.getRequestURI());
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        // 权限验证通过,继续执行
        Long adminId = jwtUtil.getUserIdFromToken(token);
        log.debug("管理员访问: adminId={}, uri={}", adminId, request.getRequestURI());

        return joinPoint.proceed();
    }
}
