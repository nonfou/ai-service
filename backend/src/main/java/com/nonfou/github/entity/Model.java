package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型配置实体
 */
@Data
@TableName(value = "models", autoResultMap = true)
public class Model {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模型名称(如 gpt-4o)
     */
    private String modelName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 提供商:copilot, claude, openai
     */
    private String provider;

    /**
     * 价格倍率(1.0表示正常价格)
     */
    private BigDecimal priceMultiplier;

    /**
     * 输入 token 价格（每百万 token 的美元价格）
     */
    private BigDecimal inputTokenPrice;

    /**
     * 输出 token 价格（每百万 token 的美元价格）
     */
    private BigDecimal outputTokenPrice;

    /**
     * 缓存读取 token 价格（每百万 token 的美元价格，可选）
     */
    private BigDecimal cacheReadTokenPrice;

    /**
     * 缓存写入 token 价格（每百万 token 的美元价格，可选）
     */
    private BigDecimal cacheWriteTokenPrice;

    /**
     * 状态:1-启用,0-禁用
     */
    private Integer status;

    /**
     * 模型说明
     */
    private String description;

    /**
     * 模型标签列表(如:["推荐", "低价", "新品"])
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 排序值,数值越小越靠前
     */
    private Integer sortOrder;

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
