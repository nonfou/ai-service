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

    /**
     * 获取综合统计（今日+总计+Token分类）
     */
    public Map<String, Object> getSummaryStatistics(Long userId) {
        // 今日统计
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<ApiCall> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(ApiCall::getUserId, userId)
                .between(ApiCall::getCreatedAt, startOfDay, endOfDay);
        List<ApiCall> todayCalls = apiCallMapper.selectList(todayWrapper);

        // 总计统计
        LambdaQueryWrapper<ApiCall> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ApiCall::getUserId, userId);
        List<ApiCall> allCalls = apiCallMapper.selectList(totalWrapper);

        Map<String, Object> result = new HashMap<>();

        // 今日数据
        Map<String, Object> today = new HashMap<>();
        today.put("calls", todayCalls.size());
        today.put("cost", todayCalls.stream().map(ApiCall::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));
        today.put("inputTokens", todayCalls.stream().mapToLong(c -> c.getInputTokens() != null ? c.getInputTokens() : 0).sum());
        today.put("outputTokens", todayCalls.stream().mapToLong(c -> c.getOutputTokens() != null ? c.getOutputTokens() : 0).sum());
        today.put("cacheReadTokens", todayCalls.stream().mapToLong(c -> c.getCacheReadTokens() != null ? c.getCacheReadTokens() : 0).sum());
        today.put("cacheWriteTokens", todayCalls.stream().mapToLong(c -> c.getCacheWriteTokens() != null ? c.getCacheWriteTokens() : 0).sum());
        result.put("today", today);

        // 总计数据
        Map<String, Object> total = new HashMap<>();
        total.put("calls", allCalls.size());
        total.put("cost", allCalls.stream().map(ApiCall::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));
        total.put("inputTokens", allCalls.stream().mapToLong(c -> c.getInputTokens() != null ? c.getInputTokens() : 0).sum());
        total.put("outputTokens", allCalls.stream().mapToLong(c -> c.getOutputTokens() != null ? c.getOutputTokens() : 0).sum());
        total.put("cacheReadTokens", allCalls.stream().mapToLong(c -> c.getCacheReadTokens() != null ? c.getCacheReadTokens() : 0).sum());
        total.put("cacheWriteTokens", allCalls.stream().mapToLong(c -> c.getCacheWriteTokens() != null ? c.getCacheWriteTokens() : 0).sum());
        result.put("total", total);

        return result;
    }

    /**
     * 获取按小时统计（24小时分布）
     */
    public List<Map<String, Object>> getHourlyStatistics(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId)
                .between(ApiCall::getCreatedAt, startOfDay, endOfDay);
        List<ApiCall> calls = apiCallMapper.selectList(wrapper);

        // 按小时分组
        Map<Integer, List<ApiCall>> groupedByHour = calls.stream()
                .collect(Collectors.groupingBy(call -> call.getCreatedAt().getHour()));

        // 生成24小时数据
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourStats = new HashMap<>();
            hourStats.put("hour", hour);

            List<ApiCall> hourCalls = groupedByHour.getOrDefault(hour, java.util.Collections.emptyList());
            hourStats.put("calls", hourCalls.size());
            hourStats.put("cost", hourCalls.stream().map(ApiCall::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(hourStats);
        }

        return result;
    }

    /**
     * 获取Token趋势统计（按天分类Token）
     */
    public List<Map<String, Object>> getTokenTrend(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<ApiCall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiCall::getUserId, userId)
                .ge(ApiCall::getCreatedAt, startDate)
                .orderByAsc(ApiCall::getCreatedAt);

        List<ApiCall> calls = apiCallMapper.selectList(wrapper);

        // 按日期分组
        Map<LocalDate, List<ApiCall>> groupedByDate = calls.stream()
                .collect(Collectors.groupingBy(call -> call.getCreatedAt().toLocalDate()));

        return groupedByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<ApiCall> dayCalls = entry.getValue();

                    Map<String, Object> dayStats = new HashMap<>();
                    dayStats.put("date", date);
                    dayStats.put("inputTokens", dayCalls.stream().mapToLong(c -> c.getInputTokens() != null ? c.getInputTokens() : 0).sum());
                    dayStats.put("outputTokens", dayCalls.stream().mapToLong(c -> c.getOutputTokens() != null ? c.getOutputTokens() : 0).sum());
                    dayStats.put("cacheReadTokens", dayCalls.stream().mapToLong(c -> c.getCacheReadTokens() != null ? c.getCacheReadTokens() : 0).sum());
                    dayStats.put("cacheWriteTokens", dayCalls.stream().mapToLong(c -> c.getCacheWriteTokens() != null ? c.getCacheWriteTokens() : 0).sum());

                    return dayStats;
                })
                .collect(Collectors.toList());
    }
}
