package com.nonfou.github.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping({"/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    @GetMapping({"/live", "/api/live"})
    public ResponseEntity<Map<String, Object>> live() {
        return health();
    }

    @GetMapping({"/ready", "/api/ready"})
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
