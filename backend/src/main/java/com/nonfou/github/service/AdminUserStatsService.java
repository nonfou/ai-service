package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.dto.response.ModelStatsResponse;
import com.nonfou.github.dto.response.TokenTrendResponse;
import com.nonfou.github.dto.response.UserTokenStatsResponse;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.entity.BalanceLog;
import com.nonfou.github.entity.Model;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.mapper.ApiCallMapper;
import com.nonfou.github.mapper.BalanceLogMapper;
import com.nonfou.github.mapper.ModelMapper;
import com.nonfou.github.mapper.RechargeOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员用户统计服务
 */
@Slf4j
@Service
public class AdminUserStatsService {

    @Autowired
    private ApiCallMapper apiCallMapper;

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private ModelMapper modelMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取用户Token统计
     */
    public UserTokenStatsResponse getUserTokenStats(Long userId) {
        UserTokenStatsResponse response = new UserTokenStatsResponse();

        // 获取总统计
        Map<String, Object> totalStats = apiCallMapper.getUserTotalStats(userId);
        if (totalStats != null) {
            response.setTotalInputTokens(getLongValue(totalStats.get("totalInputTokens")));
            response.setTotalOutputTokens(getLongValue(totalStats.get("totalOutputTokens")));
            response.setTotalCacheReadTokens(getLongValue(totalStats.get("totalCacheReadTokens")));
            response.setTotalCacheWriteTokens(getLongValue(totalStats.get("totalCacheWriteTokens")));
            response.setTotalTokens(getLongValue(totalStats.get("totalTokens")));
            response.setTotalAllTokens(getLongValue(totalStats.get("totalAllTokens")));
            response.setTotalCost(getBigDecimalValue(totalStats.get("totalCost")));
            response.setTotalCalls(getLongValue(totalStats.get("totalCalls")));
        }

        // 获取今日统计
        Map<String, Object> todayStats = apiCallMapper.getUserTodayStats(userId);
        if (todayStats != null) {
            response.setTodayInputTokens(getLongValue(todayStats.get("todayInputTokens")));
            response.setTodayOutputTokens(getLongValue(todayStats.get("todayOutputTokens")));
            response.setTodayCacheReadTokens(getLongValue(todayStats.get("todayCacheReadTokens")));
            response.setTodayCacheWriteTokens(getLongValue(todayStats.get("todayCacheWriteTokens")));
            response.setTodayTokens(getLongValue(todayStats.get("todayTokens")));
            response.setTodayAllTokens(getLongValue(todayStats.get("todayAllTokens")));
            response.setTodayCost(getBigDecimalValue(todayStats.get("todayCost")));
            response.setTodayCalls(getLongValue(todayStats.get("todayCalls")));
        }

        return response;
    }

    /**
     * 获取用户Token趋势数据
     */
    public List<TokenTrendResponse> getUserTokenTrend(Long userId, Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        if (days > 90) {
            days = 90; // 最多90天
        }
        return apiCallMapper.getUserTokenTrend(userId, days);
    }

    /**
     * 获取用户模型使用统计
     */
    public List<ModelStatsResponse> getUserModelStats(Long userId) {
        List<Map<String, Object>> rawStats = apiCallMapper.getUserModelStats(userId);

        // 获取所有模型信息用于填充displayName
        Map<String, String> modelDisplayNames = new HashMap<>();
        try {
            List<Model> models = modelMapper.selectList(null);
            for (Model model : models) {
                modelDisplayNames.put(model.getModelName(), model.getDisplayName());
            }
        } catch (Exception e) {
            log.warn("获取模型列表失败", e);
        }

        return rawStats.stream().map(stat -> {
            ModelStatsResponse response = new ModelStatsResponse();
            String modelName = (String) stat.get("model");
            response.setModel(modelName);
            response.setDisplayName(modelDisplayNames.getOrDefault(modelName, modelName));

            Long calls = getLongValue(stat.get("calls"));
            Long successCalls = getLongValue(stat.get("successCalls"));
            Long failedCalls = getLongValue(stat.get("failedCalls"));

            response.setCalls(calls);
            response.setSuccessCalls(successCalls);
            response.setFailedCalls(failedCalls);

            // 计算成功率
            if (calls > 0) {
                BigDecimal successRate = BigDecimal.valueOf(successCalls)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(calls), 2, RoundingMode.HALF_UP);
                response.setSuccessRate(successRate);
            } else {
                response.setSuccessRate(BigDecimal.ZERO);
            }

            response.setTotalInputTokens(getLongValue(stat.get("totalInputTokens")));
            response.setTotalOutputTokens(getLongValue(stat.get("totalOutputTokens")));
            response.setTotalCacheReadTokens(getLongValue(stat.get("totalCacheReadTokens")));
            response.setTotalCacheWriteTokens(getLongValue(stat.get("totalCacheWriteTokens")));
            response.setTotalTokens(getLongValue(stat.get("totalTokens")));
            response.setTotalAllTokens(getLongValue(stat.get("totalAllTokens")));
            response.setTotalCost(getBigDecimalValue(stat.get("totalCost")));
            response.setAvgDuration(getLongValue(stat.get("avgDuration")));

            Object lastUsedAt = stat.get("lastUsedAt");
            if (lastUsedAt != null) {
                response.setLastUsedAt(lastUsedAt.toString());
            }

            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户订单列表
     */
    public Page<RechargeOrder> getUserOrders(Long userId, int pageNum, int pageSize) {
        QueryWrapper<RechargeOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");

        Page<RechargeOrder> page = new Page<>(pageNum, pageSize);
        return rechargeOrderMapper.selectPage(page, wrapper);
    }

    /**
     * 获取用户API调用日志
     */
    public Page<ApiCall> getUserApiCalls(Long userId, int pageNum, int pageSize) {
        QueryWrapper<ApiCall> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");

        Page<ApiCall> page = new Page<>(pageNum, pageSize);
        return apiCallMapper.selectPage(page, wrapper);
    }

    /**
     * 获取用户余额日志
     */
    public Page<BalanceLog> getUserBalanceLogs(Long userId, int pageNum, int pageSize) {
        QueryWrapper<BalanceLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");

        Page<BalanceLog> page = new Page<>(pageNum, pageSize);
        return balanceLogMapper.selectPage(page, wrapper);
    }

    /**
     * 辅助方法：安全地获取Long值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 辅助方法:安全地获取BigDecimal值
     */
    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        }
        if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value);
        }
        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value);
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
