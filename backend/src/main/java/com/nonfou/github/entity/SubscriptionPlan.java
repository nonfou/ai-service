package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅套餐实体
 */
@Data
@TableName(value = "subscription_plans", autoResultMap = true)
public class SubscriptionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 套餐标识名称
     */
    private String planName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 套餐说明
     */
    private String description;

    /**
     * 原价（元）
     */
    private BigDecimal originalPrice;

    /**
     * 现价（元）
     */
    private BigDecimal price;

    /**
     * 额度金额（元）
     */
    private BigDecimal quotaAmount;

    /**
     * 功能特性列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> features;

    /**
     * 颜色主题: green-绿色, blue-蓝紫色, pink-粉红色
     */
    private String colorTheme;

    /**
     * 徽章显示文字,如: 推荐套餐, 新用户专享
     */
    private String badgeText;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时���
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
