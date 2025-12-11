package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付类型
     */
    private String paymentType;

    /**
     * Stripe PaymentIntent clientSecret (用于前端确认支付)
     */
    private String clientSecret;

    /**
     * 金额
     */
    private java.math.BigDecimal amount;
}
