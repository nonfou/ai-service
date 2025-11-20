package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 后端定价配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.pricing")
public class BackendPricingProperties {

    /**
     * 全局加价率
     */
    private BigDecimal markupRate = new BigDecimal("1.2");

    /**
     * 是否启用账户乘数
     */
    private boolean enableAccountMultiplier = false;

    /**
     * 是否启用模型乘数
     */
    private boolean enableModelMultiplier = false;
}
