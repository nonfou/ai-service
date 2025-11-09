package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 账户余额（元）
     */
    private BigDecimal balance;

    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;

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
