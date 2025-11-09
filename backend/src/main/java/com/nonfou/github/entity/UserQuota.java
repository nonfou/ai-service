package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户配额实体
 * 管理用户的每日/每月消费配额
 */
@Data
@TableName("user_quotas")
public class UserQuota {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 配额类型：daily（每日）, monthly（每月）, custom（自定义）
     */
    private String quotaType;

    /**
     * 配额金额（元）
     */
    private BigDecimal quotaAmount;

    /**
     * 已使用金额（元）
     */
    private BigDecimal usedAmount;

    /**
     * 下次重置时间
     */
    private LocalDateTime resetAt;

    /**
     * 是否启用配额限制
     */
    private Boolean isEnabled;

    /**
     * 告警阈值（百分比，如80.00表示80%）
     */
    private BigDecimal alertThreshold;

    /**
     * 最后告警时间
     */
    private LocalDateTime lastAlertAt;

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

    /**
     * 计算使用百分比
     */
    public BigDecimal getUsagePercentage() {
        if (quotaAmount == null || quotaAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return usedAmount.divide(quotaAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 判断配额是否启用
     */
    public boolean isEnabled() {
        return isEnabled != null && isEnabled
                && quotaAmount != null && quotaAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否超限
     */
    public boolean isExceeded() {
        return usedAmount != null && quotaAmount != null
                && usedAmount.compareTo(quotaAmount) >= 0;
    }

    /**
     * 判断是否需要告警
     */
    public boolean shouldAlert() {
        if (!isEnabled || alertThreshold == null) {
            return false;
        }
        return getUsagePercentage().compareTo(alertThreshold) >= 0;
    }

    /**
     * 判断是否需要重置
     */
    public boolean shouldReset() {
        return resetAt != null && resetAt.isBefore(LocalDateTime.now());
    }

    /**
     * 获取剩余配额
     */
    public BigDecimal getRemainingAmount() {
        if (quotaAmount == null || usedAmount == null) {
            return BigDecimal.ZERO;
        }
        return quotaAmount.subtract(usedAmount).max(BigDecimal.ZERO);
    }
}
