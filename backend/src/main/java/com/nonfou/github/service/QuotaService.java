package com.nonfou.github.service;

import com.nonfou.github.entity.UserQuota;
import com.nonfou.github.mapper.UserQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 配额服务
 * 管理用户的每日/每月配额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaMapper userQuotaMapper;
    private final RedisService redisService;
    private final EmailService emailService;

    @Value("${backend.quota.default-daily-limit:100.00}")
    private BigDecimal defaultDailyLimit;

    @Value("${backend.quota.default-monthly-limit:2000.00}")
    private BigDecimal defaultMonthlyLimit;

    @Value("${backend.quota.enforce:true}")
    private boolean enforceQuota;

    private static final String QUOTA_REDIS_PREFIX = "quota:";

    /**
     * 检查配额是否充足
     *
     * @param userId 用户ID
     * @param cost   本次请求成本
     * @throws RuntimeException 如果配额不足
     */
    public void checkQuota(Long userId, BigDecimal cost) {
        if (!enforceQuota) {
            return; // 配额检查已禁用
        }

        // 检查每日配额
        UserQuota dailyQuota = getOrCreateQuota(userId, "daily");
        if (dailyQuota.isEnabled() && dailyQuota.getUsedAmount().add(cost).compareTo(dailyQuota.getQuotaAmount()) > 0) {
            throw new RuntimeException("每日配额不足: 已使用 " + dailyQuota.getUsedAmount()
                    + " / " + dailyQuota.getQuotaAmount());
        }

        // 检查每月配额
        UserQuota monthlyQuota = getOrCreateQuota(userId, "monthly");
        if (monthlyQuota.isEnabled() && monthlyQuota.getUsedAmount().add(cost).compareTo(monthlyQuota.getQuotaAmount()) > 0) {
            throw new RuntimeException("每月配额不足: 已使用 " + monthlyQuota.getUsedAmount()
                    + " / " + monthlyQuota.getQuotaAmount());
        }
    }

    /**
     * 检查并强制执行配额限制
     *
     * @param userId 用户ID
     * @throws RuntimeException 如果配额已用尽
     */
    public void checkAndEnforceQuota(Long userId) {
        if (!enforceQuota) {
            return; // 配额检查已禁用
        }

        // 检查每日配额
        UserQuota dailyQuota = getOrCreateQuota(userId, "daily");
        if (dailyQuota.isEnabled() && dailyQuota.getUsedAmount().compareTo(dailyQuota.getQuotaAmount()) >= 0) {
            throw new RuntimeException("每日配额已用尽");
        }

        // 检查每月配额
        UserQuota monthlyQuota = getOrCreateQuota(userId, "monthly");
        if (monthlyQuota.isEnabled() && monthlyQuota.getUsedAmount().compareTo(monthlyQuota.getQuotaAmount()) >= 0) {
            throw new RuntimeException("每月配额已用尽");
        }
    }

    /**
     * 扣除配额
     *
     * @param userId 用户ID
     * @param cost   消费金额
     */
    @Transactional
    public void deductQuota(Long userId, BigDecimal cost) {
        // 更新每日配额
        UserQuota dailyQuota = getOrCreateQuota(userId, "daily");
        userQuotaMapper.incrementUsedAmount(dailyQuota.getId(), cost);

        // 更新每月配额
        UserQuota monthlyQuota = getOrCreateQuota(userId, "monthly");
        userQuotaMapper.incrementUsedAmount(monthlyQuota.getId(), cost);

        // 更新 Redis 缓存
        updateRedisCache(userId, "daily", cost);
        updateRedisCache(userId, "monthly", cost);

        log.debug("扣除配额: userId={}, cost={}", userId, cost);
    }

    /**
     * 更新配额使用量
     *
     * @param userId 用户ID
     * @param cost   消费金额
     */
    @Transactional
    public void updateUsage(Long userId, BigDecimal cost) {
        // 更新每日配额
        UserQuota dailyQuota = getOrCreateQuota(userId, "daily");
        userQuotaMapper.incrementUsedAmount(dailyQuota.getId(), cost);

        // 更新每月配额
        UserQuota monthlyQuota = getOrCreateQuota(userId, "monthly");
        userQuotaMapper.incrementUsedAmount(monthlyQuota.getId(), cost);

        // 更新 Redis 缓存
        updateRedisCache(userId, "daily", cost);
        updateRedisCache(userId, "monthly", cost);

        log.debug("更新配额使用: userId={}, cost={}", userId, cost);

        // 检查是否需要告警
        checkAndSendAlert(userId);
    }

    /**
     * 获取或创建配额
     */
    private UserQuota getOrCreateQuota(Long userId, String quotaType) {
        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);

        if (quota == null) {
            // 创建默认配额
            quota = new UserQuota();
            quota.setUserId(userId);
            quota.setQuotaType(quotaType);
            quota.setQuotaAmount("daily".equals(quotaType) ? defaultDailyLimit : defaultMonthlyLimit);
            quota.setUsedAmount(BigDecimal.ZERO);
            quota.setIsEnabled(true);
            quota.setAlertThreshold(new BigDecimal("80.00"));

            // 设置重置时间
            if ("daily".equals(quotaType)) {
                quota.setResetAt(LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            } else if ("monthly".equals(quotaType)) {
                quota.setResetAt(LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            }

            userQuotaMapper.insert(quota);
            log.info("创建默认配额: userId={}, quotaType={}, amount={}", userId, quotaType, quota.getQuotaAmount());
        }

        return quota;
    }

    /**
     * 更新 Redis 缓存
     */
    private void updateRedisCache(Long userId, String quotaType, BigDecimal cost) {
        try {
            String key = QUOTA_REDIS_PREFIX + userId + ":" + quotaType;
            String currentValue = redisService.get(key);
            BigDecimal newValue = (currentValue != null ? new BigDecimal(currentValue) : BigDecimal.ZERO).add(cost);

            // 设置过期时间
            long ttl = "daily".equals(quotaType) ? 86400L : 2592000L; // 1天或30天
            redisService.set(key, newValue.toString(), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("更新 Redis 配额缓存失败", e);
        }
    }

    /**
     * 检查并发送告警
     */
    private void checkAndSendAlert(Long userId) {
        List<UserQuota> quotas = userQuotaMapper.selectByUserId(userId);

        for (UserQuota quota : quotas) {
            if (quota.shouldAlert()) {
                // 检查是否最近1小时内已发送过告警
                if (quota.getLastAlertAt() != null &&
                        quota.getLastAlertAt().isAfter(LocalDateTime.now().minusHours(1))) {
                    continue;
                }

                // 发送告警邮件（异步）
                sendQuotaAlertEmail(userId, quota);

                // 更新最后告警时间
                userQuotaMapper.updateLastAlertAt(quota.getId());
            }
        }
    }

    /**
     * 发送配额告警邮件
     */
    private void sendQuotaAlertEmail(Long userId, UserQuota quota) {
        try {
            // TODO: 获取用户邮箱并发送告警
            log.warn("配额告警: userId={}, quotaType={}, usage={}%",
                    userId, quota.getQuotaType(), quota.getUsagePercentage());
        } catch (Exception e) {
            log.error("发送配额告警邮件失败", e);
        }
    }

    /**
     * 获取用户配额信息
     */
    public Map<String, Object> getUserQuotaInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 每日配额
        UserQuota dailyQuota = getOrCreateQuota(userId, "daily");
        result.put("daily", buildQuotaInfo(dailyQuota));

        // 每月配额
        UserQuota monthlyQuota = getOrCreateQuota(userId, "monthly");
        result.put("monthly", buildQuotaInfo(monthlyQuota));

        return result;
    }

    /**
     * 构建配额信息对象
     */
    private Map<String, Object> buildQuotaInfo(UserQuota quota) {
        Map<String, Object> info = new HashMap<>();
        info.put("quotaAmount", quota.getQuotaAmount());
        info.put("usedAmount", quota.getUsedAmount());
        info.put("remainingAmount", quota.getRemainingAmount());
        info.put("usagePercentage", quota.getUsagePercentage());
        info.put("resetAt", quota.getResetAt());
        info.put("isExceeded", quota.isExceeded());
        info.put("isEnabled", quota.getIsEnabled());
        return info;
    }

    /**
     * 设置用户配额
     */
    @Transactional
    public void setUserQuota(Long userId, String quotaType, BigDecimal amount) {
        UserQuota quota = userQuotaMapper.selectByUserIdAndType(userId, quotaType);

        if (quota == null) {
            quota = new UserQuota();
            quota.setUserId(userId);
            quota.setQuotaType(quotaType);
            quota.setUsedAmount(BigDecimal.ZERO);
            quota.setIsEnabled(true);
            quota.setAlertThreshold(new BigDecimal("80.00"));

            // 设置重置时间
            if ("daily".equals(quotaType)) {
                quota.setResetAt(LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            } else if ("monthly".equals(quotaType)) {
                quota.setResetAt(LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            }
        }

        quota.setQuotaAmount(amount);

        if (quota.getId() == null) {
            userQuotaMapper.insert(quota);
        } else {
            userQuotaMapper.updateById(quota);
        }

        log.info("设置用户配额: userId={}, quotaType={}, amount={}", userId, quotaType, amount);
    }

    /**
     * 重置配额（定时任务调用）
     */
    @Transactional
    public int resetQuotas() {
        List<UserQuota> quotasNeedReset = userQuotaMapper.selectQuotasNeedReset();
        int resetCount = 0;

        for (UserQuota quota : quotasNeedReset) {
            LocalDateTime nextResetAt;
            if ("daily".equals(quota.getQuotaType())) {
                nextResetAt = quota.getResetAt().plusDays(1);
            } else if ("monthly".equals(quota.getQuotaType())) {
                nextResetAt = quota.getResetAt().plusMonths(1);
            } else {
                continue;
            }

            userQuotaMapper.resetQuota(quota.getId(), nextResetAt);
            resetCount++;

            log.info("重置配额: userId={}, quotaType={}", quota.getUserId(), quota.getQuotaType());
        }

        if (resetCount > 0) {
            log.info("配额重置完成: count={}", resetCount);
        }

        return resetCount;
    }
}
