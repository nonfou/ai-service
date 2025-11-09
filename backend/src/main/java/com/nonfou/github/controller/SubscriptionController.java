package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.Subscription;
import com.nonfou.github.entity.SubscriptionPlan;
import com.nonfou.github.service.SubscriptionService;
import com.nonfou.github.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订阅套餐 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取所有可用套餐
     */
    @GetMapping("/plans")
    public Result<List<SubscriptionPlan>> getAvailablePlans() {
        List<SubscriptionPlan> plans = subscriptionService.getAvailablePlans();
        return Result.success(plans);
    }

    /**
     * 订阅套餐
     */
    @PostMapping("/subscribe")
    public Result<Subscription> subscribe(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Long> body) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            Long planId = body.get("planId");
            if (planId == null) {
                return Result.error("套餐ID不能为空");
            }

            Subscription subscription = subscriptionService.subscribe(userId, planId);
            return Result.success(subscription);
        } catch (Exception e) {
            log.error("订阅失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订阅历史
     */
    @GetMapping("/history")
    public Result<Page<Subscription>> getSubscriptionHistory(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Page<Subscription> page = subscriptionService.getUserSubscriptions(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 取消订阅
     */
    @PostMapping("/{subscriptionId}/cancel")
    public Result<Void> cancelSubscription(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long subscriptionId) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            subscriptionService.cancelSubscription(userId, subscriptionId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消订阅失败", e);
            return Result.error(e.getMessage());
        }
    }

    private Long getUserIdFromToken(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        if (!jwtUtil.validateToken(token)) {
            return null;
        }

        return jwtUtil.getUserIdFromToken(token);
    }
}
