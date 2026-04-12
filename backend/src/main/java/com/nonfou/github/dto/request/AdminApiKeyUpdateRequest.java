package com.nonfou.github.dto.request;

import lombok.Data;

/**
 * 管理端更新 API Key 请求
 */
@Data
public class AdminApiKeyUpdateRequest {

    /**
     * 密钥名称
     */
    private String keyName;

    /**
     * Copilot Relay 地址
     */
    private String relayBaseUrl;

    /**
     * 上游 API Key
     */
    private String upstreamApiKey;

    /**
     * 备注
     */
    private String description;
}
