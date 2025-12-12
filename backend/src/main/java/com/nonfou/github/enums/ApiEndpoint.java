package com.nonfou.github.enums;

import java.util.Locale;

/**
 * API 端点枚举
 * 定义所有支持的 API 端点及其协议格式标签
 */
public enum ApiEndpoint {

    // ============================================================
    // OpenAI 兼容端点 - 返回 OpenAI 格式
    // ============================================================

    /**
     * OpenAI Chat Completions API
     */
    CHAT_COMPLETIONS("/v1/chat/completions", ApiProtocol.OPENAI),

    /**
     * OpenAI Models API
     */
    MODELS("/v1/models", ApiProtocol.OPENAI),

    /**
     * OpenAI Embeddings API
     */
    EMBEDDINGS("/v1/embeddings", ApiProtocol.OPENAI),

    /**
     * OpenAI Responses API (Codex 系列)
     */
    RESPONSES("/v1/responses", ApiProtocol.OPENAI_CODEX),

    // ============================================================
    // Anthropic 兼容端点 - 返回 Claude 原生格式
    // ============================================================

    /**
     * Claude Messages API
     */
    MESSAGES("/v1/messages", ApiProtocol.ANTHROPIC),

    /**
     * Claude Messages Count Tokens API
     */
    MESSAGES_COUNT_TOKENS("/v1/messages/count_tokens", ApiProtocol.ANTHROPIC);

    private final String path;
    private final ApiProtocol protocol;

    ApiEndpoint(String path, ApiProtocol protocol) {
        this.path = path;
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    public ApiProtocol getProtocol() {
        return protocol;
    }

    /**
     * 判断是否为 OpenAI 兼容格式（包括 Codex）
     */
    public boolean isOpenAiCompatible() {
        return protocol.isOpenAiFamily();
    }

    /**
     * 判断是否为 Anthropic 格式
     */
    public boolean isAnthropicFormat() {
        return protocol == ApiProtocol.ANTHROPIC;
    }

    /**
     * 根据路径查找端点（精确匹配）
     */
    public static ApiEndpoint fromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        for (ApiEndpoint endpoint : values()) {
            if (endpoint.path.equalsIgnoreCase(normalized)) {
                return endpoint;
            }
        }
        return null;
    }

    /**
     * 根据路径匹配端点（支持路径前缀匹配）
     * 用于处理子路径，如 /v1/messages/count_tokens
     */
    public static ApiEndpoint matchPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String normalized = path.toLowerCase(Locale.ROOT);

        // 优先精确匹配
        ApiEndpoint exact = fromPath(normalized);
        if (exact != null) {
            return exact;
        }

        // 前缀匹配（优先匹配更长的路径）
        ApiEndpoint bestMatch = null;
        int maxLength = 0;
        for (ApiEndpoint endpoint : values()) {
            String endpointPath = endpoint.path.toLowerCase(Locale.ROOT);
            if (normalized.startsWith(endpointPath) && endpointPath.length() > maxLength) {
                bestMatch = endpoint;
                maxLength = endpointPath.length();
            }
        }
        return bestMatch;
    }
}
