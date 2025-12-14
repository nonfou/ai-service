package com.nonfou.github.controller;

import com.nonfou.github.annotation.RequireAdmin;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.SubscriptionPlan;
import com.nonfou.github.service.AdminPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员套餐管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/plans")
@RequireAdmin
public class AdminPlanController {

    @Autowired
    private AdminPlanService adminPlanService;

    /**
     * 获取所有套餐列表(包括禁用的)
     */
    @GetMapping
    public Result<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = adminPlanService.getAllPlans();
        return Result.success(plans);
    }

    /**
     * 获取套餐详情
     */
    @GetMapping("/{planId}")
    public Result<SubscriptionPlan> getPlanDetail(@PathVariable Long planId) {
        try {
            SubscriptionPlan plan = adminPlanService.getPlanDetail(planId);
            if (plan == null) {
                return Result.error("套餐不存在");
            }
            return Result.success(plan);
        } catch (Exception e) {
            log.error("获取套餐详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建新套餐
     */
    @PostMapping
    public Result<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        try {
            SubscriptionPlan createdPlan = adminPlanService.createPlan(plan);
            return Result.success(createdPlan);
        } catch (Exception e) {
            log.error("创建套餐失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新套餐信息
     */
    @PutMapping("/{planId}")
    public Result<Void> updatePlan(
            @PathVariable Long planId,
            @RequestBody SubscriptionPlan plan) {

        try {
            adminPlanService.updatePlan(planId, plan);
            return Result.success();
        } catch (Exception e) {
            log.error("更新套餐信息失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除套餐(软删除)
     */
    @DeleteMapping("/{planId}")
    public Result<Void> deletePlan(@PathVariable Long planId) {
        try {
            adminPlanService.deletePlan(planId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除套餐失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新套餐状态
     */
    @PutMapping("/{planId}/status")
    public Result<Void> updatePlanStatus(
            @PathVariable Long planId,
            @RequestBody Map<String, Integer> body) {

        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.error("状态值无效");
            }

            adminPlanService.updatePlanStatus(planId, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新套餐状态失败", e);
            return Result.error(e.getMessage());
        }
    }
}
