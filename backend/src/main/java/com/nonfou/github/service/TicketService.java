package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.config.TicketNotificationProperties;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.entity.TicketMessage;
import com.nonfou.github.entity.User;
import com.nonfou.github.mapper.TicketMapper;
import com.nonfou.github.mapper.TicketMessageMapper;
import com.nonfou.github.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单服务
 */
@Slf4j
@Service
public class TicketService {

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketMessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TicketNotificationProperties ticketNotificationProperties;

    /**
     * 创建工单
     */
    @Transactional
    public Ticket createTicket(Long userId, String subject, String content, String priority) {
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setSubject(subject);
        ticket.setContent(content);
        ticket.setStatus("open");
        ticket.setPriority(priority != null ? priority : "normal");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticketMapper.insert(ticket);

        // 创建初始消息
        TicketMessage message = new TicketMessage();
        message.setTicketId(ticket.getId());
        message.setUserId(userId);
        message.setMessage(content);
        message.setIsStaff(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        log.info("工单创建成功: userId={}, ticketId={}, subject={}", userId, ticket.getId(), subject);

        notifyAdminForTicket(ticket, content, "新工单提交");

        return ticket;
    }

    /**
     * 获取用户工单列表
     */
    public Page<Ticket> getUserTickets(Long userId, int pageNum, int pageSize) {
        Page<Ticket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getUserId, userId)
                .orderByDesc(Ticket::getCreatedAt);

        return ticketMapper.selectPage(page, wrapper);
    }

    /**
     * 获取工单详情
     */
    public Ticket getTicketById(Long userId, Long ticketId) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getId, ticketId)
                .eq(Ticket::getUserId, userId);

        return ticketMapper.selectOne(wrapper);
    }

    /**
     * 获取工单消息列表
     */
    public List<TicketMessage> getTicketMessages(Long ticketId) {
        LambdaQueryWrapper<TicketMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketMessage::getTicketId, ticketId)
                .orderByAsc(TicketMessage::getCreatedAt);

        return messageMapper.selectList(wrapper);
    }

    /**
     * 用户回复工单
     */
    @Transactional
    public TicketMessage replyTicket(Long userId, Long ticketId, String message) {
        // 验证工单归属
        Ticket ticket = getTicketById(userId, ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        if ("closed".equals(ticket.getStatus())) {
            throw new RuntimeException("工单已关闭，无法回复");
        }

        // 创建消息
        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicketId(ticketId);
        ticketMessage.setUserId(userId);
        ticketMessage.setMessage(message);
        ticketMessage.setIsStaff(0);
        ticketMessage.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(ticketMessage);

        // 更新工单状态
        if ("closed".equals(ticket.getStatus())) {
            ticket.setStatus("open");
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        log.info("工单回复成功: userId={}, ticketId={}", userId, ticketId);

        notifyAdminForTicket(ticket, message, "用户回复");

        return ticketMessage;
    }

    /**
     * 管理员回复工单
     */
    @Transactional
    public TicketMessage adminReply(Long adminId, Long ticketId, String message) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        // 创建管理员消息
        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicketId(ticketId);
        ticketMessage.setAdminId(adminId);
        ticketMessage.setMessage(message);
        ticketMessage.setIsStaff(1);
        ticketMessage.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(ticketMessage);

        // 更新工单状态为处理中
        if ("open".equals(ticket.getStatus())) {
            ticket.setStatus("in_progress");
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        log.info("管理员回复工单: adminId={}, ticketId={}", adminId, ticketId);

        return ticketMessage;
    }

    /**
     * 关闭工单
     */
    @Transactional
    public void closeTicket(Long userId, Long ticketId) {
        Ticket ticket = getTicketById(userId, ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        ticket.setStatus("closed");
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        log.info("工单已关闭: userId={}, ticketId={}", userId, ticketId);
    }

    /**
     * 向管理员发送工单通知.
     */
    private void notifyAdminForTicket(Ticket ticket, String messageContent, String eventLabel) {
        if (!ticketNotificationProperties.isEnabled()) {
            return;
        }
        String adminEmail = ticketNotificationProperties.getAdminEmail();
        if (!StringUtils.hasText(adminEmail)) {
            log.warn("工单邮件通知已启用但未配置管理员邮箱");
            return;
        }

        User user = userMapper.selectById(ticket.getUserId());
        String userEmail = user != null ? user.getEmail() : "未知邮箱";
        String subject = String.format("【xCoder】%s - 工单 #%d", eventLabel, ticket.getId());
        StringBuilder body = new StringBuilder()
                .append("用户: ").append(userEmail).append("\n")
                .append("工单ID: ").append(ticket.getId()).append("\n")
                .append("主题: ").append(ticket.getSubject()).append("\n")
                .append("优先级: ").append(ticket.getPriority()).append("\n\n")
                .append("消息内容:\n")
                .append(messageContent);

        emailService.sendNotification(adminEmail, subject, body.toString());
    }
}
