package com.nonfou.github.controller;

import com.nonfou.github.service.QuotaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 配额管理控制器
 */
@RestController
@RequestMapping("/api/user/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    /**
     * 获取当前用户的配额信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getQuota(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);

        Map<String, Object> quotaInfo = quotaService.getUserQuotaInfo(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", quotaInfo);

        return ResponseEntity.ok(result);
    }

    /**
     * 管理员：设置用户配额
     */
    @PutMapping("/admin/users/{userId}")
    public ResponseEntity<Map<String, Object>> setUserQuota(
            @PathVariable Long userId,
            @RequestBody SetQuotaRequest request) {

        if ("daily".equals(request.getQuotaType())) {
            quotaService.setUserQuota(userId, "daily", request.getAmount());
        } else if ("monthly".equals(request.getQuotaType())) {
            quotaService.setUserQuota(userId, "monthly", request.getAmount());
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无效的配额类型，支持: daily, monthly"
            ));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "设置配额成功");

        return ResponseEntity.ok(result);
    }

    /**
     * 从 Authentication 中获取用户 ID
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        // TODO: 根据实际的 Authentication 实现获取用户ID
        // 这里假设从 principal 中获取
        Object principal = authentication.getPrincipal();
        if (principal instanceof Map) {
            Object userId = ((Map<?, ?>) principal).get("userId");
            if (userId != null) {
                return Long.parseLong(userId.toString());
            }
        }
        throw new RuntimeException("无法获取用户ID");
    }

    /**
     * 设置配额请求
     */
    @Data
    public static class SetQuotaRequest {
        private String quotaType;  // daily, monthly
        private BigDecimal amount;
    }
}
