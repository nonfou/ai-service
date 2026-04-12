package com.nonfou.github.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.common.Result;
import com.nonfou.github.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存的速率限制过滤器
 * 使用滑动窗口算法限制请求频率
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final RateLimitProperties rateLimitProperties;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final long WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final Map<String, LinkedList<Long>> requestWindows = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper, RateLimitProperties rateLimitProperties) {
        this.objectMapper = objectMapper;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Scheduled(fixedDelay = 120_000)
    public void cleanup() {
        long windowStart = System.currentTimeMillis() - WINDOW_MILLIS;
        requestWindows.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(ts -> ts < windowStart);
            return entry.getValue().isEmpty();
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        if (uri.startsWith("/actuator") || uri.equals("/health") || uri.equals("/ready") || uri.equals("/live")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIdentifier = getClientIdentifier(request);
        int limit = uri.startsWith("/v1/") ? rateLimitProperties.getApiRequestsPerMinute() : rateLimitProperties.getRequestsPerMinute();

        if (!isAllowed(clientIdentifier, limit)) {
            log.warn("速率限制触发: client={}, uri={}, limit={}/min", clientIdentifier, uri, limit);
            sendRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return "user:" + authHeader.substring(7).hashCode();
        }

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "apikey:" + apiKey.hashCode();
        }

        return "ip:" + getClientIp(request);
    }

    private boolean isAllowed(String clientIdentifier, int limit) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + clientIdentifier;
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - WINDOW_MILLIS;

            LinkedList<Long> timestamps = requestWindows.computeIfAbsent(key, k -> new LinkedList<>());

            synchronized (timestamps) {
                timestamps.removeIf(ts -> ts < windowStart);
                if (timestamps.size() >= limit) {
                    return false;
                }
                timestamps.add(currentTime);
            }

            return true;
        } catch (Exception e) {
            log.error("速率限制检查失败，降级放行: {}", e.getMessage());
            return true;
        }
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<?> errorResponse = Result.error(429, "请求过于频繁，请稍后再试");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
