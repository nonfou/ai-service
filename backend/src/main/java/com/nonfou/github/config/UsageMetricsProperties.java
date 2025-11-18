package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * 用于配置 Token 使用统计与账单事件的属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "usage.metrics")
public class UsageMetricsProperties {

    /**
     * 时区标识，默认 Asia/Shanghai
     */
    private String timezone = "Asia/Shanghai";

    /**
     * 每日统计保留天数
     */
    private int dailyTtlDays = 32;

    /**
     * 每月统计保留天数
     */
    private int monthlyTtlDays = 365;

    /**
     * 每小时统计保留天数
     */
    private int hourlyTtlDays = 7;

    /**
     * 统计窗口（分钟级）用于系统级分钟指标
     */
    private int metricsWindowMinutes = 5;

    /**
     * 最近记录保留条数
     */
    private int usageRecordLimit = 200;

    /**
     * 是否启用 Redis 聚合
     */
    private boolean enableAggregation = true;

    /**
     * 是否启用账单事件发布
     */
    private boolean enableBillingEvents = true;

    /**
     * 账单事件 Stream key
     */
    private String billingStreamKey = "billing:events";

    /**
     * 账单事件 Stream 最大长度
     */
    private long billingStreamMaxLength = 100000L;

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }
}
