package com.nonfou.github.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户Token统计响应
 */
@Data
public class UserTokenStatsResponse {

    /**
     * 总输入Token数量
     */
    private Long totalInputTokens;

    /**
     * 总输出Token数量
     */
    private Long totalOutputTokens;

    /**
     * 总缓存读取Token数量
     */
    private Long totalCacheReadTokens;

    /**
     * 总缓存写入Token数量
     */
    private Long totalCacheWriteTokens;

    /**
     * 总Token数量（仅输入+输出）
     */
    private Long totalTokens;

    /**
     * 包含缓存Token的总数
     */
    private Long totalAllTokens;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 今日输入Token数量
     */
    private Long todayInputTokens;

    /**
     * 今日输出Token数量
     */
    private Long todayOutputTokens;

    /**
     * 今日缓存读取Token数量
     */
    private Long todayCacheReadTokens;

    /**
     * 今日缓存写入Token数量
     */
    private Long todayCacheWriteTokens;

    /**
     * 今日Token数量（输入+输出）
     */
    private Long todayTokens;

    /**
     * 今日包含缓存的Token数量
     */
    private Long todayAllTokens;

    /**
     * 今日费用
     */
    private BigDecimal todayCost;

    /**
     * 今日调用次数
     */
    private Long todayCalls;

    public UserTokenStatsResponse() {
        this.totalInputTokens = 0L;
        this.totalOutputTokens = 0L;
        this.totalCacheReadTokens = 0L;
        this.totalCacheWriteTokens = 0L;
        this.totalTokens = 0L;
        this.totalAllTokens = 0L;
        this.totalCost = BigDecimal.ZERO;
        this.totalCalls = 0L;
        this.todayInputTokens = 0L;
        this.todayOutputTokens = 0L;
        this.todayCacheReadTokens = 0L;
        this.todayCacheWriteTokens = 0L;
        this.todayTokens = 0L;
        this.todayAllTokens = 0L;
        this.todayCost = BigDecimal.ZERO;
        this.todayCalls = 0L;
    }
}
