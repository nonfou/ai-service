package com.nonfou.github.enums;

import java.util.Locale;

/**
 * 工单状态枚举,负责统一不同数据源中的状态值.
 */
public enum TicketStatus {

    PENDING("pending"),
    PROCESSING("processing"),
    RESOLVED("resolved"),
    CLOSED("closed");

    private final String value;

    TicketStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 将数据库中的状态值转换为标准值.
     */
    public static String normalize(String rawStatus) {
        if (rawStatus == null || rawStatus.isEmpty()) {
            return PENDING.value;
        }
        String status = rawStatus.toLowerCase(Locale.ROOT);
        return switch (status) {
            case "open" -> PENDING.value;
            case "in_progress", "in-progress" -> PROCESSING.value;
            case "resolved" -> RESOLVED.value;
            case "closed" -> CLOSED.value;
            case "pending" -> PENDING.value;
            case "processing" -> PROCESSING.value;
            default -> status;
        };
    }

    /**
     * 校验状态是否合法,不合法则抛出异常.
     */
    public static String validateOrThrow(String status) {
        String normalized = normalize(status);
        for (TicketStatus ticketStatus : values()) {
            if (ticketStatus.value.equals(normalized)) {
                return normalized;
            }
        }
        throw new IllegalArgumentException("不支持的工单状态: " + status);
    }
}
