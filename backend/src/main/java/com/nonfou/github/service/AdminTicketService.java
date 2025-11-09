package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.entity.TicketMessage;
import com.nonfou.github.mapper.TicketMapper;
import com.nonfou.github.mapper.TicketMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员工单管理服务
 */
@Slf4j
@Service
public class AdminTicketService {

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketMessageMapper ticketMessageMapper;

    /**
     * 获取工单列表
     */
    public Page<Ticket> getTicketList(int pageNum, int pageSize, String status, String priority) {
        Page<Ticket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Ticket::getStatus, status);
        }

        if (priority != null && !priority.isEmpty()) {
            wrapper.eq(Ticket::getPriority, priority);
        }

        wrapper.orderByDesc(Ticket::getCreatedAt);

        return ticketMapper.selectPage(page, wrapper);
    }

    /**
     * 获取工单详情及消息列表
     */
    public Map<String, Object> getTicketDetail(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        LambdaQueryWrapper<TicketMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(TicketMessage::getTicketId, ticketId)
                .orderByAsc(TicketMessage::getCreatedAt);
        List<TicketMessage> messages = ticketMessageMapper.selectList(messageWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("ticket", ticket);
        result.put("messages", messages);

        return result;
    }

    /**
     * 管理员回复工单
     */
    @Transactional
    public TicketMessage replyTicket(Long adminId, Long ticketId, String message) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        // 创建消息
        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicketId(ticketId);
        ticketMessage.setUserId(adminId);
        ticketMessage.setIsStaff(1);
        ticketMessage.setMessage(message);
        ticketMessage.setCreatedAt(LocalDateTime.now());

        ticketMessageMapper.insert(ticketMessage);

        // 更新工单状态为处理中
        if ("pending".equals(ticket.getStatus())) {
            ticket.setStatus("processing");
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketMapper.updateById(ticket);
        }

        log.info("管理员回复工单: adminId={}, ticketId={}", adminId, ticketId);

        return ticketMessage;
    }

    /**
     * 更新工单状态
     */
    @Transactional
    public void updateTicketStatus(Long ticketId, String status) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        log.info("工单状态更新: ticketId={}, status={}", ticketId, status);
    }

    /**
     * 更新工单优先级
     */
    @Transactional
    public void updateTicketPriority(Long ticketId, String priority) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        log.info("工单优先级更新: ticketId={}, priority={}", ticketId, priority);
    }

    /**
     * 获取工单统计
     */
    public Map<String, Object> getTicketStatistics() {
        // 总工单数
        Long totalTickets = ticketMapper.selectCount(null);

        // 待处理工单数
        LambdaQueryWrapper<Ticket> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Ticket::getStatus, "pending");
        Long pendingTickets = ticketMapper.selectCount(pendingWrapper);

        // 处理中工单数
        LambdaQueryWrapper<Ticket> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.eq(Ticket::getStatus, "processing");
        Long processingTickets = ticketMapper.selectCount(processingWrapper);

        // 已关闭工单数
        LambdaQueryWrapper<Ticket> closedWrapper = new LambdaQueryWrapper<>();
        closedWrapper.eq(Ticket::getStatus, "closed");
        Long closedTickets = ticketMapper.selectCount(closedWrapper);

        // 今日新增工单
        LambdaQueryWrapper<Ticket> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(Ticket::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay());
        Long todayTickets = ticketMapper.selectCount(todayWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTickets", totalTickets);
        stats.put("pendingTickets", pendingTickets);
        stats.put("processingTickets", processingTickets);
        stats.put("closedTickets", closedTickets);
        stats.put("todayTickets", todayTickets);

        return stats;
    }
}
