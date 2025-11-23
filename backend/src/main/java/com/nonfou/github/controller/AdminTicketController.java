package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.annotation.RequireAdmin;
import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.ticket.AdminTicketReplyRequest;
import com.nonfou.github.dto.response.ticket.TicketDetailResponse;
import com.nonfou.github.dto.response.ticket.TicketMessageResponse;
import com.nonfou.github.entity.Ticket;
import com.nonfou.github.service.AdminTicketService;
import com.nonfou.github.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员工单管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tickets")
@RequireAdmin
@RequiredArgsConstructor
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    /**
     * 获取工单列表
     */
    @GetMapping
    public Result<Page<Ticket>> getTicketList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long ticketId) {

        Page<Ticket> page = adminTicketService.getTicketList(pageNum, pageSize, status, priority, ticketId);
        return Result.success(page);
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/{ticketId}")
    public Result<TicketDetailResponse> getTicketDetail(@PathVariable Long ticketId) {
        try {
            TicketDetailResponse result = adminTicketService.getTicketDetail(ticketId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取工单详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员回复工单
     */
    @PostMapping("/{ticketId}/reply")
    public Result<TicketMessageResponse> replyTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AdminTicketReplyRequest requestBody) {

        try {
            Long adminId = SecurityUtil.getCurrentUserId();
            if (adminId == null) {
                return Result.error(401, "未授权");
            }

            var ticketMessage = adminTicketService.replyTicket(adminId, ticketId, requestBody.getMessage());
            return Result.success(TicketMessageResponse.fromEntity(ticketMessage));
        } catch (Exception e) {
            log.error("回复工单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新工单状态
     */
    @PutMapping("/{ticketId}/status")
    public Result<Void> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> body) {

        try {
            String status = body.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error("状态值不能为空");
            }

            adminTicketService.updateTicketStatus(ticketId, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新工单状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新工单优先级
     */
    @PutMapping("/{ticketId}/priority")
    public Result<Void> updateTicketPriority(
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> body) {

        try {
            String priority = body.get("priority");
            if (priority == null || priority.isEmpty()) {
                return Result.error("优先级不能为空");
            }

            adminTicketService.updateTicketPriority(ticketId, priority);
            return Result.success();
        } catch (Exception e) {
            log.error("更新工单优先级失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取工单统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getTicketStatistics() {
        Map<String, Object> stats = adminTicketService.getTicketStatistics();
        return Result.success(stats);
    }
}
