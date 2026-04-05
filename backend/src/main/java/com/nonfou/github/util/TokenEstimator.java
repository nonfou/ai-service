package com.nonfou.github.util;

import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.service.CostCalculatorService;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * 粗略的 token 估算器，用于流式响应缺少 usage 数据时的兜底统计。
 * 该实现根据字符类型（CJK/非 CJK）估算，用于保证计费和配额统计不会明显偏离。
 */
@Component
public class TokenEstimator {

    private static final double ASCII_CHARS_PER_TOKEN = 4.0;
    private static final double CJK_CHARS_PER_TOKEN = 1.6;
    private static final long MAX_ESTIMATED_TOKENS = 200_000L;

    /**
     * 根据请求与流式字符计数器估算 token 使用情况。
     */
    public CostCalculatorService.TokenUsage estimateUsage(ChatRequest request, StreamingCharCounter charCounter) {
        int inputTokens = 0;
        if (request != null && request.getMessages() != null) {
            inputTokens = request.getMessages().stream()
                    .map(ChatRequest.Message::getContent)
                    .mapToInt(this::estimateContentTokens)
                    .sum();
        }

        int outputTokens = charCounter.estimateTokens();

        return CostCalculatorService.TokenUsage.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheReadTokens(0)
                .cacheWriteTokens(0)
                .build();
    }

    /**
     * 根据请求与模型响应内容估算 token 使用情况。
     */
    public CostCalculatorService.TokenUsage estimateUsage(ChatRequest request, String responseText) {
        int inputTokens = 0;
        if (request != null && request.getMessages() != null) {
            inputTokens = request.getMessages().stream()
                    .map(ChatRequest.Message::getContent)
                    .mapToInt(this::estimateContentTokens)
                    .sum();
        }

        int outputTokens = estimateTextTokens(responseText);

        return CostCalculatorService.TokenUsage.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheReadTokens(0)
                .cacheWriteTokens(0)
                .build();
    }

    /**
     * 估算文本 token 数。
     */
    public int estimateTextTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        long asciiChars = 0;
        long cjkChars = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCjk(ch)) {
                cjkChars++;
            } else if (!Character.isWhitespace(ch)) {
                asciiChars++;
            }
        }

        double estimated = (asciiChars / ASCII_CHARS_PER_TOKEN) + (cjkChars / CJK_CHARS_PER_TOKEN);
        long rounded = Math.min(Math.round(Math.ceil(estimated)), MAX_ESTIMATED_TOKENS);
        return (int) rounded;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    /**
     * 流式字符计数器，增量统计 ASCII/CJK 字符数以估算 token，
     * 避免累积完整响应文本。
     */
    public static class StreamingCharCounter {
        private long asciiChars;
        private long cjkChars;

        public void append(String text) {
            if (text == null) return;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (isCjk(ch)) {
                    cjkChars++;
                } else if (!Character.isWhitespace(ch)) {
                    asciiChars++;
                }
            }
        }

        public int estimateTokens() {
            double estimated = (asciiChars / ASCII_CHARS_PER_TOKEN) + (cjkChars / CJK_CHARS_PER_TOKEN);
            long rounded = Math.min(Math.round(Math.ceil(estimated)), MAX_ESTIMATED_TOKENS);
            return (int) rounded;
        }

        private static boolean isCjk(char ch) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
        }
    }

    /**
     * 估算任意内容结构的 token 数，兼容字符串、分段内容、工具调用等格式。
     */
    private int estimateContentTokens(Object content) {
        if (content == null) {
            return 0;
        }

        if (content instanceof String text) {
            return estimateTextTokens(text);
        }

        if (content instanceof Collection<?> collection) {
            int total = 0;
            for (Object part : collection) {
                total += estimateContentTokens(part);
            }
            return total;
        }

        if (content.getClass().isArray()) {
            int total = 0;
            int length = Array.getLength(content);
            for (int i = 0; i < length; i++) {
                total += estimateContentTokens(Array.get(content, i));
            }
            return total;
        }

        if (content instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if ("text".equals(type)) {
                return estimateContentTokens(map.get("text"));
            }
            if ("image_url".equals(type)) {
                Object image = map.get("image_url");
                if (image instanceof Map<?, ?> imageMap) {
                    Object url = imageMap.get("url");
                    return url instanceof String ? estimateTextTokens((String) url) : 0;
                }
                return image instanceof String ? estimateTextTokens((String) image) : 0;
            }
            if (map.containsKey("content")) {
                return estimateContentTokens(map.get("content"));
            }
            if (map.containsKey("text")) {
                return estimateContentTokens(map.get("text"));
            }
            return estimateTextTokens(String.valueOf(content));
        }

        return estimateTextTokens(String.valueOf(content));
    }
}
