package com.nonfou.github.util;

import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.service.CostCalculatorService;
import org.springframework.stereotype.Component;

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
     * 根据请求与模型响应内容估算 token 使用情况。
     */
    public CostCalculatorService.TokenUsage estimateUsage(ChatRequest request, String responseText) {
        int inputTokens = 0;
        if (request != null && request.getMessages() != null) {
            inputTokens = request.getMessages().stream()
                    .map(ChatRequest.Message::getContent)
                    .mapToInt(this::estimateTextTokens)
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
}
