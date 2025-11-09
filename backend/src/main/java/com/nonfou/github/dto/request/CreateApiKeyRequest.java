package com.nonfou.github.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建API密钥请求
 */
@Data
public class CreateApiKeyRequest {

    /**
     * 密钥名称
     */
    @NotBlank(message = "密钥名称不能为空")
    private String keyName;
}
