package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.Subscription;
import com.nonfou.github.entity.SubscriptionPlan;
import com.nonfou.github.mapper.SubscriptionMapper;
import com.nonfou.github.mapper.SubscriptionPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅服务
 */
@Slf4j
@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionPlanMapper planMapper;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private BalanceService balanceService;

    /**
     * 获取所有可用套餐
     */
    public List<SubscriptionPlan> getAvailablePlans() {
        LambdaQueryWrapper<SubscriptionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionPlan::getStatus, 1)
                .orderByAsc(SubscriptionPlan::getSortOrder);

        return planMapper.selectList(wrapper);
    }

    /**
     * 获取套餐详情
     */
    public SubscriptionPlan getPlanById(Long planId) {
        return planMapper.selectById(planId);
    }

    /**
     * 订阅套餐（使用余额扣费）
     */
    @Transactional
    public Subscription subscribe(Long userId, Long planId) {
        // 获取套餐信息
        SubscriptionPlan plan = planMapper.selectById(planId);
        if (plan == null || plan.getStatus() != 1) {
            throw new RuntimeException("套餐不存在或已下架");
        }

        // 检查用户余额
        BigDecimal userBalance = balanceService.getUserBalance(userId);
        if (userBalance.compareTo(plan.getPrice()) < 0) {
            throw new RuntimeException("余额不足，请先充值");
        }

        // 扣除费用
        balanceService.deductBalance(userId, plan.getPrice(), "subscribe", null, "订阅套餐: " + plan.getDisplayName());

        // 创建订阅记录
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlanId(planId);
        subscription.setPlanName(plan.getDisplayName());
        subscription.setAmount(plan.getPrice());
        subscription.setQuotaAmount(plan.getQuotaAmount());
        subscription.setStartDate(LocalDate.now());

        // 根据套餐设置结束日期（这里简单处理为30天）
        subscription.setEndDate(LocalDate.now().plusDays(30));
        subscription.setStatus("active");
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());

        subscriptionMapper.insert(subscription);

        // 增加对应额度到余额
        balanceService.addBalance(userId, plan.getQuotaAmount(), "subscription", subscription.getId(),
                "订阅套餐额度: " + plan.getDisplayName());

        log.info("用户订阅成功: userId={}, planId={}, planName={}", userId, planId, plan.getDisplayName());

        return subscription;
    }

    /**
     * 获取用户订阅历史
     */
    public Page<Subscription> getUserSubscriptions(Long userId, int pageNum, int pageSize) {
        Page<Subscription> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getUserId, userId)
                .orderByDesc(Subscription::getCreatedAt);

        return subscriptionMapper.selectPage(page, wrapper);
    }

    /**
     * 获取用户当前生效的订阅
     */
    public Subscription getActiveSubscription(Long userId) {
        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getUserId, userId)
                .eq(Subscription::getStatus, "active")
                .ge(Subscription::getEndDate, LocalDate.now())
                .orderByDesc(Subscription::getCreatedAt)
                .last("LIMIT 1");

        return subscriptionMapper.selectOne(wrapper);
    }

    /**
     * 取消订阅
     */
    @Transactional
    public void cancelSubscription(Long userId, Long subscriptionId) {
        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getId, subscriptionId)
                .eq(Subscription::getUserId, userId);

        Subscription subscription = subscriptionMapper.selectOne(wrapper);
        if (subscription == null) {
            throw new RuntimeException("订阅记录不存在");
        }

        if (!"active".equals(subscription.getStatus())) {
            throw new RuntimeException("订阅已取消或过期");
        }

        subscription.setStatus("cancelled");
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.updateById(subscription);

        log.info("订阅已取消: userId={}, subscriptionId={}", userId, subscriptionId);
    }
}
