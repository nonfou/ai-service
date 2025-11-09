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
     * 输入Token数
     */
    private Long inputTokens;

    /**
     * 输出Token数
     */
    private Long outputTokens;

    /**
     * 总Token数
     */
    private Long totalTokens;

    /**
     * 费用
     */
    private BigDecimal cost;

    /**
     * 调用次数
     */
    private Long calls;
}
