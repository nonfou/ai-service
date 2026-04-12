package com.nonfou.github.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.common.Result;
import com.nonfou.github.config.AdminSecurityProperties;
import com.nonfou.github.service.InMemoryCacheService;
import com.nonfou.github.util.LogMaskUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 管理后台安全拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSecurityInterceptor implements HandlerInterceptor {

    private final AdminSecurityProperties adminSecurityProperties;
    private final InMemoryCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 只拦截管理后台接口
        if (!requestURI.startsWith("/admin")) {
            return true;
        }

        // 获取客户端IP
        String clientIp = getClientIp(request);

        // 1. IP白名单检查
        if (adminSecurityProperties.isEnableIpWhitelist() && !isIpAllowed(clientIp)) {
            log.warn("⚠️ 管理后台访问被拒绝(IP不在白名单): IP={}, URI={}", LogMaskUtil.maskIp(clientIp), requestURI);
            sendErrorResponse(response, "访问被拒绝: IP地址不在白名单中");
            return false;
        }

        // 2. 请求频率限制检查
        if (adminSecurityProperties.isEnableRateLimit() && !checkRateLimit(clientIp)) {
            log.warn("⚠️ 管理后台访问被限流: IP={}, URI={}, 限制={}/分钟",
                LogMaskUtil.maskIp(clientIp), requestURI, adminSecurityProperties.getRateLimit());
            sendErrorResponse(response, "请求过于频繁,请稍后再试");
            return false;
        }

        // 记录访问日志(便于安全审计)
        log.info("管理后台访问: IP={}, URI={}, Method={}", LogMaskUtil.maskIp(clientIp), requestURI, request.getMethod());

        return true;
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
     * 检查IP是否在白名单中
     */
    private boolean isIpAllowed(String clientIp) {
        if (adminSecurityProperties.getIpWhitelist().isEmpty()) {
            return true;
        }

        // 支持IP段匹配 (如: 192.168.1.*)
        for (String allowedIp : adminSecurityProperties.getIpWhitelist()) {
            if (allowedIp.equals(clientIp)) {
                return true;
            }
            if (allowedIp.endsWith("*")) {
                String prefix = allowedIp.substring(0, allowedIp.length() - 1);
                if (clientIp.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查请求频率限制
     */
    private boolean checkRateLimit(String clientIp) {
        String key = "admin:ratelimit:" + clientIp;

        String countStr = cacheService.get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= adminSecurityProperties.getRateLimit()) {
            return false;
        }

        if (count == 0) {
            cacheService.set(key, "1", 1, TimeUnit.MINUTES);
        } else {
            cacheService.increment(key);
        }

        return true;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.error(message);
        String json = objectMapper.writeValueAsString(result);

        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
