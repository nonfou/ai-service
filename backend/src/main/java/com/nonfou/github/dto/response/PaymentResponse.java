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
     * 支付类型: alipay | wechat
     */
    private String paymentType;

    /**
     * 支付宝支付链接 (支付宝使用)
     */
    private String paymentUrl;

    /**
     * 微信支付二维码内容 (微信使用)
     */
    private String qrCode;

    /**
     * 金额
     */
    private java.math.BigDecimal amount;
}
