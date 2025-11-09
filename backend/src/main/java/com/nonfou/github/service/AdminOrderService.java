package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.mapper.RechargeOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员订单管理服务
 */
@Slf4j
@Service
public class AdminOrderService {

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private BalanceService balanceService;

    /**
     * 获取订单列表
     */
    public Page<RechargeOrder> getOrderList(int pageNum, int pageSize, Integer status, String orderNo) {
        Page<RechargeOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(RechargeOrder::getStatus, status);
        }

        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        }

        wrapper.orderByDesc(RechargeOrder::getCreatedAt);

        return rechargeOrderMapper.selectPage(page, wrapper);
    }

    /**
     * 获取订单详情
     */
    public RechargeOrder getOrderDetail(Long orderId) {
        return rechargeOrderMapper.selectById(orderId);
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Integer status) {
        RechargeOrder order = rechargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);

        log.info("订单状态更新: orderId={}, status={}", orderId, status);
    }

    /**
     * 手动完成订单
     */
    @Transactional
    public void completeOrder(Long orderId, String tradeNo) {
        RechargeOrder order = rechargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态异常,无法完成");
        }

        // 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setTradeNo(tradeNo != null ? tradeNo : "ADMIN_" + System.currentTimeMillis());
        order.setUpdatedAt(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);

        // 增加用户余额
        balanceService.addBalance(order.getUserId(), order.getAmount(), "recharge", order.getId(), "管理员手动完成订单");

        log.info("管理员手动完成订单: orderId={}, amount={}", orderId, order.getAmount());
    }

    /**
     * 退款
     */
    @Transactional
    public void refundOrder(Long orderId, String reason) {
        RechargeOrder order = rechargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 1) {
            throw new RuntimeException("订单未支付,无法退款");
        }

        // 更新订单状态为已取消
        order.setStatus(2);
        order.setUpdatedAt(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);

        // 扣减用户余额
        balanceService.deductBalance(order.getUserId(), order.getAmount(), "refund", order.getId(),
                "订单退款: " + (reason != null ? reason : "管理员操作"));

        log.info("订单退款: orderId={}, amount={}, reason={}", orderId, order.getAmount(), reason);
    }

    /**
     * 获取订单统计
     */
    public Map<String, Object> getOrderStatistics() {
        // 总订单数
        Long totalOrders = rechargeOrderMapper.selectCount(null);

        // 待支付订单数
        LambdaQueryWrapper<RechargeOrder> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RechargeOrder::getStatus, 0);
        Long pendingOrders = rechargeOrderMapper.selectCount(pendingWrapper);

        // 已支付订单数
        LambdaQueryWrapper<RechargeOrder> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(RechargeOrder::getStatus, 1);
        Long paidOrders = rechargeOrderMapper.selectCount(paidWrapper);

        // 今日订单数
        LambdaQueryWrapper<RechargeOrder> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(RechargeOrder::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay());
        Long todayOrders = rechargeOrderMapper.selectCount(todayWrapper);

        // 今日成交金额
        LambdaQueryWrapper<RechargeOrder> todayPaidWrapper = new LambdaQueryWrapper<>();
        todayPaidWrapper.eq(RechargeOrder::getStatus, 1)
                .ge(RechargeOrder::getPayTime, LocalDateTime.now().toLocalDate().atStartOfDay());

        BigDecimal todayAmount = rechargeOrderMapper.selectList(todayPaidWrapper).stream()
                .map(RechargeOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("paidOrders", paidOrders);
        stats.put("todayOrders", todayOrders);
        stats.put("todayAmount", todayAmount);

        return stats;
    }
}
