package com.nonfou.github.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.common.Result;
import com.nonfou.github.entity.BalanceLog;
import com.nonfou.github.service.BalanceLogService;
import com.nonfou.github.service.BalanceService;
import com.nonfou.github.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 余额管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private BalanceLogService balanceLogService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取用户余额
     */
    @GetMapping
    public Result<Map<String, Object>> getUserBalance(@RequestHeader("Authorization") String authorization) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        BigDecimal balance = balanceService.getUserBalance(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("balance", balance);

        return Result.success(data);
    }

    /**
     * 获取余额变动日志
     */
    @GetMapping("/logs")
    public Result<Page<BalanceLog>> getBalanceLogs(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Page<BalanceLog> page = balanceLogService.getUserBalanceLogs(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取余额统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, BigDecimal>> getBalanceStatistics(
            @RequestHeader("Authorization") String authorization) {
        Long userId = getUserIdFromToken(authorization);
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        Map<String, BigDecimal> stats = balanceLogService.getUserBalanceStatistics(userId);
        return Result.success(stats);
    }

    private Long getUserIdFromToken(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        if (!jwtUtil.validateToken(token)) {
            return null;
        }

        return jwtUtil.getUserIdFromToken(token);
    }
}
