package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.dto.request.ticket.TicketCreateRequest;
import com.nonfou.github.dto.request.ticket.TicketReplyRequest;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.entity.TicketMessage;
import com.nonfou.github.enums.TicketStatus;
import com.nonfou.github.mapper.TicketMapper;
import com.nonfou.github.mapper.TicketMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper messageMapper;
    private final TicketNotificationService ticketNotificationService;

    /**
     * 创建工单
     */
    @Transactional
    public Ticket createTicket(Long userId, TicketCreateRequest request) {
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setSubject(request.getSubject());
        ticket.setContent(request.getContent());
        ticket.setStatus(TicketStatus.PENDING.getValue());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : "normal");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticketMapper.insert(ticket);

        // 创建初始消息
        TicketMessage message = new TicketMessage();
        message.setTicketId(ticket.getId());
        message.setUserId(userId);
        message.setMessage(request.getContent());
        message.setIsStaff(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        log.info("工单创建成功: userId={}, ticketId={}, subject={}", userId, ticket.getId(), request.getSubject());
        ticketNotificationService.notifyAdmin(ticket, request.getContent(), "新工单提交");

        normalizeTicketStatus(ticket);
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

        Page<Ticket> result = ticketMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::normalizeTicketStatus);
        return result;
    }

    /**
     * 获取工单详情
     */
    public Ticket getTicketById(Long userId, Long ticketId) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getId, ticketId)
                .eq(Ticket::getUserId, userId);

        Ticket ticket = ticketMapper.selectOne(wrapper);
        normalizeTicketStatus(ticket);
        return ticket;
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
    public TicketMessage replyTicket(Long userId, Long ticketId, TicketReplyRequest request) {
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
        ticketMessage.setMessage(request.getMessage());
        ticketMessage.setIsStaff(0);
        ticketMessage.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(ticketMessage);

        // 更新工单状态
        ticket.setStatus(TicketStatus.PENDING.getValue());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        normalizeTicketStatus(ticket);

        log.info("工单回复成功: userId={}, ticketId={}", userId, ticketId);

        ticketNotificationService.notifyAdmin(ticket, request.getMessage(), "用户回复");

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
        if (!TicketStatus.CLOSED.getValue().equals(ticket.getStatus())) {
            ticket.setStatus(TicketStatus.PROCESSING.getValue());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        normalizeTicketStatus(ticket);

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

        ticket.setStatus(TicketStatus.CLOSED.getValue());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        log.info("工单已关闭: userId={}, ticketId={}", userId, ticketId);
    }

    private void normalizeTicketStatus(Ticket ticket) {
        if (ticket == null) {
            return;
        }
        ticket.setStatus(TicketStatus.normalize(ticket.getStatus()));
    }
}
