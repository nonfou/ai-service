package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.annotation.RequireAdmin;
import com.nonfou.github.common.Result;
import com.nonfou.github.dto.response.ModelStatsResponse;
import com.nonfou.github.dto.response.TokenTrendResponse;
import com.nonfou.github.dto.response.UserTokenStatsResponse;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.entity.BalanceLog;
import com.nonfou.github.service.AdminUserStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户统计 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
public class AdminUserStatsController {

    @Autowired
    private AdminUserStatsService adminUserStatsService;

    /**
     * 获取用户Token统计
     */
    @GetMapping("/{userId}/token-stats")
    @RequireAdmin
    public Result<UserTokenStatsResponse> getUserTokenStats(@PathVariable Long userId) {
        try {
            UserTokenStatsResponse stats = adminUserStatsService.getUserTokenStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取用户Token统计失败", e);
            return Result.error("获取用户Token统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户Token趋势数据
     */
    @GetMapping("/{userId}/token-trend")
    @RequireAdmin
    public Result<List<TokenTrendResponse>> getUserTokenTrend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<TokenTrendResponse> trend = adminUserStatsService.getUserTokenTrend(userId, days);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取用户Token趋势失败", e);
            return Result.error("获取用户Token趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户模型使用统计
     */
    @GetMapping("/{userId}/model-stats")
    @RequireAdmin
    public Result<List<ModelStatsResponse>> getUserModelStats(@PathVariable Long userId) {
        try {
            List<ModelStatsResponse> stats = adminUserStatsService.getUserModelStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取用户模型统计失败", e);
            return Result.error("获取用户模型统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户API调用日志
     */
    @GetMapping("/{userId}/api-calls")
    @RequireAdmin
    public Result<Page<ApiCall>> getUserApiCalls(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<ApiCall> apiCalls = adminUserStatsService.getUserApiCalls(userId, pageNum, pageSize);
            return Result.success(apiCalls);
        } catch (Exception e) {
            log.error("获取用户API调用日志失败", e);
            return Result.error("获取用户API调用日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户余额日志
     */
    @GetMapping("/{userId}/balance-logs")
    @RequireAdmin
    public Result<Page<BalanceLog>> getUserBalanceLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<BalanceLog> balanceLogs = adminUserStatsService.getUserBalanceLogs(userId, pageNum, pageSize);
            return Result.success(balanceLogs);
        } catch (Exception e) {
            log.error("获取用户余额日志失败", e);
            return Result.error("获取用户余额日志失败: " + e.getMessage());
        }
    }
}
