package com.nonfou.github.service;

import com.nonfou.github.config.TicketNotificationProperties;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.entity.User;
import com.nonfou.github.enums.TicketStatus;
import com.nonfou.github.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 工单通知服务,负责向管理员和用户发送邮件通知.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketNotificationService {

    private final TicketNotificationProperties properties;
    private final EmailService emailService;
    private final UserMapper userMapper;

    /**
     * 通知管理员有新的工单或用户回复.
     */
    public void notifyAdmin(Ticket ticket, String messageBody, String eventLabel) {
        if (!properties.isEnabled()) {
            return;
        }
        String adminEmail = properties.getAdminEmail();
        if (!StringUtils.hasText(adminEmail)) {
            log.warn("已启用工单邮件通知,但未配置管理员邮箱(admin-email)");
            return;
        }

        String subject = String.format("【工单提醒】%s - #%d", eventLabel, ticket.getId());
        StringBuilder content = new StringBuilder()
                .append("工单ID: ").append(ticket.getId()).append("\n")
                .append("主题: ").append(ticket.getSubject()).append("\n")
                .append("优先级: ").append(ticket.getPriority()).append("\n")
                .append("当前状态: ").append(TicketStatus.normalize(ticket.getStatus())).append("\n\n")
                .append("最新内容:\n")
                .append(messageBody);

        emailService.sendNotification(
                adminEmail,
                subject,
                content.toString(),
                String.valueOf(ticket.getId()),
                TicketStatus.normalize(ticket.getStatus()),
                buildAdminActionUrl(ticket.getId())
        );
    }

    /**
     * 通知用户管理员已回复.
     */
    public void notifyUserForReply(Ticket ticket, String replyMessage) {
        if (!properties.isEnabled()) {
            return;
        }
        User user = userMapper.selectById(ticket.getUserId());
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            log.warn("无法发送工单回复邮件,用户不存在或未设置邮箱: ticketId={}", ticket.getId());
            return;
        }

        String subject = String.format("【工单回复】#%d 已更新", ticket.getId());
        StringBuilder content = new StringBuilder()
                .append("您的工单已收到客服回复。\n\n")
                .append("工单主题: ").append(ticket.getSubject()).append("\n")
                .append("回复内容:\n")
                .append(replyMessage)
                .append("\n\n请登录控制台查看详情并继续沟通。");

        emailService.sendNotification(
                user.getEmail(),
                subject,
                content.toString(),
                String.valueOf(ticket.getId()),
                TicketStatus.normalize(ticket.getStatus()),
                properties.getUserPortalUrl()
        );
    }

    private String buildAdminActionUrl(Long ticketId) {
        if (!StringUtils.hasText(properties.getAdminConsoleUrl())) {
            return null;
        }
        String base = properties.getAdminConsoleUrl().endsWith("/")
                ? properties.getAdminConsoleUrl().substring(0, properties.getAdminConsoleUrl().length() - 1)
                : properties.getAdminConsoleUrl();
        return base + "/tickets/" + ticketId;
    }
}
