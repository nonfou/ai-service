package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 后端配额配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.quota")
public class BackendQuotaProperties {

    /**
     * 默认每日限额
     */
    private BigDecimal defaultDailyLimit = new BigDecimal("100.00");

    /**
     * 默认每月限额
     */
    private BigDecimal defaultMonthlyLimit = new BigDecimal("3000.00");

    /**
     * 是否强制执行配额
     */
    private boolean enforce = true;

    /**
     * 告警阈值(百分比)
     */
    private BigDecimal alertThreshold = new BigDecimal("0.8");
}
