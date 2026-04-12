package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 流式转发线程池配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.streaming")
public class StreamingProperties {

    private int timeoutMs = 300000;

    private int corePoolSize = 4;

    private int maxPoolSize = 16;

    private int queueCapacity = 500;

    private int keepAliveSeconds = 60;
}
