package com.nonfou.github.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.config.UsageMetricsProperties;
import com.nonfou.github.entity.ApiCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Token usage aggregation service inspired by claude-relay-service: stores API key
 * totals in Redis so that rate-limit, quota, billing and dashboards stay consistent.
 */
@Slf4j
@Service
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class UsageMetricsService {

    private final StringRedisTemplate redisTemplate;
    private final UsageMetricsProperties properties;
    private final ObjectMapper objectMapper;

    public void recordUsage(Long apiKeyId, ApiCall apiCall) {
        if (!properties.isEnableAggregation() || apiKeyId == null || apiCall == null) {
            return;
        }

        try {
            UsageWindow window = UsageWindow.from(apiCall.getRequestTime(), properties.getZoneId());

            long inputTokens = safe(apiCall.getInputTokens());
            long outputTokens = safe(apiCall.getOutputTokens());
            long cacheReadTokens = safe(apiCall.getCacheReadTokens());
            long cacheWriteTokens = safe(apiCall.getCacheWriteTokens());
            long coreTokens = inputTokens + outputTokens;
            long allTokens = coreTokens + cacheReadTokens + cacheWriteTokens;

            String apiKeyStr = String.valueOf(apiKeyId);

            updateTotalStats(apiKeyStr, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);
            updateWindowStats(apiKeyStr, window, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);
            updateModelStats(apiKeyStr, apiCall.getModel(), window, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);
            updateSystemMetrics(window, allTokens, coreTokens);

            recordCost(apiKeyStr, window, apiCall.getCost());
            appendUsageRecord(apiKeyStr, apiCall, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, allTokens);
            publishBillingEvent(apiKeyId, apiCall, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, allTokens, window);
        } catch (Exception ex) {
            log.warn("Failed to record token metrics: {}", ex.getMessage());
        }
    }

    private void updateTotalStats(String apiKeyId,
                                  long inputTokens,
                                  long outputTokens,
                                  long cacheReadTokens,
                                  long cacheWriteTokens,
                                  long coreTokens,
                                  long allTokens) {
        String totalKey = key("usage", "api_key", apiKeyId, "total");
        incrementHash(totalKey, "totalInputTokens", inputTokens);
        incrementHash(totalKey, "totalOutputTokens", outputTokens);
        incrementHash(totalKey, "totalCacheReadTokens", cacheReadTokens);
        incrementHash(totalKey, "totalCacheWriteTokens", cacheWriteTokens);
        incrementHash(totalKey, "totalTokens", coreTokens);
        incrementHash(totalKey, "totalAllTokens", allTokens);
        incrementHash(totalKey, "totalRequests", 1);
    }

    private void updateWindowStats(String apiKeyId,
                                   UsageWindow window,
                                   long inputTokens,
                                   long outputTokens,
                                   long cacheReadTokens,
                                   long cacheWriteTokens,
                                   long coreTokens,
                                   long allTokens) {
        String dailyKey = key("usage", "api_key", apiKeyId, "daily", window.date());
        String monthlyKey = key("usage", "api_key", apiKeyId, "monthly", window.month());
        String hourlyKey = key("usage", "api_key", apiKeyId, "hourly", window.hourKey());

        incrementUsageBucket(dailyKey, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);
        incrementUsageBucket(monthlyKey, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);
        incrementUsageBucket(hourlyKey, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);

        expireDays(dailyKey, properties.getDailyTtlDays());
        expireDays(monthlyKey, properties.getMonthlyTtlDays());
        expireDays(hourlyKey, properties.getHourlyTtlDays());
    }

    private void updateModelStats(String apiKeyId,
                                  String model,
                                  UsageWindow window,
                                  long inputTokens,
                                  long outputTokens,
                                  long cacheReadTokens,
                                  long cacheWriteTokens,
                                  long coreTokens,
                                  long allTokens) {
        String normalizedModel = normalizeModelName(model);
        String dailyKey = key("usage", "api_key", apiKeyId, "model", "daily", normalizedModel, window.date());
        String monthlyKey = key("usage", "api_key", apiKeyId, "model", "monthly", normalizedModel, window.month());

        incrementUsageBucket(dailyKey, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);
        incrementUsageBucket(monthlyKey, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, coreTokens, allTokens);

        expireDays(dailyKey, properties.getDailyTtlDays());
        expireDays(monthlyKey, properties.getMonthlyTtlDays());
    }

    private void updateSystemMetrics(UsageWindow window, long allTokens, long coreTokens) {
        if (properties.getMetricsWindowMinutes() <= 0) {
            return;
        }
        String systemKey = key("system", "metrics", "minute", String.valueOf(window.minuteBucket()));
        incrementHash(systemKey, "requests", 1);
        incrementHash(systemKey, "allTokens", allTokens);
        incrementHash(systemKey, "coreTokens", coreTokens);
        redisTemplate.expire(systemKey, Duration.ofMinutes(properties.getMetricsWindowMinutes() * 2L));
    }

    private void recordCost(String apiKeyId, UsageWindow window, BigDecimal cost) {
        if (cost == null || cost.signum() == 0) {
            return;
        }
        double amount = cost.doubleValue();
        String dailyKey = key("usage", "cost", "daily", apiKeyId, window.date());
        String monthlyKey = key("usage", "cost", "monthly", apiKeyId, window.month());
        String totalKey = key("usage", "cost", "total", apiKeyId);

        redisTemplate.opsForValue().increment(dailyKey, amount);
        redisTemplate.opsForValue().increment(monthlyKey, amount);
        redisTemplate.opsForValue().increment(totalKey, amount);

        expireDays(dailyKey, properties.getDailyTtlDays());
        expireDays(monthlyKey, properties.getMonthlyTtlDays());
    }

    private void appendUsageRecord(String apiKeyId,
                                   ApiCall apiCall,
                                   long inputTokens,
                                   long outputTokens,
                                   long cacheReadTokens,
                                   long cacheWriteTokens,
                                   long allTokens) throws JsonProcessingException {
        if (properties.getUsageRecordLimit() <= 0) {
            return;
        }

        Map<String, Object> record = new HashMap<>();
        record.put("timestamp", apiCall.getRequestTime() != null ? apiCall.getRequestTime().toString() : Instant.now().toString());
        record.put("model", apiCall.getModel());
        record.put("provider", apiCall.getProvider());
        record.put("inputTokens", inputTokens);
        record.put("outputTokens", outputTokens);
        record.put("cacheReadTokens", cacheReadTokens);
        record.put("cacheWriteTokens", cacheWriteTokens);
        record.put("allTokens", allTokens);
        record.put("cost", apiCall.getCost() != null ? apiCall.getCost() : BigDecimal.ZERO);
        record.put("status", apiCall.getStatus());
        record.put("error", apiCall.getErrorMsg());

        String recordKey = key("usage", "api_key", apiKeyId, "records");
        redisTemplate.opsForList().leftPush(recordKey, objectMapper.writeValueAsString(record));
        redisTemplate.opsForList().trim(recordKey, 0, properties.getUsageRecordLimit() - 1L);
        expireDays(recordKey, properties.getMonthlyTtlDays());
    }

    private void publishBillingEvent(Long apiKeyId,
                                     ApiCall apiCall,
                                     long inputTokens,
                                     long outputTokens,
                                     long cacheReadTokens,
                                     long cacheWriteTokens,
                                     long allTokens,
                                     UsageWindow window) {
        if (!properties.isEnableBillingEvents() || apiCall.getCost() == null) {
            return;
        }

        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "usage.recorded");
            event.put("timestamp", window.isoTimestamp());
            event.put("apiKeyId", apiKeyId);
            event.put("apiKey", maskApiKey(apiCall.getApiKey()));
            event.put("model", apiCall.getModel());
            event.put("provider", apiCall.getProvider());
            event.put("inputTokens", inputTokens);
            event.put("outputTokens", outputTokens);
            event.put("cacheReadTokens", cacheReadTokens);
            event.put("cacheWriteTokens", cacheWriteTokens);
            event.put("totalTokens", allTokens);
            event.put("cost", apiCall.getCost());
            event.put("status", apiCall.getStatus());
            event.put("userId", apiCall.getUserId());

            redisTemplate.opsForStream().add(properties.getBillingStreamKey(), event);
            if (properties.getBillingStreamMaxLength() > 0) {
                redisTemplate.opsForStream().trim(properties.getBillingStreamKey(), properties.getBillingStreamMaxLength());
            }
        } catch (Exception e) {
            log.debug("Failed to publish billing event: {}", e.getMessage());
        }
    }

    private void incrementUsageBucket(String key,
                                      long inputTokens,
                                      long outputTokens,
                                      long cacheReadTokens,
                                      long cacheWriteTokens,
                                      long coreTokens,
                                      long allTokens) {
        incrementHash(key, "inputTokens", inputTokens);
        incrementHash(key, "outputTokens", outputTokens);
        incrementHash(key, "cacheReadTokens", cacheReadTokens);
        incrementHash(key, "cacheWriteTokens", cacheWriteTokens);
        incrementHash(key, "tokens", coreTokens);
        incrementHash(key, "allTokens", allTokens);
        incrementHash(key, "requests", 1);
    }

    private void incrementHash(String key, String field, long delta) {
        if (delta == 0) {
            return;
        }
        redisTemplate.opsForHash().increment(key, field, delta);
    }

    private void expireDays(String key, int days) {
        if (days <= 0) {
            return;
        }
        redisTemplate.expire(key, Duration.ofDays(days));
    }

    private long safe(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private String key(String... parts) {
        return String.join(":", parts);
    }

    private String normalizeModelName(String model) {
        if (model == null || model.isBlank()) {
            return "unknown";
        }
        return model.toLowerCase(Locale.ROOT).replace(":", "_").replace(" ", "_");
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return apiKey;
        }
        return apiKey.substring(0, 6) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private record UsageWindow(String date, String month, String hourKey, long minuteBucket, String isoTimestamp) {
        static UsageWindow from(LocalDateTime reference, ZoneId zoneId) {
            LocalDateTime base = reference != null ? reference : LocalDateTime.now();
            ZonedDateTime zoned = base.atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId);
            LocalDate date = zoned.toLocalDate();
            String dateStr = date.toString();
            String monthStr = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            String hourKey = dateStr + ":" + String.format("%02d", zoned.getHour());
            long minuteBucket = zoned.toEpochSecond() / 60;
            return new UsageWindow(dateStr, monthStr, hourKey, minuteBucket, zoned.toLocalDateTime().toString());
        }
    }
}
