package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台安全配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin.security")
public class AdminSecurityProperties {

    private boolean enableIpWhitelist = false;

    private List<String> ipWhitelist = new ArrayList<>();

    private boolean enableRateLimit = true;

    private int rateLimit = 60;

    private boolean enableLoginLock = true;

    private int maxLoginAttempts = 5;

    private int lockDuration = 30;
}
