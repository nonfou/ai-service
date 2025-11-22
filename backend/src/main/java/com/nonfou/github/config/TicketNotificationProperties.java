package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工单通知相关配置.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ticket.notify")
public class TicketNotificationProperties {

    /**
     * 是否启用邮件通知.
     */
    private boolean enabled = true;

    /**
     * 管理员通知邮箱地址.
     */
    private String adminEmail;
}
