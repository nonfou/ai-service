package com.nonfou.github.service;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.*;
import com.nonfou.github.config.WechatPayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 微信支付服务
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "wechat", name = "enabled", havingValue = "true", matchIfMissing = false)
public class WechatPayService {

    @Autowired
    private Config wechatPaySDKConfig;

    @Autowired
    private WechatPayConfig payConfig;

    /**
     * 创建Native支付 (扫码支付)
     */
    public String createNativePayment(String orderNo, String description, Integer amount) {
        try {
            NativePayService service = new NativePayService.Builder().config(wechatPaySDKConfig).build();

            PrepayRequest request = new PrepayRequest();
            Amount amountObj = new Amount();
            amountObj.setTotal(amount); // 单位:分
            request.setAmount(amountObj);
            request.setAppid(payConfig.getAppId());
            request.setMchid(payConfig.getMchId());
            request.setDescription(description);
            request.setNotifyUrl(payConfig.getNotifyUrl());
            request.setOutTradeNo(orderNo);

            PrepayResponse response = service.prepay(request);

            log.info("微信Native支付创建成功: orderNo={}, codeUrl={}", orderNo, response.getCodeUrl());
            return response.getCodeUrl();

        } catch (Exception e) {
            log.error("创建微信支付失败: orderNo={}", orderNo, e);
            throw new RuntimeException("创建微信支付失败: " + e.getMessage());
        }
    }

    /**
     * 查询订单支付状态
     */
    public String queryOrder(String orderNo) {
        try {
            NativePayService service = new NativePayService.Builder().config(wechatPaySDKConfig).build();

            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(payConfig.getMchId());
            request.setOutTradeNo(orderNo);

            var response = service.queryOrderByOutTradeNo(request);

            log.info("微信订单查询成功: orderNo={}, tradeState={}",
                    orderNo, response.getTradeState());

            return response.getTradeState().name();

        } catch (Exception e) {
            log.error("微信订单查询失败: orderNo={}", orderNo, e);
            return null;
        }
    }

    /**
     * 关闭订单
     */
    public void closeOrder(String orderNo) {
        try {
            NativePayService service = new NativePayService.Builder().config(wechatPaySDKConfig).build();

            CloseOrderRequest request = new CloseOrderRequest();
            request.setMchid(payConfig.getMchId());
            request.setOutTradeNo(orderNo);

            service.closeOrder(request);

            log.info("微信订单关闭成功: orderNo={}", orderNo);

        } catch (Exception e) {
            log.error("微信订单关闭失败: orderNo={}", orderNo, e);
        }
    }
}
