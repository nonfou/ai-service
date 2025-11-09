package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.BalanceLog;
import com.nonfou.github.mapper.BalanceLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 余额日志服务
 */
@Slf4j
@Service
public class BalanceLogService {

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    /**
     * 获取用户余额变动日志（分页）
     */
    public Page<BalanceLog> getUserBalanceLogs(Long userId, int pageNum, int pageSize) {
        Page<BalanceLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BalanceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceLog::getUserId, userId)
                .orderByDesc(BalanceLog::getCreatedAt);

        return balanceLogMapper.selectPage(page, wrapper);
    }

    /**
     * 获取用户余额统计（累计充值、累计消费）
     */
    public Map<String, BigDecimal> getUserBalanceStatistics(Long userId) {
        LambdaQueryWrapper<BalanceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceLog::getUserId, userId);

        List<BalanceLog> logs = balanceLogMapper.selectList(wrapper);

        BigDecimal totalRecharge = logs.stream()
                .filter(log -> "recharge".equals(log.getType()) || "subscription".equals(log.getType()))
                .map(BalanceLog::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConsume = logs.stream()
                .filter(log -> "consume".equals(log.getType()) || "subscribe".equals(log.getType()))
                .map(log -> log.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("totalRecharge", totalRecharge);
        stats.put("totalConsume", totalConsume);

        return stats;
    }
}
