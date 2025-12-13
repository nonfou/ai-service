package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.config.StripeConfig;
import com.nonfou.github.dto.response.PaymentResponse;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.service.RechargeOrderService;
import com.nonfou.github.service.StripeService;
import com.nonfou.github.util.SecurityUtil;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 充值订单 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    @Autowired
    private RechargeOrderService rechargeOrderService;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private StripeConfig stripeConfig;

    /**
     * 获取 Stripe 公钥和配置信息
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getStripeConfig() {
        if (!stripeService.isConfigured()) {
            return Result.error("支付功能暂未开放");
        }

        Map<String, Object> config = new HashMap<>();
        config.put("publishableKey", stripeService.getPublishableKey());
        config.put("currency", stripeConfig.getCurrency());
        config.put("minAmount", stripeConfig.getMinAmount());
        config.put("maxAmount", stripeConfig.getMaxAmount());
        // 预设金额选项
        config.put("presetAmounts", new int[]{5, 10, 20, 50, 100});

        return Result.success(config);
    }

    /**
     * 创建充值订单
     */
    @PostMapping("/create")
    public Result<PaymentResponse> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        // 检查 Stripe 是否已配置
        if (!stripeService.isConfigured()) {
            return Result.error("支付功能暂未开放，敬请期待");
        }

        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());

            // 验证金额范围
            if (amount.compareTo(stripeConfig.getMinAmount()) < 0) {
                return Result.error("充值金额不能小于 $" + stripeConfig.getMinAmount());
            }
            if (amount.compareTo(stripeConfig.getMaxAmount()) > 0) {
                return Result.error("充值金额不能大于 $" + stripeConfig.getMaxAmount());
            }

            // 创建订单
            RechargeOrder order = rechargeOrderService.createOrder(userId, amount, "stripe");

            // 创建 Stripe PaymentIntent
            PaymentIntent paymentIntent = stripeService.createPaymentIntent(amount, order.getOrderNo());

            // 更新订单的 PaymentIntent ID
            rechargeOrderService.updatePaymentIntentId(order.getId(), paymentIntent.getId());

            PaymentResponse response = PaymentResponse.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .amount(order.getAmount())
                    .paymentType("stripe")
                    .clientSecret(paymentIntent.getClientSecret())
                    .build();

            return Result.success(response);

        } catch (Exception e) {
            log.error("创建充值订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * Stripe Webhook 回调
     */
    @PostMapping("/webhook")
    public String paymentWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        log.info("收到 Stripe Webhook 回调");

        if (signature == null || signature.isBlank()) {
            log.error("Webhook 请求缺少 Stripe-Signature 头");
            return "error: missing signature";
        }

        boolean success = stripeService.handleWebhookEvent(payload, signature);

        if (success) {
            return "success";
        } else {
            return "error";
        }
    }

    /**
     * 查询订单支付状态
     */
    @GetMapping("/query/{orderId}")
    public Result<Map<String, Object>> queryOrder(@PathVariable Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            RechargeOrder order = rechargeOrderService.getOrderById(userId, orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);

            // 如果订单未支付且有 PaymentIntent ID，查询 Stripe 状态
            if (order.getStatus() == 0 && order.getPaymentIntentId() != null) {
                try {
                    PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(order.getPaymentIntentId());
                    result.put("stripeStatus", paymentIntent.getStatus());
                } catch (Exception e) {
                    log.warn("查询 Stripe PaymentIntent 状态失败: {}", e.getMessage());
                }
            }

            return Result.success(result);

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
    public Result<RechargeOrder> getOrderById(@PathVariable Long orderId) {
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
