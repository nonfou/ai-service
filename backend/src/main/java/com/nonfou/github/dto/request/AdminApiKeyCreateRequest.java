package com.nonfou.github.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端创建 API Key 请求
 */
@Data
public class AdminApiKeyCreateRequest {

    /**
     * 密钥名称
     */
    @NotBlank(message = "密钥名称不能为空")
    private String keyName;

    /**
     * Copilot Relay 地址
     */
    @NotBlank(message = "转发地址不能为空")
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
