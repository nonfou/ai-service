package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Token趋势数据响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenTrendResponse {

    /**
     * 日期 (yyyy-MM-dd)
     */
    private String date;

    /**
     * 输入Token数量
     */
    private Long inputTokens;

    /**
     * 输出Token数量
     */
    private Long outputTokens;

    /**
     * 缓存读取Token数量
     */
    private Long cacheReadTokens;

    /**
     * 缓存写入Token数量
     */
    private Long cacheWriteTokens;

    /**
     * 核心Token数量（输入+输出）
     */
    private Long totalTokens;

    /**
     * 包含缓存的Token数量
     */
    private Long allTokens;

    /**
     * 费用
     */
    private BigDecimal cost;

    /**
     * 调用次数
     */
    private Long calls;
}
