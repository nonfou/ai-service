package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 后端调度器配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.scheduler")
public class BackendSchedulerProperties {

    /**
     * 调度策略
     */
    private String strategy = "round-robin";

    /**
     * 会话粘性配置
     */
    private SessionStickiness sessionStickiness = new SessionStickiness();

    /**
     * 健康检查配置
     */
    private HealthCheck healthCheck = new HealthCheck();

    /**
     * 负载均衡配置
     */
    private LoadBalance loadBalance = new LoadBalance();

    /**
     * 会话粘性配置
     */
    @Data
    public static class SessionStickiness {
        /**
         * 是否启用会话粘性
         */
        private boolean enabled = true;

        /**
         * 会话生存时间(小时)
         */
        private int ttlHours = 1;

        /**
         * 是否在请求时续期
         */
        private boolean renewalOnRequest = true;
    }

    /**
     * 健康检查配置
     */
    @Data
    public static class HealthCheck {
        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;

        /**
         * 检查间隔(秒)
         */
        private int intervalSeconds = 30;

        /**
         * 超时时间(秒)
         */
        private int timeoutSeconds = 10;

        /**
         * 失败阈值
         */
        private int failureThreshold = 3;

        /**
         * 成功阈值
         */
        private int successThreshold = 2;
    }

    /**
     * 负载均衡配置
     */
    @Data
    public static class LoadBalance {
        /**
         * 是否启用并发限制
         */
        private boolean enableConcurrentLimit = true;

        /**
         * 是否启用速率限制
         */
        private boolean enableRateLimit = true;
    }
}
