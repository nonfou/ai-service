package com.nonfou.github.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 邮件服务
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 生成6位验证码
     */
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送验证码邮件（异步）
     */
    @Async
    public void sendVerifyCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("xCoder <" + from + ">");
            message.setTo(to);
            message.setSubject("【xCoder】邮箱验证码");
            message.setText(buildVerifyCodeText(code));

            mailSender.send(message);
            log.info("验证码邮件发送成功: {}", to);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: {}", to, e);
            throw new RuntimeException("邮件发送失败");
        }
    }

    /**
     * 构建验证码邮件内容
     */
    private String buildVerifyCodeText(String code) {
        return String.format(
            "【xCoder 邮箱验证】\n\n" +
            "您的验证码是：%s\n\n" +
            "验证码有效期为 5 分钟，请勿泄露给他人。\n\n" +
            "如果这不是您的操作，请忽略此邮件。\n\n" +
            "---\n" +
            "xCoder - AI 编程助手平台\n" +
            "https://xcoder.plus",
            code
        );
    }

    /**
     * 发送通知邮件
     */
    @Async
    public void sendNotification(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("通知邮件发送成功: {}", to);
        } catch (Exception e) {
            log.error("通知邮件发送失败: {}", to, e);
        }
    }
}
