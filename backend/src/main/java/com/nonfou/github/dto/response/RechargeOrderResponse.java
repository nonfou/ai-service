package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrderResponse {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 充值金额
     */
    private BigDecimal amount;

    /**
     * 订单状态: 0-待支付，1-已支付，2-已取消
     */
    private Integer status;

    /**
     * 支付方式
     */
    private String payMethod;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
