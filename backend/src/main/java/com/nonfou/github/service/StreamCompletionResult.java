package com.nonfou.github.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式请求完成结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamCompletionResult {

    private CostCalculatorService.TokenUsage tokenUsage;

    private boolean success;

    private String errorMessage;

    private Long firstTokenLatencyMs;

    private Long durationMs;
}
