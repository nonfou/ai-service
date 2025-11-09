package com.nonfou.github.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户Token统计响应
 */
@Data
public class UserTokenStatsResponse {

    /**
     * 总输入Token数
     */
    private Long totalInputTokens;

    /**
     * 总输出Token数
     */
    private Long totalOutputTokens;

    /**
     * 总Token数
     */
    private Long totalTokens;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 今日输入Token数
     */
    private Long todayInputTokens;

    /**
     * 今日输出Token数
     */
    private Long todayOutputTokens;

    /**
     * 今日Token数
     */
    private Long todayTokens;

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
        this.totalTokens = 0L;
        this.totalCost = BigDecimal.ZERO;
        this.totalCalls = 0L;
        this.todayInputTokens = 0L;
        this.todayOutputTokens = 0L;
        this.todayTokens = 0L;
        this.todayCost = BigDecimal.ZERO;
        this.todayCalls = 0L;
    }
}
