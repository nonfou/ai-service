package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.dto.response.ModelStatsResponse;
import com.nonfou.github.dto.response.TokenTrendResponse;
import com.nonfou.github.entity.ApiCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * API调用日志 Mapper
 */
@Mapper
public interface ApiCallMapper extends BaseMapper<ApiCall> {

    /**
     * 获取用户Token统计（总计）
     */
    @Select("SELECT " +
            "COALESCE(SUM(input_tokens), 0) AS totalInputTokens, " +
            "COALESCE(SUM(output_tokens), 0) AS totalOutputTokens, " +
            "COALESCE(SUM(cache_read_tokens), 0) AS totalCacheReadTokens, " +
            "COALESCE(SUM(cache_write_tokens), 0) AS totalCacheWriteTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) AS totalTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens + cache_read_tokens + cache_write_tokens), 0) AS totalAllTokens, " +
            "COALESCE(SUM(cost), 0) AS totalCost, " +
            "COUNT(*) AS totalCalls " +
            "FROM api_calls " +
            "WHERE user_id = #{userId}")
    Map<String, Object> getUserTotalStats(@Param("userId") Long userId);

    /**
     * 获取用户今日Token统计
     */
    @Select("SELECT " +
            "COALESCE(SUM(input_tokens), 0) AS todayInputTokens, " +
            "COALESCE(SUM(output_tokens), 0) AS todayOutputTokens, " +
            "COALESCE(SUM(cache_read_tokens), 0) AS todayCacheReadTokens, " +
            "COALESCE(SUM(cache_write_tokens), 0) AS todayCacheWriteTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) AS todayTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens + cache_read_tokens + cache_write_tokens), 0) AS todayAllTokens, " +
            "COALESCE(SUM(cost), 0) AS todayCost, " +
            "COUNT(*) AS todayCalls " +
            "FROM api_calls " +
            "WHERE user_id = #{userId} AND DATE(created_at) = CURDATE()")
    Map<String, Object> getUserTodayStats(@Param("userId") Long userId);

    /**
     * 获取用户Token趋势数据
     */
    @Select("SELECT " +
            "DATE(created_at) AS date, " +
            "COALESCE(SUM(input_tokens), 0) AS inputTokens, " +
            "COALESCE(SUM(output_tokens), 0) AS outputTokens, " +
            "COALESCE(SUM(cache_read_tokens), 0) AS cacheReadTokens, " +
            "COALESCE(SUM(cache_write_tokens), 0) AS cacheWriteTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) AS totalTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens + cache_read_tokens + cache_write_tokens), 0) AS allTokens, " +
            "COALESCE(SUM(cost), 0) AS cost, " +
            "COUNT(*) AS calls " +
            "FROM api_calls " +
            "WHERE user_id = #{userId} " +
            "AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC")
    List<TokenTrendResponse> getUserTokenTrend(@Param("userId") Long userId, @Param("days") Integer days);

    /**
     * 获取用户模型使用统计
     */
    @Select("SELECT " +
            "model, " +
            "COUNT(*) AS calls, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS successCalls, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS failedCalls, " +
            "COALESCE(SUM(input_tokens), 0) AS totalInputTokens, " +
            "COALESCE(SUM(output_tokens), 0) AS totalOutputTokens, " +
            "COALESCE(SUM(cache_read_tokens), 0) AS totalCacheReadTokens, " +
            "COALESCE(SUM(cache_write_tokens), 0) AS totalCacheWriteTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) AS totalTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens + cache_read_tokens + cache_write_tokens), 0) AS totalAllTokens, " +
            "COALESCE(SUM(cost), 0) AS totalCost, " +
            "COALESCE(AVG(duration), 0) AS avgDuration, " +
            "MAX(created_at) AS lastUsedAt " +
            "FROM api_calls " +
            "WHERE user_id = #{userId} " +
            "GROUP BY model " +
            "ORDER BY calls DESC")
    List<Map<String, Object>> getUserModelStats(@Param("userId") Long userId);
}
