package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按天聚合的趋势数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenTrendItem {

    private String date;

    private Long requestCount;

    private Long inputTokens;

    private Long outputTokens;

    private Long cacheReadTokens;

    private Long cacheWriteTokens;

    private Long totalTokens;
}
