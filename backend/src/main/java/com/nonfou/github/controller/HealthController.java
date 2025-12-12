package com.nonfou.github.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供 Kubernetes 风格的健康检查端点
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    /**
     * 存活探针 - 检查应用是否还在运行
     * Kubernetes liveness probe
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * 存活探针（别名）
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> live() {
        return health();
    }

    /**
     * 就绪探针 - 检查应用是否准备好接收流量
     * Kubernetes readiness probe
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();
        boolean allHealthy = true;

        // 检查数据库连接
        try {
            try (Connection conn = dataSource.getConnection()) {
                conn.isValid(2);
            }
            checks.put("database", "UP");
        } catch (Exception e) {
            checks.put("database", "DOWN");
            checks.put("database_error", e.getMessage());
            allHealthy = false;
            log.warn("健康检查: 数据库连接失败 - {}", e.getMessage());
        }

        // 检查 Redis 连接
        try {
            redisTemplate.opsForValue().get("health:check");
            checks.put("redis", "UP");
        } catch (Exception e) {
            checks.put("redis", "DOWN");
            checks.put("redis_error", e.getMessage());
            allHealthy = false;
            log.warn("健康检查: Redis连接失败 - {}", e.getMessage());
        }

        result.put("status", allHealthy ? "UP" : "DOWN");
        result.put("checks", checks);
        result.put("timestamp", System.currentTimeMillis());

        if (allHealthy) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(503).body(result);
        }
    }
}
