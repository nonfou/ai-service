package com.nonfou.github.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat")
@ConditionalOnProperty(prefix = "wechat", name = "enabled", havingValue = "true", matchIfMissing = false)
public class WechatPayConfig {

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户API私钥路径
     */
    private String privateKeyPath;

    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;

    /**
     * 商户APIV3密钥
     */
    private String apiV3Key;

    /**
     * 应用ID (APPID)
     */
    private String appId;

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 创建微信支付配置
     */
    @Bean
    public Config wechatPaySDKConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }
}
