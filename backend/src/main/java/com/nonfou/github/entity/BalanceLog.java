package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额变动日志实体
 */
@Data
@TableName("balance_log")
public class BalanceLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 变动金额（正数为增加，负数为减少）
     */
    private BigDecimal amount;

    /**
     * 变动后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 类型：recharge-充值，consume-消费
     */
    private String type;

    /**
     * 关联ID（充值订单ID或API调用ID）
     */
    private Long relatedId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
