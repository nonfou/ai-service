package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.verify-code")
public class AuthVerifyCodeProperties {

    /**
     * 验证码有效期(秒)
     */
    private long ttlSeconds = 300;

    /**
     * 最小发送间隔(秒)
     */
    private long minIntervalSeconds = 60;

    /**
     * 每小时最大发送次数
     */
    private int maxPerHour = 10;

    /**
     * 哈希密钥
     */
    private String hashSecret;
}
