package com.nonfou.github.service;

import com.nonfou.github.config.StripeConfig;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Stripe 支付服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfig stripeConfig;
    private final RechargeOrderService rechargeOrderService;

    /**
     * 检查 Stripe 是否已配置
     */
    public boolean isConfigured() {
        return stripeConfig.isConfigured();
    }

    /**
     * 创建 PaymentIntent
     * 支持的付款方式：信用卡、支付宝、微信支付等
     *
     * @param amount  金额（美元）
     * @param orderNo 订单号
     * @return PaymentIntent
     */
    public PaymentIntent createPaymentIntent(BigDecimal amount, String orderNo) throws StripeException {
        // Stripe 使用最小货币单位（美分）
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(stripeConfig.getCurrency())
                .putMetadata("order_no", orderNo)
                // 显式指定支持的支付方式，包括支付宝和微信支付
                .addPaymentMethodType("card")
                .addPaymentMethodType("alipay")
                .addPaymentMethodType("wechat_pay")
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.info("Created PaymentIntent: {} for order: {}, amount: {} cents, methods: card, alipay, wechat_pay",
                paymentIntent.getId(), orderNo, amountInCents);

        return paymentIntent;
    }

    /**
     * 处理 Webhook 事件
     *
     * @param payload   请求体
     * @param signature Stripe-Signature 头
     * @return 是否处理成功
     */
    public boolean handleWebhookEvent(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(
                    payload, signature, stripeConfig.getWebhookSecret());

            log.info("Processing Stripe event: type={}, id={}", event.getType(), event.getId());

            // 处理支付成功事件
            if ("payment_intent.succeeded".equals(event.getType())) {
                return handlePaymentIntentSucceeded(event);
            }

            // 处理支付失败事件
            if ("payment_intent.payment_failed".equals(event.getType())) {
                return handlePaymentIntentFailed(event);
            }

            log.info("Unhandled event type: {}", event.getType());
            return true;

        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error processing webhook event", e);
            return false;
        }
    }

    /**
     * 处理支付成功事件
     */
    private boolean handlePaymentIntentSucceeded(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent()) {
                StripeObject stripeObject = deserializer.getObject().get();
                if (stripeObject instanceof PaymentIntent paymentIntent) {
                    String orderNo = paymentIntent.getMetadata().get("order_no");
                    String paymentIntentId = paymentIntent.getId();

                    log.info("Payment succeeded for order: {}, paymentIntent: {}", orderNo, paymentIntentId);

                    // 处理支付成功，更新订单状态和用户余额
                    rechargeOrderService.processPayment(orderNo, paymentIntentId);
                    return true;
                }
            }
            log.warn("Failed to deserialize PaymentIntent from event");
            return false;
        } catch (Exception e) {
            log.error("Error handling payment_intent.succeeded", e);
            return false;
        }
    }

    /**
     * 处理支付失败事件
     */
    private boolean handlePaymentIntentFailed(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent()) {
                StripeObject stripeObject = deserializer.getObject().get();
                if (stripeObject instanceof PaymentIntent paymentIntent) {
                    String orderNo = paymentIntent.getMetadata().get("order_no");
                    log.warn("Payment failed for order: {}, reason: {}",
                            orderNo, paymentIntent.getLastPaymentError());
                    // 可以在这里更新订单状态为失败
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error handling payment_intent.payment_failed", e);
            return false;
        }
    }

    /**
     * 获取 PaymentIntent
     *
     * @param paymentIntentId PaymentIntent ID
     * @return PaymentIntent
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * 获取 Publishable Key
     */
    public String getPublishableKey() {
        return stripeConfig.getPublishableKey();
    }
}
