package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 后端账户实体
 * 存储 GitHub Copilot 的账户配置信息
 */
@Data
@TableName(value = "backend_accounts", autoResultMap = true)
public class BackendAccount {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 提供商类型：copilot
     */
    private String provider;

    /**
     * 访问令牌（AES-256加密后）
     */
    private String accessToken;

    /**
     * 优先级（1-100，数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 账户状态：active, disabled, error
     */
    private String status;

    /**
     * 最大并发请求数
     */
    private Integer maxConcurrent;

    /**
     * 每分钟请求限制
     */
    private Integer rateLimitPerMinute;

    /**
     * 成本倍率
     */
    private BigDecimal costMultiplier;

    /**
     * 连续错误次数
     */
    private Integer errorCount;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 最后错误时间
     */
    private LocalDateTime lastErrorAt;

    /**
     * 最后错误信息
     */
    private String lastErrorMessage;

    /**
     * 扩展信息（JSON格式）
     * 示例：{"region": "us", "supported_models": ["gpt-4"]}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

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
     * 判断账户是否健康
     */
    public boolean isHealthy() {
        return "active".equals(status) && errorCount < 3;
    }

    /**
     * 判断账户是否可用
     */
    public boolean isAvailable() {
        return "active".equals(status);
    }
}
