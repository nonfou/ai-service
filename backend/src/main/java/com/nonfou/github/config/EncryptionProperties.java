package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    private String key;

    /**
     * 旧版字段，仅保留兼容提示。
     */
    private String iv;
}
