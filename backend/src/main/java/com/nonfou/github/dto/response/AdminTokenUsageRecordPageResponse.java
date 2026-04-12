package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理端 Token 使用记录分页响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTokenUsageRecordPageResponse {

    private TokenUsageSummary summary;

    private TokenStatsFilterOptions filters;

    private List<TokenUsageRecordItem> records;

    private PageMeta pagination;
}
