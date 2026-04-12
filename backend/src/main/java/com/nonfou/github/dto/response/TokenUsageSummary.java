package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 使用记录汇总信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageSummary {

    private Long requestCount;

    private Long successCount;

    private Long failureCount;

    private Long inputTokens;

    private Long outputTokens;

    private Long cacheReadTokens;

    private Long cacheWriteTokens;

    private Long totalTokens;

    private Long averageDurationMs;
}
