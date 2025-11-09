package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.Model;
import com.nonfou.github.entity.User;
import com.nonfou.github.service.AdminService;
import com.nonfou.github.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ModelService modelService;

    /**
     * 管理员登录 (不需要权限验证)
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || password == null) {
                return Result.error("用户名和密码不能为空");
            }

            Map<String, Object> result = adminService.login(username, password);
            return Result.success(result);
        } catch (Exception e) {
            log.error("管理员登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置管理员密码 (临时接口,仅用于开发测试)
     */
    @PostMapping("/reset-password")
    public Result<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String newPassword = body.get("newPassword");

            if (username == null || newPassword == null) {
                return Result.error("用户名和新密码不能为空");
            }

            Map<String, Object> result = adminService.resetAdminPassword(username, newPassword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/users/statistics")
    public Result<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = adminService.getPlatformStatistics();
        return Result.success(stats);
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public Result<Page<User>> getUserList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer status) {

        Page<User> page = adminService.getUserList(pageNum, pageSize, email, status);
        return Result.success(page);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{userId}")
    public Result<User> getUserDetail(@PathVariable Long userId) {
        try {
            User user = adminService.getUserDetail(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {

        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.error("状态值无效");
            }

            adminService.updateUserStatus(userId, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 调整用户余额
     */
    @PostMapping("/users/{userId}/adjust-balance")
    public Result<Void> adjustUserBalance(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {

        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
            String remark = (String) body.getOrDefault("remark", "管理员调整余额");

            adminService.adjustUserBalance(userId, amount, remark);
            return Result.success();
        } catch (Exception e) {
            log.error("调整用户余额失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取平台统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getPlatformStatistics() {
        Map<String, Object> stats = adminService.getPlatformStatistics();
        return Result.success(stats);
    }

    /**
     * 获取所有模型(包括禁用的)
     */
    @GetMapping("/models")
    public Result<List<Model>> getAllModels() {
        List<Model> models = modelService.getAllModels();
        return Result.success(models);
    }

    /**
     * 更新模型
     */
    @PutMapping("/models/{id}")
    public Result<Void> updateModel(
            @PathVariable Long id,
            @RequestBody Model model) {

        try {
            modelService.updateModel(id, model);
            return Result.success();
        } catch (Exception e) {
            log.error("更新模型失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新模型状态
     */
    @PutMapping("/models/{id}/status")
    public Result<Void> updateModelStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {

        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.error("状态值无效");
            }

            modelService.updateModelStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新模型状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/models/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        try {
            modelService.deleteModel(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除模型失败", e);
            return Result.error(e.getMessage());
        }
    }
}
