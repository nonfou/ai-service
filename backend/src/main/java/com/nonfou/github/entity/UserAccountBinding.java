package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账户绑定实体
 * 管理用户/API Key 与后端账户的绑定关系
 */
@Data
@TableName("user_account_bindings")
public class UserAccountBinding {

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
     * API密钥ID（可选，为NULL时表示用户级绑定）
     */
    private Long apiKeyId;

    /**
     * 后端账户ID
     */
    private Long backendAccountId;

    /**
     * 是否为默认账户
     */
    private Boolean isDefault;

    /**
     * 绑定类型：user（用户级）, api_key（API Key级）
     */
    private String bindingType;

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
     * 判断是否为用户级绑定
     */
    public boolean isUserBinding() {
        return apiKeyId == null || "user".equals(bindingType);
    }

    /**
     * 判断是否为API Key级绑定
     */
    public boolean isApiKeyBinding() {
        return apiKeyId != null && "api_key".equals(bindingType);
    }
}
