package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话粘性映射实体
 * 实现同一会话使用同一后端账户，保证上下文一致性
 */
@Data
@TableName("session_mappings")
public class SessionMapping {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话哈希（SHA-256）
     */
    private String sessionHash;

    /**
     * 绑定的后端账户ID
     */
    private Long backendAccountId;

    /**
     * API密钥ID
     */
    private Long apiKeyId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 该会话请求次数
     */
    private Integer requestCount;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 判断会话是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 增加请求计数
     */
    public void incrementRequestCount() {
        this.requestCount = (this.requestCount == null ? 0 : this.requestCount) + 1;
    }
}
