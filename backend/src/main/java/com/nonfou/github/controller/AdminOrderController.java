package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.annotation.RequireAdmin;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.service.AdminOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员订单管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequireAdmin
public class AdminOrderController {

    @Autowired
    private AdminOrderService adminOrderService;

    /**
     * 获取订单列表
     */
    @GetMapping
    public Result<Page<RechargeOrder>> getOrderList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {

        Page<RechargeOrder> page = adminOrderService.getOrderList(pageNum, pageSize, status, orderNo);
        return Result.success(page);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{orderId}")
    public Result<RechargeOrder> getOrderDetail(@PathVariable Long orderId) {
        try {
            RechargeOrder order = adminOrderService.getOrderDetail(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            return Result.success(order);
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{orderId}/status")
    public Result<Void> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, Integer> body) {

        try {
            Integer status = body.get("status");
            if (status == null) {
                return Result.error("状态值不能为空");
            }

            adminOrderService.updateOrderStatus(orderId, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新订单状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动完成订单
     */
    @PostMapping("/{orderId}/complete")
    public Result<Void> completeOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            String tradeNo = body != null ? body.get("tradeNo") : null;
            adminOrderService.completeOrder(orderId, tradeNo);
            return Result.success();
        } catch (Exception e) {
            log.error("手动完成订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 退款
     */
    @PostMapping("/{orderId}/refund")
    public Result<Void> refundOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            String reason = body != null ? body.get("reason") : null;
            adminOrderService.refundOrder(orderId, reason);
            return Result.success();
        } catch (Exception e) {
            log.error("订单退款失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getOrderStatistics() {
        Map<String, Object> stats = adminOrderService.getOrderStatistics();
        return Result.success(stats);
    }
}
