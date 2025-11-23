package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型别名映射配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.client.models")
public class ModelMappingProperties {

    /**
     * key -> value 的别名映射。
     */
    private Map<String, String> aliases = new HashMap<>();

    /**
     * 关闭/开启匹配（默认开启）。
     */
    private boolean enabled = true;
}
