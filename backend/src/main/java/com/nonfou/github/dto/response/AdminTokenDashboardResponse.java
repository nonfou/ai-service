package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理端 Token 仪表盘响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTokenDashboardResponse {

    private Integer days;

    private List<TokenStatsSummaryCard> summaryCards;

    private List<TokenTrendItem> trend;

    private List<TokenDistributionItem> modelDistribution;

    private List<TokenDistributionItem> endpointDistribution;

    private List<TokenUsageRecordItem> recentRequests;
}
