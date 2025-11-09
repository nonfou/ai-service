package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API密钥实体
 */
@Data
@TableName("api_keys")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 密钥名称
     */
    private String keyName;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

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
