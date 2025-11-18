package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 模型使用统计响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatsResponse {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 模型显示名称
     */
    private String displayName;

    /**
     * 总调用次数
     */
    private Long calls;

    /**
     * 成功调用次数
     */
    private Long successCalls;

    /**
     * 失败调用次数
     */
    private Long failedCalls;

    /**
     * 成功率（百分比）
     */
    private BigDecimal successRate;

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
     * 总Token数量（输入+输出）
     */
    private Long totalTokens;

    /**
     * 包含缓存后的总Token数量
     */
    private Long totalAllTokens;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 平均耗时 (毫秒)
     */
    private Long avgDuration;

    /**
     * 最后使用时间
     */
    private String lastUsedAt;
}
