package com.nonfou.github.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Stripe 支付配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "stripe")
public class StripeConfig {

    /**
     * Stripe Secret Key (sk_test_xxx 或 sk_live_xxx)
     */
    private String apiKey;

    /**
     * Stripe Publishable Key (pk_test_xxx 或 pk_live_xxx)
     */
    private String publishableKey;

    /**
     * Webhook 签名密钥 (whsec_xxx)
     */
    private String webhookSecret;

    /**
     * 默认货币
     */
    private String currency = "usd";

    /**
     * 最小充值金额
     */
    private BigDecimal minAmount = BigDecimal.ONE;

    /**
     * 最大充值金额
     */
    private BigDecimal maxAmount = new BigDecimal("1000");

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            Stripe.apiKey = this.apiKey;
        }
    }

    /**
     * 检查 Stripe 是否已配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && publishableKey != null && !publishableKey.isBlank();
    }
}
