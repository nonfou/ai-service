package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 后端路由配置，承载多提供方路由相关的属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.routing")
public class BackendRoutingProperties {

    /**
     * 默认提供方标识，默认为 copilot。
     */
    private String defaultProvider = "copilot";
}
