package com.nonfou.github.enums;

import java.util.Locale;

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

    /**
     * 根据模型名称判断应使用的协议格式
     * 用于确定调用哪个上游端点
     *
     * @param model 模型名称
     * @return 对应的协议格式
     */
    public static ApiProtocol fromModel(String model) {
        if (model == null || model.isEmpty()) {
            return OPENAI; // 默认使用 OpenAI 格式
        }

        String lower = model.toLowerCase(Locale.ROOT);

        // Claude 系列模型使用 Anthropic 协议
        if (lower.startsWith("claude-") || lower.contains("claude")) {
            return ANTHROPIC;
        }

        // Codex 系列模型使用 OpenAI Codex 协议
        if (lower.contains("codex")) {
            return OPENAI_CODEX;
        }

        // GPT 系列模型使用 OpenAI 标准协议
        if (lower.startsWith("gpt-")) {
            return OPENAI;
        }

        // text-embedding 等模型使用 OpenAI 标准协议
        if (lower.startsWith("text-")) {
            return OPENAI;
        }

        // 默认使用 OpenAI 格式
        return OPENAI;
    }
}
