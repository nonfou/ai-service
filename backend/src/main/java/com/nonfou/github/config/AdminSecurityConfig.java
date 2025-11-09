package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台安全配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin.security")
public class AdminSecurityConfig {

    /**
     * 是否启用IP白名单
     */
    private boolean enableIpWhitelist = false;

    /**
     * IP白名单列表
     */
    private List<String> ipWhitelist = new ArrayList<>();

    /**
     * 是否启用请求频率限制
     */
    private boolean enableRateLimit = true;

    /**
     * 请求频率限制(每分钟)
     */
    private int rateLimit = 60;

    /**
     * 是否启用登录失败锁定
     */
    private boolean enableLoginLock = true;

    /**
     * 登录失败次数上限
     */
    private int maxLoginAttempts = 5;

    /**
     * 登录锁定时间(分钟)
     */
    private int lockDuration = 30;
}
