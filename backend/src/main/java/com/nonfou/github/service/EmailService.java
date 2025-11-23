package com.nonfou.github.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 邮件服务
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成6位验证码
     */
    public String generateCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送验证码邮件（异步）
     */
    @Async
    public void sendVerifyCode(String to, String code) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("code", code);
            variables.put("expiryMinutes", "5");

            sendHtmlEmail(
                to,
                "【xCoder】邮箱验证码",
                "email/verify-code",
                variables,
                "xCoder <" + from + ">"
            );

            log.info("验证码邮件发送成功: {}", to);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: {}", to, e);
            throw new RuntimeException("邮件发送失败");
        }
    }

    /**
     * 发送通知邮件（使用HTML模板）
     *
     * @param to 收件人
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Async
    public void sendNotification(String to, String subject, String content) {
        sendNotification(to, subject, content, null, null, null);
    }

    /**
     * 发送通知邮件（完整版本，支持更多参数）
     *
     * @param to 收件人
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param ticketId 工单ID（可选）
     * @param status 状态（可选）
     * @param actionUrl 操作链接（可选）
     */
    @Async
    public void sendNotification(String to, String subject, String content,
                                 String ticketId, String status, String actionUrl) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("subject", subject);
            variables.put("content", content);

            if (ticketId != null) {
                variables.put("ticketId", ticketId);
            }
            if (status != null) {
                variables.put("status", status);
            }
            if (actionUrl != null) {
                variables.put("actionUrl", actionUrl);
                variables.put("actionText", "查看详情");
            }

            sendHtmlEmail(
                to,
                subject,
                "email/ticket-notification",
                variables,
                "xCoder <" + from + ">"
            );

            log.info("通知邮件发送成功: {}", to);
        } catch (Exception e) {
            log.error("通知邮件发送失败: {}", to, e);
        }
    }

    /**
     * 发送HTML邮件的通用方法
     *
     * @param to 收件人
     * @param subject 邮件主题
     * @param contentTemplate 内容模板名称（不含.html后缀）
     * @param variables 模板变量
     * @param fromAddress 发件人地址
     */
    private void sendHtmlEmail(String to, String subject, String contentTemplate,
                              Map<String, Object> variables, String fromAddress) {
        try {
            // 创建 MimeMessage
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 设置邮件基本信息
            helper.setFrom(fromAddress != null ? fromAddress : from);
            helper.setTo(to);
            helper.setSubject(subject);

            // 准备模板上下文
            Context context = new Context();
            context.setVariable("title", subject);
            context.setVariable("contentTemplate", contentTemplate);

            // 添加内容变量
            if (variables != null) {
                variables.forEach(context::setVariable);
            }

            // 渲染HTML内容
            String htmlContent = templateEngine.process("email/base-email", context);
            helper.setText(htmlContent, true);

            // 发送邮件
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: {}", to, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}
