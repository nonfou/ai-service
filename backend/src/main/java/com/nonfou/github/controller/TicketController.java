package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.entity.TicketMessage;
import com.nonfou.github.service.TicketService;
import com.nonfou.github.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建工单
     */
    @PostMapping
    public Result<Ticket> createTicket(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> body) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            String subject = body.get("subject");
            String content = body.get("content");
            String priority = body.getOrDefault("priority", "normal");

            if (subject == null || content == null) {
                return Result.error("主题和内容不能为空");
            }

            Ticket ticket = ticketService.createTicket(userId, subject, content, priority);
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
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getUserIdFromToken(authorization);
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
    public Result<Map<String, Object>> getTicketDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ticketId) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Ticket ticket = ticketService.getTicketById(userId, ticketId);
        if (ticket == null) {
            return Result.error("工单不存在");
        }

        List<TicketMessage> messages = ticketService.getTicketMessages(ticketId);

        Map<String, Object> data = new HashMap<>();
        data.put("ticket", ticket);
        data.put("messages", messages);

        return Result.success(data);
    }

    /**
     * 回复工单
     */
    @PostMapping("/{ticketId}/reply")
    public Result<TicketMessage> replyTicket(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> body) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            String message = body.get("message");
            if (message == null || message.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }

            TicketMessage ticketMessage = ticketService.replyTicket(userId, ticketId, message);
            return Result.success(ticketMessage);
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
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ticketId) {
        Long userId = getUserIdFromToken(authorization);
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

    private Long getUserIdFromToken(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        if (!jwtUtil.validateToken(token)) {
            return null;
        }

        return jwtUtil.getUserIdFromToken(token);
    }
}
