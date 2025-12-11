package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.dto.response.PaymentResponse;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.service.RechargeOrderService;
import com.nonfou.github.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 充值订单 Controller
 *
 * 注: 支付功能待接入 Stripe
 */
@Slf4j
@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    @Autowired
    private RechargeOrderService rechargeOrderService;

    /**
     * 创建充值订单
     *
     * TODO: 接入 Stripe 支付后，返回 PaymentIntent 的 clientSecret
     */
    @PostMapping("/create")
    public Result<PaymentResponse> createOrder(
            @RequestBody Map<String, Object> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());

            // 创建订单
            RechargeOrder order = rechargeOrderService.createOrder(userId, amount, "pending");

            PaymentResponse response = PaymentResponse.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .amount(order.getAmount())
                    .paymentType("pending")
                    .build();

            // TODO: 接入 Stripe 后，在此创建 PaymentIntent 并设置 clientSecret
            // String clientSecret = stripeService.createPaymentIntent(amount, "usd");
            // response.setClientSecret(clientSecret);

            return Result.error("支付功能暂未开放，敬请期待");

        } catch (Exception e) {
            log.error("创建充值订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付回调 (预留给 Stripe Webhook)
     *
     * TODO: 接入 Stripe 后实现 Webhook 处理
     */
    @PostMapping("/webhook")
    public String paymentWebhook(@RequestBody String payload,
                                  @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        log.info("收到支付回调");
        // TODO: 验证 Stripe 签名并处理支付事件
        return "success";
    }

    /**
     * 查询订单支付状态
     */
    @GetMapping("/query/{orderId}")
    public Result<RechargeOrder> queryOrder(
            @PathVariable Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            RechargeOrder order = rechargeOrderService.getOrderById(userId, orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }

            return Result.success(order);

        } catch (Exception e) {
            log.error("查询订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取充值订单列表
     */
    @GetMapping("/orders")
    public Result<Page<RechargeOrder>> getUserOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Page<RechargeOrder> page = rechargeOrderService.getUserOrders(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/orders/{orderId}")
    public Result<RechargeOrder> getOrderById(
            @PathVariable Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        RechargeOrder order = rechargeOrderService.getOrderById(userId, orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        return Result.success(order);
    }

}
