package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.entity.SubscriptionPlan;
import com.nonfou.github.mapper.SubscriptionPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员套餐管理服务
 */
@Slf4j
@Service
public class AdminPlanService {

    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;

    /**
     * 获取所有套餐列表(包括禁用的)
     */
    public List<SubscriptionPlan> getAllPlans() {
        LambdaQueryWrapper<SubscriptionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SubscriptionPlan::getSortOrder);
        return subscriptionPlanMapper.selectList(wrapper);
    }

    /**
     * 获取套餐详情
     */
    public SubscriptionPlan getPlanDetail(Long planId) {
        return subscriptionPlanMapper.selectById(planId);
    }

    /**
     * 创建新套餐
     */
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        // 检查套餐标识名称是否重复
        LambdaQueryWrapper<SubscriptionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionPlan::getPlanName, plan.getPlanName());
        Long count = subscriptionPlanMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("套餐标识名称已存在");
        }

        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());

        // 如果没有设置状态,默认为启用
        if (plan.getStatus() == null) {
            plan.setStatus(1);
        }

        // 如果没有设置排序,默认为0
        if (plan.getSortOrder() == null) {
            plan.setSortOrder(0);
        }

        subscriptionPlanMapper.insert(plan);

        log.info("创建新套餐: planName={}, displayName={}", plan.getPlanName(), plan.getDisplayName());

        return plan;
    }

    /**
     * 更新套餐信息
     */
    @Transactional
    public void updatePlan(Long planId, SubscriptionPlan plan) {
        SubscriptionPlan existingPlan = subscriptionPlanMapper.selectById(planId);
        if (existingPlan == null) {
            throw new RuntimeException("套餐不存在");
        }

        // 如果修改了套餐标识名称,检查是否重复
        if (plan.getPlanName() != null && !plan.getPlanName().equals(existingPlan.getPlanName())) {
            LambdaQueryWrapper<SubscriptionPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SubscriptionPlan::getPlanName, plan.getPlanName());
            Long count = subscriptionPlanMapper.selectCount(wrapper);
            if (count > 0) {
                throw new RuntimeException("套餐标识名称已存在");
            }
            existingPlan.setPlanName(plan.getPlanName());
        }

        // 更新其他字段
        if (plan.getDisplayName() != null) {
            existingPlan.setDisplayName(plan.getDisplayName());
        }
        if (plan.getDescription() != null) {
            existingPlan.setDescription(plan.getDescription());
        }
        if (plan.getOriginalPrice() != null) {
            existingPlan.setOriginalPrice(plan.getOriginalPrice());
        }
        if (plan.getPrice() != null) {
            existingPlan.setPrice(plan.getPrice());
        }
        if (plan.getQuotaAmount() != null) {
            existingPlan.setQuotaAmount(plan.getQuotaAmount());
        }
        if (plan.getFeatures() != null) {
            existingPlan.setFeatures(plan.getFeatures());
        }
        if (plan.getColorTheme() != null) {
            existingPlan.setColorTheme(plan.getColorTheme());
        }
        if (plan.getBadgeText() != null) {
            existingPlan.setBadgeText(plan.getBadgeText());
        }
        if (plan.getSortOrder() != null) {
            existingPlan.setSortOrder(plan.getSortOrder());
        }
        if (plan.getStatus() != null) {
            existingPlan.setStatus(plan.getStatus());
        }

        existingPlan.setUpdatedAt(LocalDateTime.now());
        subscriptionPlanMapper.updateById(existingPlan);

        log.info("更新套餐信息: planId={}, planName={}, colorTheme={}, badgeText={}",
                planId, existingPlan.getPlanName(), existingPlan.getColorTheme(), existingPlan.getBadgeText());
    }

    /**
     * 删除套餐(软删除,实际是禁用)
     */
    @Transactional
    public void deletePlan(Long planId) {
        SubscriptionPlan plan = subscriptionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("套餐不存在");
        }

        // 软删除:设置状态为禁用
        plan.setStatus(0);
        plan.setUpdatedAt(LocalDateTime.now());
        subscriptionPlanMapper.updateById(plan);

        log.info("删除套餐: planId={}, planName={}", planId, plan.getPlanName());
    }

    /**
     * 更新套餐状态
     */
    @Transactional
    public void updatePlanStatus(Long planId, Integer status) {
        SubscriptionPlan plan = subscriptionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("套餐不存在");
        }

        plan.setStatus(status);
        plan.setUpdatedAt(LocalDateTime.now());
        subscriptionPlanMapper.updateById(plan);

        log.info("更新套餐状态: planId={}, status={}", planId, status);
    }
}
