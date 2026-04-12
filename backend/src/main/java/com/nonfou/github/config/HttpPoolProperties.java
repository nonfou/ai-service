package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HTTP 连接池配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "http.pool")
public class HttpPoolProperties {

    private int maxTotal = 200;

    private int maxPerRoute = 50;

    private int validateAfterInactivityMs = 2000;
}
