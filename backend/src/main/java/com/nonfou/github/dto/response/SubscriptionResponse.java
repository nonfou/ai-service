package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订阅记录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    /**
     * 订阅ID
     */
    private Long id;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 获得额度
     */
    private BigDecimal quotaAmount;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 订阅状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
