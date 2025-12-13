package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单实体
 */
@Data
@TableName("recharge_orders")
public class RechargeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 充值金额（元）
     */
    private BigDecimal amount;

    /**
     * 状态：0-待支付，1-已支付，2-已取消
     */
    private Integer status;

    /**
     * 支付方式：alipay, wechat
     */
    private String payMethod;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * Stripe PaymentIntent ID
     */
    private String paymentIntentId;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
