package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订阅记录实体
 */
@Data
@TableName("subscriptions")
public class Subscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 套餐名称（冗余）
     */
    private String planName;

    /**
     * 支付金额（元）
     */
    private BigDecimal amount;

    /**
     * 获得额度（元）
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
     * 状态：active-生效中，expired-已过期，cancelled-已取消
     */
    private String status;

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
