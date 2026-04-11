package com.nonfou.github.service;

import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.entity.UserAccountBinding;
import com.nonfou.github.mapper.UserAccountBindingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 账户调度服务
 * 核心功能：根据策略选择最优的后端账户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSchedulerService {

    private final BackendAccountService backendAccountService;
    private final UserAccountBindingMapper userAccountBindingMapper;
    private final SessionStickinessService sessionStickinessService;
    private final ModelService modelService;

    @Value("${backend.scheduler.strategy:HYBRID}")
    private String schedulerStrategy;

    // 轮询索引
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    // 账户使用计数（内存版）
    private final ConcurrentHashMap<Long, AtomicLong> accountUsageCounts = new ConcurrentHashMap<>();

    /**
     * 选择账户的主入口方法
     *
     * @param apiKey      API Key 对象
     * @param model       请求的模型名称
     * @param sessionHash 会话哈希（可选）
     * @return 选中的后端账户
     */
    public BackendAccount selectAccount(ApiKey apiKey, String model, String sessionHash) {
        log.debug("开始选择账户: userId={}, model={}, sessionHash={}", apiKey.getUserId(), model, sessionHash);

        // 1. 检查会话粘性
        if (sessionHash != null) {
            Long accountId = sessionStickinessService.getAccountIdBySession(sessionHash);
            if (accountId != null) {
                BackendAccount account = backendAccountService.getAccountById(accountId);
                if (account != null && account.isHealthy()) {
                    log.debug("使用会话粘性账户: accountId={}, sessionHash={}", accountId, sessionHash);
                    return account;
                }
                log.debug("会话绑定的账户不健康，重新选择: accountId={}", accountId);
            }
        }

        // 2. 确定提供商
        String provider = determineProvider(model);
        log.debug("确定提供商: provider={}, model={}", provider, model);

        // 3. 获取候选账户列表
        List<BackendAccount> candidates = getCandidateAccounts(apiKey.getUserId(), provider);

        if (candidates.isEmpty()) {
            throw new RuntimeException("没有可用的后端账户: provider=" + provider);
        }

        log.debug("候选账户数量: {}", candidates.size());

        // 4. 根据策略选择账户
        BackendAccount selected = selectByStrategy(candidates);

        // 5. 保存会话映射
        if (sessionHash != null && selected != null) {
            sessionStickinessService.saveSessionMapping(
                    sessionHash,
                    selected.getId(),
                    apiKey.getId(),
                    apiKey.getUserId()
            );
        }

        log.info("选择账户成功: accountId={}, accountName={}, provider={}, strategy={}",
                selected.getId(), selected.getAccountName(), selected.getProvider(), schedulerStrategy);

        return selected;
    }

    /**
     * 确定提供商
     * 优先从数据库查询模型配置，如果查不到则根据模型名前缀推断
     */
    private String determineProvider(String model) {
        // 先从数据库查询
        try {
            var modelConfig = modelService.getModelByName(model);
            if (modelConfig != null && modelConfig.getProvider() != null) {
                return modelConfig.getProvider();
            }
        } catch (Exception e) {
            log.warn("查询模型配置失败: model={}", model, e);
        }

        // 降级：根据前缀推断
        String lowerModel = model.toLowerCase();
        if (lowerModel.contains("copilot") || lowerModel.startsWith("github-")) {
            return "copilot";
        }
        // 最小化代理模式下默认走 copilot
        return "copilot";
    }

    /**
     * 获取候选账户列表
     */
    private List<BackendAccount> getCandidateAccounts(Long userId, String provider) {
        // 1. 查询用户绑定的账户
        List<Long> boundAccountIds = userAccountBindingMapper
                .selectAccountIdsByUserIdAndProvider(userId, provider);

        // 2. 如果有绑定，使用绑定的账户
        if (!boundAccountIds.isEmpty()) {
            return boundAccountIds.stream()
                    .map(backendAccountService::getAccountById)
                    .filter(account -> account != null && account.isHealthy())
                    .sorted(Comparator.comparing(BackendAccount::getPriority))
                    .collect(Collectors.toList());
        }

        // 3. 否则使用该提供商下所有激活的账户
        return backendAccountService.getActiveAccountsByProvider(provider).stream()
                .filter(BackendAccount::isHealthy)
                .sorted(Comparator.comparing(BackendAccount::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 根据策略选择账户
     */
    private BackendAccount selectByStrategy(List<BackendAccount> candidates) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        return switch (schedulerStrategy) {
            case "ROUND_ROBIN" -> roundRobinSelect(candidates);
            case "LEAST_USED" -> leastUsedSelect(candidates);
            case "PRIORITY" -> prioritySelect(candidates);
            case "COST_OPTIMIZED" -> costOptimizedSelect(candidates);
            case "HYBRID" -> hybridSelect(candidates);
            default -> prioritySelect(candidates);
        };
    }

    /**
     * 轮询选择
     */
    private BackendAccount roundRobinSelect(List<BackendAccount> candidates) {
        int index = roundRobinIndex.getAndIncrement() % candidates.size();
        return candidates.get(index);
    }

    /**
     * 最少使用选择
     */
    private BackendAccount leastUsedSelect(List<BackendAccount> candidates) {
        return candidates.stream()
                .min(Comparator.comparingLong(this::getUsageCount))
                .orElse(candidates.get(0));
    }

    /**
     * 优先级选择（选择优先级最高的）
     */
    private BackendAccount prioritySelect(List<BackendAccount> candidates) {
        return candidates.get(0); // 已经按优先级排序
    }

    /**
     * 成本优化选择（选择成本倍率最低的）
     */
    private BackendAccount costOptimizedSelect(List<BackendAccount> candidates) {
        return candidates.stream()
                .min(Comparator.comparing(BackendAccount::getCostMultiplier))
                .orElse(candidates.get(0));
    }

    /**
     * 混合选择（综合考虑优先级、负载、健康状态）
     */
    private BackendAccount hybridSelect(List<BackendAccount> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(this::calculateScore))
                .orElse(candidates.get(0));
    }

    /**
     * 计算账户得分（越高越好）
     * 得分 = 优先级分数(50%) + 负载分数(30%) + 健康分数(20%)
     */
    private double calculateScore(BackendAccount account) {
        // 优先级分数：priority 越小，分数越高
        double priorityScore = 100.0 - account.getPriority();

        // 负载分数：使用次数越少，分数越高
        long usageCount = getUsageCount(account);
        double loadScore = Math.max(0, 100.0 - usageCount);

        // 健康分数：errorCount 越少，分数越高
        double healthScore = Math.max(0, 100.0 - account.getErrorCount() * 20);

        // 加权计算总分
        return priorityScore * 0.5 + loadScore * 0.3 + healthScore * 0.2;
    }

    /**
     * 获取账户使用次数（内存版）
     */
    private long getUsageCount(BackendAccount account) {
        AtomicLong counter = accountUsageCounts.get(account.getId());
        return counter != null ? counter.get() : 0;
    }

    /**
     * 记录账户使用（由外部调用）
     */
    public void recordAccountUsage(Long accountId) {
        accountUsageCounts.computeIfAbsent(accountId, k -> new AtomicLong(0)).incrementAndGet();
    }
}
