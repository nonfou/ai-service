package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.entity.User;
import com.nonfou.github.service.BalanceService;
import com.nonfou.github.service.SubscriptionService;
import com.nonfou.github.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        User user = balanceService.getUserById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("email", user.getEmail());
        data.put("balance", user.getBalance());
        data.put("apiKey", maskApiKey(user.getApiKey()));
        data.put("status", user.getStatus());
        data.put("createdAt", user.getCreatedAt());

        return Result.success(data);
    }

    /**
     * 获取用户余额
     */
    @GetMapping("/balance")
    public Result<Map<String, Object>> getUserBalance() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        User user = balanceService.getUserById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("balance", user.getBalance());
        data.put("userId", user.getId());

        return Result.success(data);
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        User user = balanceService.getUserById(userId);
        var activeSubscription = subscriptionService.getActiveSubscription(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("balance", user.getBalance());
        data.put("activeSubscription", activeSubscription);
        data.put("email", user.getEmail());
        data.put("status", user.getStatus());

        return Result.success(data);
    }

    /**
     * 脱敏API密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return apiKey;
        }
        return apiKey.substring(0, 8) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
