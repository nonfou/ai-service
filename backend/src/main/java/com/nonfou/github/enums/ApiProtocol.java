package com.nonfou.github.enums;

/**
 * API 协议格式枚举
 * 定义不同 API 供应商的协议格式
 */
public enum ApiProtocol {

    /**
     * OpenAI 标准格式
     * 用于 /v1/chat/completions, /v1/models, /v1/embeddings
     */
    OPENAI("openai", "OpenAI 标准格式"),

    /**
     * OpenAI Codex 格式
     * 用于 /v1/responses (Codex CLI)
     */
    OPENAI_CODEX("openai_codex", "OpenAI Codex 格式"),

    /**
     * Anthropic Claude 格式
     * 用于 /v1/messages
     */
    ANTHROPIC("anthropic", "Anthropic Claude 格式");

    private final String code;
    private final String description;

    ApiProtocol(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否使用 OpenAI 系列格式（包括 Codex）
     */
    public boolean isOpenAiFamily() {
        return this == OPENAI || this == OPENAI_CODEX;
    }
}
