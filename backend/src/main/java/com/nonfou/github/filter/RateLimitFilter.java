package com.nonfou.github.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的速率限制过滤器
 * 使用滑动窗口算法限制请求频率
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.api-requests-per-minute:30}")
    private int apiRequestsPerMinute;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    public RateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        // 跳过健康检查端点
        if (uri.startsWith("/actuator") || uri.equals("/health") || uri.equals("/ready") || uri.equals("/live")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIdentifier = getClientIdentifier(request);
        int limit = uri.startsWith("/v1/") ? apiRequestsPerMinute : requestsPerMinute;

        if (!isAllowed(clientIdentifier, uri, limit)) {
            log.warn("速率限制触发: client={}, uri={}, limit={}/min", clientIdentifier, uri, limit);
            sendRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 获取客户端标识符（优先使用用户ID，否则使用IP）
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // 尝试从 Authorization Header 中获取用户标识
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 使用 token 的哈希作为标识符（避免存储完整token）
            return "user:" + authHeader.substring(7).hashCode();
        }

        // 使用 API Key 作为标识符
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "apikey:" + apiKey.hashCode();
        }

        // 回退到 IP 地址
        return "ip:" + getClientIp(request);
    }

    /**
     * 检查请求是否被允许（滑动窗口算法）
     */
    private boolean isAllowed(String clientIdentifier, String uri, int limit) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + clientIdentifier;
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - TimeUnit.MINUTES.toMillis(1);

            // 移除窗口外的请求记录
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // 获取当前窗口内的请求数
            Long count = redisTemplate.opsForZSet().zCard(key);
            if (count != null && count >= limit) {
                return false;
            }

            // 添加当前请求
            redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);

            // 设置过期时间（窗口大小 + 缓冲）
            redisTemplate.expire(key, Duration.ofMinutes(2));

            return true;
        } catch (Exception e) {
            // Redis 故障时允许请求通过（降级策略）
            log.error("速率限制检查失败，降级放行: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 发送速率限制响应
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<?> errorResponse = Result.error(429, "请求过于频繁，请稍后再试");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
