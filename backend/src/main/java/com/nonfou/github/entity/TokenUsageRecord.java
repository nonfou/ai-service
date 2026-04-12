package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token 使用明细记录。
 */
@Data
@TableName("token_usage_records")
public class TokenUsageRecord {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long apiKeyId;

    private String apiKeyNameSnapshot;

    private String endpoint;

    private String model;

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
