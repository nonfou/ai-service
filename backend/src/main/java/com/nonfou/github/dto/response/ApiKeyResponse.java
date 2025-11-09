package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API密钥响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    /**
     * 密钥ID
     */
    private Long id;

    /**
     * 密钥名称
     */
    private String keyName;

    /**
     * API密钥（创建时返回完整密钥,列表时返回脱敏）
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
    private LocalDateTime createdAt;
}
