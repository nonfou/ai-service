package com.nonfou.github.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 商户私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String publicKey;

    /**
     * 支付宝网关地址
     */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 同步跳转地址
     */
    private String returnUrl;

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 字符编码
     */
    private String charset = "UTF-8";

    /**
     * 返回数据格式
     */
    private String format = "json";

    /**
     * 创建支付宝客户端
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                format,
                charset,
                publicKey,
                signType
        );
    }
}
