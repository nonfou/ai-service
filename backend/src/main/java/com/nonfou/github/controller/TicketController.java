package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.ticket.TicketCreateRequest;
import com.nonfou.github.dto.request.ticket.TicketReplyRequest;
import com.nonfou.github.dto.response.ticket.TicketDetailResponse;
import com.nonfou.github.dto.response.ticket.TicketMessageResponse;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.entity.TicketMessage;
import com.nonfou.github.service.TicketService;
import com.nonfou.github.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单系统 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 创建工单
     */
    @PostMapping
    public Result<Ticket> createTicket(
            @Valid @RequestBody TicketCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            Ticket ticket = ticketService.createTicket(userId, request);
            return Result.success(ticket);
        } catch (Exception e) {
            log.error("创建工单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取工单列表
     */
    @GetMapping
    public Result<Page<Ticket>> getTickets(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Page<Ticket> page = ticketService.getUserTickets(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/{ticketId}")
    public Result<TicketDetailResponse> getTicketDetail(
            @PathVariable Long ticketId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Ticket ticket = ticketService.getTicketById(userId, ticketId);
        if (ticket == null) {
            return Result.error("工单不存在");
        }

        List<TicketMessageResponse> messages = ticketService.getTicketMessages(ticketId).stream()
                .map(TicketMessageResponse::fromEntity)
                .toList();

        return Result.success(TicketDetailResponse.builder()
                .ticket(ticket)
                .messages(messages)
                .build());
    }

    /**
     * 回复工单
     */
    @PostMapping("/{ticketId}/reply")
    public Result<TicketMessageResponse> replyTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketReplyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            TicketMessage ticketMessage = ticketService.replyTicket(userId, ticketId, request);
            return Result.success(TicketMessageResponse.fromEntity(ticketMessage));
        } catch (Exception e) {
            log.error("回复工单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 关闭工单
     */
    @PostMapping("/{ticketId}/close")
    public Result<Void> closeTicket(
            @PathVariable Long ticketId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            ticketService.closeTicket(userId, ticketId);
            return Result.success();
        } catch (Exception e) {
            log.error("关闭工单失败", e);
            return Result.error(e.getMessage());
        }
    }

}
