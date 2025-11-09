package com.nonfou.github.service;

import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.entity.Model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 成本计算服务
 * 负责计算 API 调用的成本，支持多种加成倍率
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostCalculatorService {

    private final ModelService modelService;
    private final BackendAccountService backendAccountService;

    @Value("${backend.pricing.markup-rate:1.2}")
    private BigDecimal globalMarkupRate;

    /**
     * 计算成本
     *
     * @param usage            Token 用量
     * @param modelName        模型名称
     * @param backendAccountId 后端账户ID
     * @param userId           用户ID（用于用户级加成倍率，可选）
     * @return 成本计算结果
     */
    public CostResult calculate(TokenUsage usage, String modelName, Long backendAccountId, Long userId) {
        // 1. 获取模型定价
        Model model = modelService.getModelByName(modelName);
        if (model == null) {
            log.warn("模型不存在，使用默认定价: model={}", modelName);
            model = createDefaultModel(modelName);
        }

        // 2. 获取账户信息
        BackendAccount account = null;
        if (backendAccountId != null) {
            account = backendAccountService.getAccountById(backendAccountId);
        }

        // 3. 计算原始成本
        BigDecimal rawCost = calculateRawCost(usage, model);

        // 4. 计算加成倍率
        BigDecimal totalMarkupRate = calculateTotalMarkupRate(model, account);

        // 5. 计算加成金额和总成本
        BigDecimal markupCost = rawCost.multiply(totalMarkupRate.subtract(BigDecimal.ONE))
                .setScale(10, RoundingMode.HALF_UP);
        BigDecimal totalCost = rawCost.add(markupCost);

        log.debug("成本计算: model={}, rawCost={}, markupRate={}, totalCost={}",
                modelName, rawCost, totalMarkupRate, totalCost);

        return CostResult.builder()
                .modelName(modelName)
                .inputTokens(usage.getInputTokens())
                .outputTokens(usage.getOutputTokens())
                .cacheReadTokens(usage.getCacheReadTokens())
                .cacheWriteTokens(usage.getCacheWriteTokens())
                .rawCost(rawCost)
                .markupRate(totalMarkupRate)
                .markupCost(markupCost)
                .totalCost(totalCost)
                .costBreakdown(buildCostBreakdown(usage, model))
                .build();
    }

    /**
     * 计算原始成本
     */
    private BigDecimal calculateRawCost(TokenUsage usage, Model model) {
        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;
        BigDecimal cacheReadCost = BigDecimal.ZERO;
        BigDecimal cacheWriteCost = BigDecimal.ZERO;

        // 输入 token 成本
        if (usage.getInputTokens() > 0 && model.getInputTokenPrice() != null) {
            inputCost = BigDecimal.valueOf(usage.getInputTokens())
                    .multiply(model.getInputTokenPrice())
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
        }

        // 输出 token 成本
        if (usage.getOutputTokens() > 0 && model.getOutputTokenPrice() != null) {
            outputCost = BigDecimal.valueOf(usage.getOutputTokens())
                    .multiply(model.getOutputTokenPrice())
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
        }

        // 缓存读取成本（通常为输入成本的 10%）
        if (usage.getCacheReadTokens() > 0 && model.getCacheReadTokenPrice() != null) {
            cacheReadCost = BigDecimal.valueOf(usage.getCacheReadTokens())
                    .multiply(model.getCacheReadTokenPrice())
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
        }

        // 缓存写入成本（通常为输入成本的 125%）
        if (usage.getCacheWriteTokens() > 0 && model.getCacheWriteTokenPrice() != null) {
            cacheWriteCost = BigDecimal.valueOf(usage.getCacheWriteTokens())
                    .multiply(model.getCacheWriteTokenPrice())
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
        }

        return inputCost.add(outputCost).add(cacheReadCost).add(cacheWriteCost);
    }

    /**
     * 计算总加成倍率
     * 总倍率 = 全局倍率 × 模型倍率 × 账户倍率
     */
    private BigDecimal calculateTotalMarkupRate(Model model, BackendAccount account) {
        BigDecimal totalRate = globalMarkupRate;

        // 应用模型倍率
        if (model.getPriceMultiplier() != null && model.getPriceMultiplier().compareTo(BigDecimal.ZERO) > 0) {
            totalRate = totalRate.multiply(model.getPriceMultiplier());
        }

        // 应用账户倍率
        if (account != null && account.getCostMultiplier() != null && account.getCostMultiplier().compareTo(BigDecimal.ZERO) > 0) {
            totalRate = totalRate.multiply(account.getCostMultiplier());
        }

        return totalRate;
    }

    /**
     * 构建成本明细
     */
    private Map<String, BigDecimal> buildCostBreakdown(TokenUsage usage, Model model) {
        Map<String, BigDecimal> breakdown = new HashMap<>();

        if (usage.getInputTokens() > 0) {
            BigDecimal inputCost = BigDecimal.valueOf(usage.getInputTokens())
                    .multiply(model.getInputTokenPrice() != null ? model.getInputTokenPrice() : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            breakdown.put("inputCost", inputCost);
        }

        if (usage.getOutputTokens() > 0) {
            BigDecimal outputCost = BigDecimal.valueOf(usage.getOutputTokens())
                    .multiply(model.getOutputTokenPrice() != null ? model.getOutputTokenPrice() : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            breakdown.put("outputCost", outputCost);
        }

        if (usage.getCacheReadTokens() > 0) {
            BigDecimal cacheReadCost = BigDecimal.valueOf(usage.getCacheReadTokens())
                    .multiply(model.getCacheReadTokenPrice() != null ? model.getCacheReadTokenPrice() : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            breakdown.put("cacheReadCost", cacheReadCost);
        }

        if (usage.getCacheWriteTokens() > 0) {
            BigDecimal cacheWriteCost = BigDecimal.valueOf(usage.getCacheWriteTokens())
                    .multiply(model.getCacheWriteTokenPrice() != null ? model.getCacheWriteTokenPrice() : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            breakdown.put("cacheWriteCost", cacheWriteCost);
        }

        return breakdown;
    }

    /**
     * 创建默认模型（当模型不存在时）
     */
    private Model createDefaultModel(String modelName) {
        Model model = new Model();
        model.setModelName(modelName);
        model.setInputTokenPrice(BigDecimal.ZERO);
        model.setOutputTokenPrice(BigDecimal.ZERO);
        model.setCacheReadTokenPrice(BigDecimal.ZERO);
        model.setCacheWriteTokenPrice(BigDecimal.ZERO);
        model.setPriceMultiplier(BigDecimal.ONE);
        return model;
    }

    /**
     * Token 用量
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private int inputTokens;
        private int outputTokens;
        private int cacheReadTokens;
        private int cacheWriteTokens;

        public static TokenUsage from(Map<String, Object> usage) {
            TokenUsage tokenUsage = new TokenUsage();
            tokenUsage.setInputTokens(getIntValue(usage, "prompt_tokens", "input_tokens"));
            tokenUsage.setOutputTokens(getIntValue(usage, "completion_tokens", "output_tokens"));
            tokenUsage.setCacheReadTokens(getIntValue(usage, "cache_read_input_tokens"));
            tokenUsage.setCacheWriteTokens(getIntValue(usage, "cache_creation_input_tokens"));
            return tokenUsage;
        }

        private static int getIntValue(Map<String, Object> map, String... keys) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                }
            }
            return 0;
        }
    }

    /**
     * 成本计算结果
     */
    @Data
    @lombok.Builder
    public static class CostResult {
        private String modelName;
        private int inputTokens;
        private int outputTokens;
        private int cacheReadTokens;
        private int cacheWriteTokens;
        private BigDecimal rawCost;          // 原始成本
        private BigDecimal markupRate;       // 加成倍率
        private BigDecimal markupCost;       // 加成金额
        private BigDecimal totalCost;        // 总成本（用户实付）
        private Map<String, BigDecimal> costBreakdown;  // 成本明细
    }
}
