package com.nonfou.github.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 保留 TokenUsage 等数据结构，兼容现有流式代理代码。
 * 核心 Copilot 转发链路当前不再使用历史计费服务实现。
 */
public final class CostCalculatorService {

    private CostCalculatorService() {
    }

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
                    if (value instanceof Number number) {
                        return number.intValue();
                    }
                }
            }
            return 0;
        }
    }

    @Data
    @Builder
    public static class CostResult {
        private String modelName;
        private int inputTokens;
        private int outputTokens;
        private int cacheReadTokens;
        private int cacheWriteTokens;
        private BigDecimal rawCost;
        private BigDecimal markupRate;
        private BigDecimal markupCost;
        private BigDecimal totalCost;
        private Map<String, BigDecimal> costBreakdown;
    }
}
