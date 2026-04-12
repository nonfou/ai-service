package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置缓存参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "system-config")
public class SystemConfigProperties {

    private long cacheTtlSeconds = 60;
}
