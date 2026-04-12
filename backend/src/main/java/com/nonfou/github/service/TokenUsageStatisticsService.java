package com.nonfou.github.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nonfou.github.dto.request.AdminTokenUsageQueryRequest;
import com.nonfou.github.dto.response.*;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.TokenUsageRecord;
import com.nonfou.github.enums.ApiEndpoint;
import com.nonfou.github.exception.BusinessException;
import com.nonfou.github.mapper.TokenUsageRecordMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Token 使用统计服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageStatisticsService {

    private static final Set<Integer> SUPPORTED_DAYS = Set.of(7, 30);
    private static final DateTimeFormatter CSV_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER = String.join(",",
            "createdAt",
            "apiKeyName",
            "model",
            "endpoint",
            "requestType",
            "success",
            "inputTokens",
            "outputTokens",
            "cacheReadTokens",
            "cacheWriteTokens",
            "totalTokens",
            "firstTokenLatencyMs",
            "durationMs",
            "userAgent",
            "errorMessage");
    private static final List<ApiEndpoint> TRACKED_ENDPOINTS = List.of(
            ApiEndpoint.CHAT_COMPLETIONS,
            ApiEndpoint.MESSAGES,
            ApiEndpoint.RESPONSES,
            ApiEndpoint.EMBEDDINGS
    );

    private final TokenUsageRecordMapper tokenUsageRecordMapper;
    private final ApiKeyService apiKeyService;

    public void record(TokenUsageRecordCommand command) {
        if (command == null || command.getApiKey() == null || !StringUtils.hasText(command.getEndpoint())) {
            return;
        }

        CostCalculatorService.TokenUsage tokenUsage = command.getTokenUsage();
        int inputTokens = tokenUsage != null ? Math.max(tokenUsage.getInputTokens(), 0) : 0;
        int outputTokens = tokenUsage != null ? Math.max(tokenUsage.getOutputTokens(), 0) : 0;
        int cacheReadTokens = tokenUsage != null ? Math.max(tokenUsage.getCacheReadTokens(), 0) : 0;
        int cacheWriteTokens = tokenUsage != null ? Math.max(tokenUsage.getCacheWriteTokens(), 0) : 0;

        TokenUsageRecord entity = new TokenUsageRecord();
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setApiKeyId(command.getApiKey().getId());
        entity.setApiKeyNameSnapshot(StringUtils.hasText(command.getApiKey().getKeyName()) ? command.getApiKey().getKeyName() : "-");
        entity.setEndpoint(command.getEndpoint());
        entity.setModel(StringUtils.hasText(command.getModel()) ? command.getModel().trim() : "-");
        entity.setRequestType(StringUtils.hasText(command.getRequestType()) ? command.getRequestType() : "non_stream");
        entity.setSuccess(command.isSuccess());
        entity.setInputTokens(inputTokens);
        entity.setOutputTokens(outputTokens);
        entity.setCacheReadTokens(cacheReadTokens);
        entity.setCacheWriteTokens(cacheWriteTokens);
        entity.setTotalTokens(inputTokens + outputTokens + cacheReadTokens + cacheWriteTokens);
        entity.setFirstTokenLatencyMs(normalizeLong(command.getFirstTokenLatencyMs()));
        entity.setDurationMs(normalizeLong(command.getDurationMs()));
        entity.setErrorMessage(normalizeOptionalText(command.getErrorMessage()));
        entity.setUserAgent(normalizeOptionalText(command.getUserAgent()));
        entity.setCreatedAt(LocalDateTime.now());
        tokenUsageRecordMapper.insert(entity);
    }

    public AdminTokenDashboardResponse getDashboard(int days) {
        int normalizedDays = normalizeDays(days);
        LocalDateTime startTime = startTimeOfDays(normalizedDays);
        TokenUsageSummary summary = querySummary(startTime, null);

        List<TokenStatsSummaryCard> cards = List.of(
                TokenStatsSummaryCard.builder()
                        .key("requestCount")
                        .label("总请求数")
                        .value(summary.getRequestCount())
                        .detail("近 " + normalizedDays + " 天")
                        .build(),
                TokenStatsSummaryCard.builder()
                        .key("totalTokens")
                        .label("总 Token")
                        .value(summary.getTotalTokens())
                        .detail("输入/输出/缓存合计")
                        .build(),
                TokenStatsSummaryCard.builder()
                        .key("inputTokens")
                        .label("输入 Token")
                        .value(summary.getInputTokens())
                        .detail("最近窗口内输入")
                        .build(),
                TokenStatsSummaryCard.builder()
                        .key("outputTokens")
                        .label("输出 Token")
                        .value(summary.getOutputTokens())
                        .detail("最近窗口内输出")
                        .build(),
                TokenStatsSummaryCard.builder()
                        .key("averageDurationMs")
                        .label("平均耗时")
                        .value(summary.getAverageDurationMs())
                        .unit("ms")
                        .detail("所有请求平均总耗时")
                        .build(),
                TokenStatsSummaryCard.builder()
                        .key("failureCount")
                        .label("失败请求数")
                        .value(summary.getFailureCount())
                        .detail("最近窗口内失败次数")
                        .build()
        );

        return AdminTokenDashboardResponse.builder()
                .days(normalizedDays)
                .summaryCards(cards)
                .trend(queryTrend(startTime))
                .modelDistribution(queryDistribution(startTime, "model"))
                .endpointDistribution(queryDistribution(startTime, "endpoint"))
                .recentRequests(queryRecentRecords(startTime, 10))
                .build();
    }

    public AdminTokenUsageRecordPageResponse getUsageRecords(AdminTokenUsageQueryRequest request) {
        AdminTokenUsageQueryRequest normalized = normalizeQuery(request);
        LocalDateTime startTime = startTimeOfDays(normalized.getDays());

        QueryWrapper<TokenUsageRecord> pageQuery = buildFilterQuery(startTime, normalized);
        long total = tokenUsageRecordMapper.selectCount(pageQuery);

        int offset = (normalized.getPage() - 1) * normalized.getPageSize();
        pageQuery.orderByDesc("created_at")
                .last("LIMIT " + normalized.getPageSize() + " OFFSET " + offset);

        List<TokenUsageRecordItem> records = tokenUsageRecordMapper.selectList(pageQuery).stream()
                .map(this::toRecordItem)
                .toList();

        long totalPages = total == 0 ? 0 : (total + normalized.getPageSize() - 1) / normalized.getPageSize();

        return AdminTokenUsageRecordPageResponse.builder()
                .summary(querySummary(startTime, normalized))
                .filters(buildFilterOptions(normalized.getDays()))
                .records(records)
                .pagination(PageMeta.builder()
                        .page(normalized.getPage())
                        .pageSize(normalized.getPageSize())
                        .total(total)
                        .totalPages((int) totalPages)
                        .build())
                .build();
    }

    public byte[] exportUsageRecords(AdminTokenUsageQueryRequest request) {
        AdminTokenUsageQueryRequest normalized = normalizeQuery(request);
        LocalDateTime startTime = startTimeOfDays(normalized.getDays());

        QueryWrapper<TokenUsageRecord> queryWrapper = buildFilterQuery(startTime, normalized);
        queryWrapper.orderByDesc("created_at");

        StringBuilder csvBuilder = new StringBuilder(CSV_HEADER).append("\r\n");
        tokenUsageRecordMapper.selectList(queryWrapper).stream()
                .map(this::toRecordItem)
                .forEach(item -> csvBuilder.append(toCsvLine(item)).append("\r\n"));
        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private TokenUsageSummary querySummary(LocalDateTime startTime, AdminTokenUsageQueryRequest request) {
        QueryWrapper<TokenUsageRecord> queryWrapper = buildFilterQuery(startTime, request);
        queryWrapper.select(
                "COUNT(1) AS requestCount",
                "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS successCount",
                "SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) AS failureCount",
                "COALESCE(SUM(input_tokens), 0) AS inputTokens",
                "COALESCE(SUM(output_tokens), 0) AS outputTokens",
                "COALESCE(SUM(cache_read_tokens), 0) AS cacheReadTokens",
                "COALESCE(SUM(cache_write_tokens), 0) AS cacheWriteTokens",
                "COALESCE(SUM(total_tokens), 0) AS totalTokens",
                "COALESCE(AVG(duration_ms), 0) AS averageDurationMs"
        );

        List<Map<String, Object>> rows = tokenUsageRecordMapper.selectMaps(queryWrapper);
        Map<String, Object> row = CollectionUtils.isEmpty(rows) ? Map.of() : rows.get(0);

        return TokenUsageSummary.builder()
                .requestCount(readLong(row, "requestCount"))
                .successCount(readLong(row, "successCount"))
                .failureCount(readLong(row, "failureCount"))
                .inputTokens(readLong(row, "inputTokens"))
                .outputTokens(readLong(row, "outputTokens"))
                .cacheReadTokens(readLong(row, "cacheReadTokens"))
                .cacheWriteTokens(readLong(row, "cacheWriteTokens"))
                .totalTokens(readLong(row, "totalTokens"))
                .averageDurationMs(readLong(row, "averageDurationMs"))
                .build();
    }

    private List<TokenTrendItem> queryTrend(LocalDateTime startTime) {
        QueryWrapper<TokenUsageRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("created_at", startTime)
                .select(
                        "DATE(created_at) AS statDate",
                        "COUNT(1) AS requestCount",
                        "COALESCE(SUM(input_tokens), 0) AS inputTokens",
                        "COALESCE(SUM(output_tokens), 0) AS outputTokens",
                        "COALESCE(SUM(cache_read_tokens), 0) AS cacheReadTokens",
                        "COALESCE(SUM(cache_write_tokens), 0) AS cacheWriteTokens",
                        "COALESCE(SUM(total_tokens), 0) AS totalTokens"
                )
                .groupBy("DATE(created_at)");

        List<Map<String, Object>> rows = tokenUsageRecordMapper.selectMaps(queryWrapper);
        Map<String, Map<String, Object>> rowMap = rows.stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get("statDate")), item -> item));

        List<TokenTrendItem> trend = new ArrayList<>();
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = LocalDate.now();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Map<String, Object> row = rowMap.get(date.toString());
            trend.add(TokenTrendItem.builder()
                    .date(date.toString())
                    .requestCount(readLong(row, "requestCount"))
                    .inputTokens(readLong(row, "inputTokens"))
                    .outputTokens(readLong(row, "outputTokens"))
                    .cacheReadTokens(readLong(row, "cacheReadTokens"))
                    .cacheWriteTokens(readLong(row, "cacheWriteTokens"))
                    .totalTokens(readLong(row, "totalTokens"))
                    .build());
        }
        return trend;
    }

    private List<TokenDistributionItem> queryDistribution(LocalDateTime startTime, String groupField) {
        QueryWrapper<TokenUsageRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("created_at", startTime)
                .select(
                        "COALESCE(" + groupField + ", '-') AS groupName",
                        "COUNT(1) AS requestCount",
                        "COALESCE(SUM(total_tokens), 0) AS totalTokens"
                )
                .groupBy(groupField);

        return tokenUsageRecordMapper.selectMaps(queryWrapper).stream()
                .map(item -> TokenDistributionItem.builder()
                        .name(String.valueOf(item.get("groupName")))
                        .requestCount(readLong(item, "requestCount"))
                        .totalTokens(readLong(item, "totalTokens"))
                        .build())
                .sorted(Comparator.comparing(TokenDistributionItem::getTotalTokens, Comparator.nullsFirst(Long::compareTo)).reversed())
                .toList();
    }

    private List<TokenUsageRecordItem> queryRecentRecords(LocalDateTime startTime, int limit) {
        QueryWrapper<TokenUsageRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("created_at", startTime)
                .orderByDesc("created_at")
                .last("LIMIT " + limit);
        return tokenUsageRecordMapper.selectList(queryWrapper).stream()
                .map(this::toRecordItem)
                .toList();
    }

    private TokenStatsFilterOptions buildFilterOptions(int days) {
        List<TokenFilterOption> apiKeys = apiKeyService.listAdminApiKeys().stream()
                .map(item -> TokenFilterOption.builder()
                        .label(item.getKeyName())
                        .value(item.getId())
                        .build())
                .toList();

        List<TokenFilterOption> endpoints = TRACKED_ENDPOINTS.stream()
                .map(endpoint -> TokenFilterOption.builder()
                        .label(endpoint.getPath())
                        .value(endpoint.getPath())
                        .build())
                .toList();

        return TokenStatsFilterOptions.builder()
                .days(days)
                .apiKeys(apiKeys)
                .endpoints(endpoints)
                .build();
    }

    private QueryWrapper<TokenUsageRecord> buildFilterQuery(LocalDateTime startTime, AdminTokenUsageQueryRequest request) {
        QueryWrapper<TokenUsageRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("created_at", startTime);
        if (request == null) {
            return queryWrapper;
        }
        if (request.getApiKeyId() != null) {
            queryWrapper.eq("api_key_id", request.getApiKeyId());
        }
        if (StringUtils.hasText(request.getModel())) {
            queryWrapper.like("model", request.getModel().trim());
        }
        if (StringUtils.hasText(request.getEndpoint())) {
            queryWrapper.eq("endpoint", request.getEndpoint().trim());
        }
        if (request.getSuccess() != null) {
            queryWrapper.eq("success", request.getSuccess());
        }
        return queryWrapper;
    }

    private AdminTokenUsageQueryRequest normalizeQuery(AdminTokenUsageQueryRequest request) {
        AdminTokenUsageQueryRequest normalized = request != null ? request : new AdminTokenUsageQueryRequest();
        normalized.setDays(normalizeDays(normalized.getDays()));
        normalized.setPage(normalized.getPage() == null || normalized.getPage() < 1 ? 1 : normalized.getPage());
        normalized.setPageSize(normalized.getPageSize() == null || normalized.getPageSize() < 1 ? 20 : Math.min(normalized.getPageSize(), 100));
        normalized.setModel(normalizeOptionalText(normalized.getModel()));
        normalized.setEndpoint(normalizeOptionalText(normalized.getEndpoint()));
        return normalized;
    }

    private int normalizeDays(Integer days) {
        int normalized = days == null ? 7 : days;
        if (!SUPPORTED_DAYS.contains(normalized)) {
            throw new BusinessException("仅支持近 7 天或近 30 天统计");
        }
        return normalized;
    }

    private LocalDateTime startTimeOfDays(int days) {
        return LocalDate.now().minusDays(days - 1L).atStartOfDay();
    }

    private TokenUsageRecordItem toRecordItem(TokenUsageRecord entity) {
        return TokenUsageRecordItem.builder()
                .id(entity.getId())
                .apiKeyId(entity.getApiKeyId())
                .apiKeyName(entity.getApiKeyNameSnapshot())
                .model(entity.getModel())
                .endpoint(entity.getEndpoint())
                .requestType(entity.getRequestType())
                .success(entity.getSuccess())
                .inputTokens(entity.getInputTokens())
                .outputTokens(entity.getOutputTokens())
                .cacheReadTokens(entity.getCacheReadTokens())
                .cacheWriteTokens(entity.getCacheWriteTokens())
                .totalTokens(entity.getTotalTokens())
                .firstTokenLatencyMs(entity.getFirstTokenLatencyMs())
                .durationMs(entity.getDurationMs())
                .errorMessage(entity.getErrorMessage())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Long readLong(Map<String, Object> row, String key) {
        if (row == null || !row.containsKey(key) || row.get(key) == null) {
            return 0L;
        }
        Object value = row.get(key);
        if (value instanceof Number number) {
            return Math.round(number.doubleValue());
        }
        try {
            return Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Long normalizeLong(Long value) {
        if (value == null || value < 0) {
            return null;
        }
        return value;
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toCsvLine(TokenUsageRecordItem item) {
        return String.join(",",
                csvCell(formatDateTime(item.getCreatedAt())),
                csvCell(item.getApiKeyName()),
                csvCell(item.getModel()),
                csvCell(item.getEndpoint()),
                csvCell(item.getRequestType()),
                csvCell(item.getSuccess()),
                csvCell(item.getInputTokens()),
                csvCell(item.getOutputTokens()),
                csvCell(item.getCacheReadTokens()),
                csvCell(item.getCacheWriteTokens()),
                csvCell(item.getTotalTokens()),
                csvCell(item.getFirstTokenLatencyMs()),
                csvCell(item.getDurationMs()),
                csvCell(item.getUserAgent()),
                csvCell(item.getErrorMessage()));
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(CSV_DATE_TIME_FORMATTER) : "";
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsageRecordCommand {

        private ApiKey apiKey;

        private String endpoint;

        private String model;

        private String requestType;

        private boolean success;

        private CostCalculatorService.TokenUsage tokenUsage;

        private Long firstTokenLatencyMs;

        private Long durationMs;

        private String errorMessage;

        private String userAgent;
    }
}
