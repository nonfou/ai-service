package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.mapper.ApiCallMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API调用日志服务
 */
@Slf4j
@Service
public class ApiCallLogService {

    @Autowired
    private ApiCallMapper apiCallMapper;

    /**
     * 获取用户API调用日志（分页）
     */
    public Page<ApiCall> getUserApiCalls(Long userId, int pageNum, int pageSize) {
        Page<ApiCall> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId)
                .orderByDesc(ApiCall::getCreatedAt);

        return apiCallMapper.selectPage(page, wrapper);
    }

    /**
     * 获取今日统计
     */
    public Map<String, Object> getTodayStatistics(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId)
                .between(ApiCall::getCreatedAt, startOfDay, endOfDay);

        List<ApiCall> calls = apiCallMapper.selectList(wrapper);

        // 统计数据
        long totalCalls = calls.size();
        BigDecimal totalCost = calls.stream()
                .map(ApiCall::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long uniqueModels = calls.stream()
                .map(ApiCall::getModel)
                .distinct()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCalls", totalCalls);
        stats.put("totalCost", totalCost);
        stats.put("uniqueModels", uniqueModels);
        stats.put("date", LocalDate.now());

        return stats;
    }

    /**
     * 获取使用趋势（最近7天）
     */
    public List<Map<String, Object>> getUsageTrend(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId)
                .ge(ApiCall::getCreatedAt, startDate)
                .orderByDesc(ApiCall::getCreatedAt);

        List<ApiCall> calls = apiCallMapper.selectList(wrapper);

        // 按日期分组统计
        Map<LocalDate, List<ApiCall>> groupedByDate = calls.stream()
                .collect(Collectors.groupingBy(call -> call.getCreatedAt().toLocalDate()));

        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<ApiCall> dayCalls = entry.getValue();

                    Map<String, Object> dayStats = new HashMap<>();
                    dayStats.put("date", date);
                    dayStats.put("calls", dayCalls.size());
                    dayStats.put("cost", dayCalls.stream()
                            .map(ApiCall::getCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

                    return dayStats;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取模型使用统计
     */
    public List<Map<String, Object>> getModelUsageStatistics(Long userId) {
        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId);

        List<ApiCall> calls = apiCallMapper.selectList(wrapper);

        // 按模型分组统计
        Map<String, List<ApiCall>> groupedByModel = calls.stream()
                .collect(Collectors.groupingBy(ApiCall::getModel));

        return groupedByModel.entrySet().stream()
                .map(entry -> {
                    String model = entry.getKey();
                    List<ApiCall> modelCalls = entry.getValue();

                    Map<String, Object> modelStats = new HashMap<>();
                    modelStats.put("model", model);
                    modelStats.put("calls", modelCalls.size());
                    modelStats.put("totalCost", modelCalls.stream()
                            .map(ApiCall::getCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    modelStats.put("totalInputTokens", modelCalls.stream()
                            .mapToInt(ApiCall::getInputTokens)
                            .sum());
                    modelStats.put("totalOutputTokens", modelCalls.stream()
                            .mapToInt(ApiCall::getOutputTokens)
                            .sum());

                    return modelStats;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取费用统计（按时间范围）
     */
    public Map<String, Object> getCostStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId);

        if (startTime != null) {
            wrapper.ge(ApiCall::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(ApiCall::getCreatedAt, endTime);
        }

        List<ApiCall> calls = apiCallMapper.selectList(wrapper);

        BigDecimal totalCost = calls.stream()
                .map(ApiCall::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCost", totalCost);
        stats.put("totalCalls", calls.size());
        stats.put("averageCost", calls.isEmpty() ? BigDecimal.ZERO
                : totalCost.divide(new BigDecimal(calls.size()), 6, BigDecimal.ROUND_HALF_UP));

        return stats;
    }
}
