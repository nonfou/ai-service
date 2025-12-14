package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.service.BackendAccountService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 后端账户管理控制器
 * 管理员专用API
 */
@RestController
@RequestMapping("/admin/backend-accounts")
@RequiredArgsConstructor
public class BackendAccountController {

    private final BackendAccountService backendAccountService;

    /**
     * 分页查询账户列表
     */
    @GetMapping
    public ResponseEntity<Page<BackendAccount>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status) {

        Page<BackendAccount> page = backendAccountService.getAccountPage(current, size, provider, status);

        // 隐藏敏感的 access_token
        page.getRecords().forEach(account -> account.setAccessToken("***"));

        return ResponseEntity.ok(page);
    }

    /**
     * 获取账户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<BackendAccount> getById(@PathVariable Long id) {
        BackendAccount account = backendAccountService.getAccountById(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        // 隐藏敏感的 access_token
        account.setAccessToken("***");

        return ResponseEntity.ok(account);
    }

    /**
     * 创建账户
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateAccountRequest request) {
        BackendAccount account = new BackendAccount();
        account.setAccountName(request.getAccountName());
        account.setProvider(request.getProvider());
        account.setAccessToken(request.getAccessToken());
        account.setPriority(request.getPriority());
        account.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        account.setMaxConcurrent(request.getMaxConcurrent() != null ? request.getMaxConcurrent() : 10);
        account.setRateLimitPerMinute(request.getRateLimitPerMinute() != null ? request.getRateLimitPerMinute() : 60);
        account.setCostMultiplier(request.getCostMultiplier() != null ? request.getCostMultiplier() : java.math.BigDecimal.ONE);
        account.setMetadata(request.getMetadata());

        BackendAccount created = backendAccountService.createAccount(account);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "创建账户成功");
        result.put("data", Map.of("id", created.getId()));

        return ResponseEntity.ok(result);
    }

    /**
     * 更新账户
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody UpdateAccountRequest request) {

        BackendAccount account = backendAccountService.getAccountById(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        // 更新字段
        if (request.getAccountName() != null) {
            account.setAccountName(request.getAccountName());
        }
        if (request.getAccessToken() != null && !"***".equals(request.getAccessToken())) {
            account.setAccessToken(request.getAccessToken());
        }
        if (request.getPriority() != null) {
            account.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }
        if (request.getMaxConcurrent() != null) {
            account.setMaxConcurrent(request.getMaxConcurrent());
        }
        if (request.getRateLimitPerMinute() != null) {
            account.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getCostMultiplier() != null) {
            account.setCostMultiplier(request.getCostMultiplier());
        }
        if (request.getMetadata() != null) {
            account.setMetadata(request.getMetadata());
        }

        backendAccountService.updateAccount(account);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "更新账户成功");

        return ResponseEntity.ok(result);
    }

    /**
     * 删除账户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        backendAccountService.deleteAccount(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除账户成功");

        return ResponseEntity.ok(result);
    }

    /**
     * 启用账户
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<Map<String, Object>> enable(@PathVariable Long id) {
        backendAccountService.enableAccount(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "启用账户成功");

        return ResponseEntity.ok(result);
    }

    /**
     * 禁用账户
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<Map<String, Object>> disable(@PathVariable Long id) {
        backendAccountService.disableAccount(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "禁用账户成功");

        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查
     */
    @PostMapping("/{id}/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck(@PathVariable Long id) {
        boolean isHealthy = backendAccountService.healthCheck(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of(
                "accountId", id,
                "isHealthy", isHealthy,
                "status", isHealthy ? "healthy" : "unhealthy"
        ));

        return ResponseEntity.ok(result);
    }

    /**
     * 获取账户统计信息
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long id) {
        BackendAccount account = backendAccountService.getAccountById(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("accountId", id);
        stats.put("accountName", account.getAccountName());
        stats.put("provider", account.getProvider());
        stats.put("status", account.getStatus());
        stats.put("errorCount", account.getErrorCount());
        stats.put("lastUsedAt", account.getLastUsedAt());
        stats.put("lastErrorAt", account.getLastErrorAt());
        stats.put("lastErrorMessage", account.getLastErrorMessage());

        // TODO: 添加更多统计信息（如总请求数、成功率等）

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stats);

        return ResponseEntity.ok(result);
    }

    /**
     * 创建账户请求
     */
    @Data
    public static class CreateAccountRequest {
        private String accountName;
        private String provider;  // copilot, openrouter
        private String accessToken;
        private Integer priority;
        private String status;
        private Integer maxConcurrent;
        private Integer rateLimitPerMinute;
        private java.math.BigDecimal costMultiplier;
        private Map<String, Object> metadata;
    }

    /**
     * 更新账户请求
     */
    @Data
    public static class UpdateAccountRequest {
        private String accountName;
        private String accessToken;
        private Integer priority;
        private String status;
        private Integer maxConcurrent;
        private Integer rateLimitPerMinute;
        private java.math.BigDecimal costMultiplier;
        private Map<String, Object> metadata;
    }
}
