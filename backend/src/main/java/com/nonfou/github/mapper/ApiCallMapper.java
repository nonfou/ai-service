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
            "COALESCE(SUM(input_tokens), 0) as totalInputTokens, " +
            "COALESCE(SUM(output_tokens), 0) as totalOutputTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) as totalTokens, " +
            "COALESCE(SUM(cost), 0) as totalCost, " +
            "COUNT(*) as totalCalls " +
            "FROM api_calls " +
            "WHERE user_id = #{userId}")
    Map<String, Object> getUserTotalStats(@Param("userId") Long userId);

    /**
     * 获取用户今日Token统计
     */
    @Select("SELECT " +
            "COALESCE(SUM(input_tokens), 0) as todayInputTokens, " +
            "COALESCE(SUM(output_tokens), 0) as todayOutputTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) as todayTokens, " +
            "COALESCE(SUM(cost), 0) as todayCost, " +
            "COUNT(*) as todayCalls " +
            "FROM api_calls " +
            "WHERE user_id = #{userId} AND DATE(created_at) = CURDATE()")
    Map<String, Object> getUserTodayStats(@Param("userId") Long userId);

    /**
     * 获取用户Token趋势数据
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COALESCE(SUM(input_tokens), 0) as inputTokens, " +
            "COALESCE(SUM(output_tokens), 0) as outputTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) as totalTokens, " +
            "COALESCE(SUM(cost), 0) as cost, " +
            "COUNT(*) as calls " +
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
            "COUNT(*) as calls, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as successCalls, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as failedCalls, " +
            "COALESCE(SUM(input_tokens), 0) as totalInputTokens, " +
            "COALESCE(SUM(output_tokens), 0) as totalOutputTokens, " +
            "COALESCE(SUM(input_tokens + output_tokens), 0) as totalTokens, " +
            "COALESCE(SUM(cost), 0) as totalCost, " +
            "COALESCE(AVG(duration), 0) as avgDuration, " +
            "MAX(created_at) as lastUsedAt " +
            "FROM api_calls " +
            "WHERE user_id = #{userId} " +
            "GROUP BY model " +
            "ORDER BY calls DESC")
    List<Map<String, Object>> getUserModelStats(@Param("userId") Long userId);
}
