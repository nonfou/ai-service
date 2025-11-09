package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copilot API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "copilot")
public class CopilotConfig {

    /**
     * 端点列表
     */
    private List<Endpoint> endpoints = new ArrayList<>();

    /**
     * 故障转移配置
     */
    private Failover failover = new Failover();

    /**
     * 获取所有启用的端点（按优先级��序）
     */
    public List<Endpoint> getEnabledEndpoints() {
        return endpoints.stream()
                .filter(Endpoint::isEnabled)
                .filter(e -> e.getUrl() != null && !e.getUrl().isEmpty())
                .filter(e -> e.getToken() != null && !e.getToken().isEmpty())
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * 端点配置
     */
    @Data
    public static class Endpoint {
        /**
         * 端点名称
         */
        private String name;

        /**
         * API URL
         */
        private String url;

        /**
         * GitHub Token
         */
        private String token;

        /**
         * 优先级（数字越小优先级越高）
         */
        private int priority = 999;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 当前状态（健康/不健康）
         */
        private transient EndpointStatus status = EndpointStatus.UNKNOWN;

        /**
         * 最后健康检查时间
         */
        private transient long lastHealthCheckTime = 0;

        /**
         * 连续失败次数
         */
        private transient int consecutiveFailures = 0;
    }

    /**
     * 故障转移配置
     */
    @Data
    public static class Failover {
        /**
         * 是否启用故障转移
         */
        private boolean enabled = true;

        /**
         * 单个端点最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 请求超时时间（毫秒）
         */
        private int timeout = 5000;

        /**
         * 健康检查间隔（秒）
         */
        private int healthCheckInterval = 30;
    }

    /**
     * 端点状态枚举
     */
    public enum EndpointStatus {
        UNKNOWN,    // 未知状态
        HEALTHY,    // 健康
        UNHEALTHY   // 不健康
    }
}
