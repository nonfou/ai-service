package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端 API Key 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApiKeyResponse {

    /**
     * 主键 ID
     */
    private String id;

    /**
     * 密钥名称
     */
    private String keyName;

    /**
     * 客户端 API Key
     */
    private String apiKey;

    /**
     * Copilot Relay 地址
     */
    private String relayBaseUrl;

    /**
     * 脱敏后的上游 API Key
     */
    private String upstreamApiKey;

    /**
     * 备注
     */
    private String description;

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
