package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.mapper.RechargeOrderMapper;
import com.nonfou.github.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 充值订单服务
 */
@Slf4j
@Service
public class RechargeOrderService {

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BalanceService balanceService;

    /**
     * 创建充值订单
     */
    @Transactional
    public RechargeOrder createOrder(Long userId, BigDecimal amount, String payMethod) {
        // 验证金额
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单
        RechargeOrder order = new RechargeOrder();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        order.setStatus(0); // 待支付
        order.setPayMethod(payMethod);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        rechargeOrderMapper.insert(order);
        log.info("充值订单创建成功: userId={}, orderNo={}, amount={}", userId, orderNo, amount);

        return order;
    }

    /**
     * 更新订单的 PaymentIntent ID
     */
    @Transactional
    public void updatePaymentIntentId(Long orderId, String paymentIntentId) {
        RechargeOrder order = rechargeOrderMapper.selectById(orderId);
        if (order != null) {
            order.setPaymentIntentId(paymentIntentId);
            order.setUpdatedAt(LocalDateTime.now());
            rechargeOrderMapper.updateById(order);
            log.info("更新订单 PaymentIntent ID: orderId={}, paymentIntentId={}", orderId, paymentIntentId);
        }
    }

    /**
     * 处理支付回调
     */
    @Transactional
    public void processPayment(String orderNo, String tradeNo) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 0) {
            log.warn("订单状态异常,跳过处理: orderNo={}, status={}", orderNo, order.getStatus());
            return; // 已处理过,防止重复回调
        }

        // 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setTradeNo(tradeNo);
        order.setUpdatedAt(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);

        // 增加用户余额
        balanceService.addBalance(order.getUserId(), order.getAmount(), "recharge", order.getId(), "充值");

        log.info("充值订单支付成功: orderNo={}, amount={}", orderNo, order.getAmount());
    }

    /**
     * 根据订单号获取订单
     */
    public RechargeOrder getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        return rechargeOrderMapper.selectOne(wrapper);
    }

    /**
     * 处理支付(已废弃 - 仅用于模拟支付)
     */
    @Deprecated
    @Transactional
    public void processPayment(Long orderId) {
        RechargeOrder order = rechargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态异常");
        }

        // 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setTradeNo("MOCK_" + UUID.randomUUID().toString().replaceAll("-", ""));
        order.setUpdatedAt(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);

        // 增加用户余额
        balanceService.addBalance(order.getUserId(), order.getAmount(), "recharge", order.getId(), "充值");

        log.info("充值订单支付成功: orderId={}, amount={}", orderId, order.getAmount());
    }

    /**
     * 获取用户充值订单列表
     */
    public Page<RechargeOrder> getUserOrders(Long userId, int pageNum, int pageSize) {
        Page<RechargeOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getUserId, userId)
                .orderByDesc(RechargeOrder::getCreatedAt);

        return rechargeOrderMapper.selectPage(page, wrapper);
    }

    /**
     * 获取订单详情
     */
    public RechargeOrder getOrderById(Long userId, Long orderId) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getId, orderId)
                .eq(RechargeOrder::getUserId, userId);

        return rechargeOrderMapper.selectOne(wrapper);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "RO" + System.currentTimeMillis() + (int)(Math.random() * 10000);
    }
}
