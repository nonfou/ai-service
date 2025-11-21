package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.dto.response.PaymentResponse;
import com.nonfou.github.entity.RechargeOrder;
import com.nonfou.github.service.AlipayService;
import com.nonfou.github.service.RechargeOrderService;
import com.nonfou.github.service.WechatPayService;
import com.nonfou.github.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    private AlipayService alipayService;

    @Autowired(required = false)
    private WechatPayService wechatPayService;

    /**
     * 创建充值订单并发起支付
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
            String payMethod = body.getOrDefault("payMethod", "alipay").toString();

            // 创建订单
            RechargeOrder order = rechargeOrderService.createOrder(userId, amount, payMethod);

            PaymentResponse response = PaymentResponse.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .amount(order.getAmount())
                    .paymentType(payMethod)
                    .build();

            // 根据支付方式调用对应的支付接口
            if ("alipay".equals(payMethod)) {
                // 支付宝支付
                String paymentUrl = alipayService.createPcPayment(
                        order.getOrderNo(),
                        "账户充值",
                        amount.toString()
                );
                response.setPaymentUrl(paymentUrl);

            } else if ("wechat".equals(payMethod)) {
                // 微信支付
                if (wechatPayService == null) {
                    return Result.error("微信支付暂未启用");
                }
                String qrCode = wechatPayService.createNativePayment(
                        order.getOrderNo(),
                        "账户充值",
                        amount.multiply(new BigDecimal("100")).intValue() // 转换为分
                );
                response.setQrCode(qrCode);

            } else {
                return Result.error("不支持的支付方式");
            }

            return Result.success(response);

        } catch (Exception e) {
            log.error("创建充值订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付宝异步回调
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        try {
            // 获取所有请求参数
            Map<String, String> params = request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue()[0]
                    ));

            log.info("收到支付宝回调: {}", params);

            // 验证签名
            if (!alipayService.verifyNotify(params)) {
                log.error("支付宝签名验证失败");
                return "failure";
            }

            // 获取订单号和交易号
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");

            // 交易成功
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                rechargeOrderService.processPayment(orderNo, tradeNo);
            }

            return "success";

        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return "failure";
        }
    }

    /**
     * 支付宝同步跳转
     */
    @GetMapping("/alipay/return")
    public String alipayReturn(HttpServletRequest request) {
        try {
            // 获取所有请求参数
            Map<String, String> params = request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue()[0]
                    ));

            log.info("收到支付宝同步跳转: {}", params);

            // 验证签名
            if (!alipayService.verifyNotify(params)) {
                log.error("支付宝签名验证失败");
                return "redirect:/wallet?payment=failure";
            }

            String orderNo = params.get("out_trade_no");
            log.info("支付宝支付成功: orderNo={}", orderNo);

            return "redirect:/wallet?payment=success&orderNo=" + orderNo;

        } catch (Exception e) {
            log.error("处理支付宝同步跳转失败", e);
            return "redirect:/wallet?payment=failure";
        }
    }

    /**
     * 微信支付异步回调
     */
    @PostMapping("/wechat/notify")
    public Map<String, String> wechatNotify(@RequestBody String requestBody) {
        Map<String, String> response = new HashMap<>();
        try {
            log.info("收到微信支付回调: {}", requestBody);

            // TODO: 实现微信支付回调验签和处理
            // 注意: 微信支付回调需要解密和验签,这里简化处理

            response.put("code", "SUCCESS");
            response.put("message", "成功");

        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            response.put("code", "FAIL");
            response.put("message", e.getMessage());
        }
        return response;
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
