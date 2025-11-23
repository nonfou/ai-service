package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.dto.response.ticket.TicketDetailResponse;
import com.nonfou.github.dto.response.ticket.TicketMessageResponse;
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
import java.util.Map;

/**
 * 管理员工单管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTicketService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final TicketNotificationService ticketNotificationService;

    /**
     * 获取工单列表
     */
    public Page<Ticket> getTicketList(int pageNum, int pageSize, String status, String priority, Long ticketId) {
        Page<Ticket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Ticket::getStatus, TicketStatus.normalize(status));
        }

        if (priority != null && !priority.isEmpty()) {
            wrapper.eq(Ticket::getPriority, priority);
        }

        if (ticketId != null) {
            wrapper.eq(Ticket::getId, ticketId);
        }

        wrapper.orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> result = ticketMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::normalizeTicketStatus);
        return result;
    }

    /**
     * 获取工单详情及消息列表
     */
    public TicketDetailResponse getTicketDetail(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }
        normalizeTicketStatus(ticket);

        LambdaQueryWrapper<TicketMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(TicketMessage::getTicketId, ticketId)
                .orderByAsc(TicketMessage::getCreatedAt);
        List<TicketMessage> messages = ticketMessageMapper.selectList(messageWrapper);

        List<TicketMessageResponse> responses = messages.stream()
                .map(TicketMessageResponse::fromEntity)
                .toList();

        return TicketDetailResponse.builder()
                .ticket(ticket)
                .messages(responses)
                .build();
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
        ticketMessage.setAdminId(adminId);
        ticketMessage.setIsStaff(1);
        ticketMessage.setMessage(message);
        ticketMessage.setCreatedAt(LocalDateTime.now());

        ticketMessageMapper.insert(ticketMessage);

        // 更新工单状态为处理中
        if (!TicketStatus.CLOSED.getValue().equals(ticket.getStatus())) {
            ticket.setStatus(TicketStatus.PROCESSING.getValue());
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketMapper.updateById(ticket);
        }
        normalizeTicketStatus(ticket);

        log.info("管理员回复工单: adminId={}, ticketId={}", adminId, ticketId);

        ticketNotificationService.notifyUserForReply(ticket, message);

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

        ticket.setStatus(TicketStatus.validateOrThrow(status));
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        normalizeTicketStatus(ticket);

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

        return Map.of(
                "totalTickets", totalTickets,
                "pendingTickets", pendingTickets,
                "processingTickets", processingTickets,
                "closedTickets", closedTickets,
                "todayTickets", todayTickets
        );
    }

    private void normalizeTicketStatus(Ticket ticket) {
        if (ticket == null) {
            return;
        }
        ticket.setStatus(TicketStatus.normalize(ticket.getStatus()));
    }
}
