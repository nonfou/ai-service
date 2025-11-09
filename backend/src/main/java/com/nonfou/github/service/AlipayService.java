package com.nonfou.github.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.nonfou.github.config.AlipayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付宝支付服务
 */
@Slf4j
@Service
public class AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    /**
     * 创建PC端支付 (电脑网站支付)
     */
    public String createPcPayment(String orderNo, String subject, String amount) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(orderNo);
        model.setTotalAmount(amount);
        model.setSubject(subject);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);

        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

        if (response.isSuccess()) {
            log.info("支付宝PC支付创建成功: orderNo={}", orderNo);
            return response.getBody();
        } else {
            throw new RuntimeException("创建支付宝支付失败: " + response.getSubMsg());
        }
    }

    /**
     * 创建移动端支付 (手机网站支付)
     */
    public String createWapPayment(String orderNo, String subject, String amount) throws AlipayApiException {
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(orderNo);
        model.setTotalAmount(amount);
        model.setSubject(subject);
        model.setProductCode("QUICK_WAP_WAY");

        request.setBizModel(model);

        AlipayTradeWapPayResponse response = alipayClient.pageExecute(request);

        if (response.isSuccess()) {
            log.info("支付宝WAP支付创建成功: orderNo={}", orderNo);
            return response.getBody();
        } else {
            throw new RuntimeException("创建支付宝支付失败: " + response.getSubMsg());
        }
    }

    /**
     * 验证支付宝回调签名
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证失败", e);
            return false;
        }
    }

    /**
     * 查询订单支付状态
     */
    public AlipayTradeQueryResponse queryOrder(String orderNo) throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);

        request.setBizModel(model);

        AlipayTradeQueryResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            log.info("支付宝订单查询成功: orderNo={}, tradeStatus={}",
                    orderNo, response.getTradeStatus());
        } else {
            log.warn("支付宝订单查询失败: orderNo={}, error={}",
                    orderNo, response.getSubMsg());
        }

        return response;
    }
}
