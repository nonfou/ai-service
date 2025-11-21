package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.service.ApiCallLogService;
import com.nonfou.github.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API调用统计 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class StatisticsController {

    @Autowired
    private ApiCallLogService apiCallLogService;

    /**
     * 获取API调用日志
     */
    @GetMapping("/api-calls")
    public Result<Page<ApiCall>> getApiCalls(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Page<ApiCall> page = apiCallLogService.getUserApiCalls(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取今日统计
     */
    @GetMapping("/statistics/today")
    public Result<Map<String, Object>> getTodayStatistics() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Map<String, Object> stats = apiCallLogService.getTodayStatistics(userId);
        return Result.success(stats);
    }

    /**
     * 获取使用趋势
     */
    @GetMapping("/statistics/usage")
    public Result<List<Map<String, Object>>> getUsageTrend(
            @RequestParam(defaultValue = "7") int days) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        List<Map<String, Object>> trend = apiCallLogService.getUsageTrend(userId, days);
        return Result.success(trend);
    }

    /**
     * 获取模型使用统计
     */
    @GetMapping("/statistics/model-usage")
    public Result<List<Map<String, Object>>> getModelUsageStatistics() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        List<Map<String, Object>> stats = apiCallLogService.getModelUsageStatistics(userId);
        return Result.success(stats);
    }

}
