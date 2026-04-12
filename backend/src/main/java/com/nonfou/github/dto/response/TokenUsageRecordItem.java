package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token 使用明细项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageRecordItem {

    private Long id;

    private Long apiKeyId;

    private String apiKeyName;

    private String model;

    private String endpoint;

    private String requestType;

    private Boolean success;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer cacheReadTokens;

    private Integer cacheWriteTokens;

    private Integer totalTokens;

    private Long firstTokenLatencyMs;

    private Long durationMs;

    private String errorMessage;

    private String userAgent;

    private LocalDateTime createdAt;
}
