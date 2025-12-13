package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Copilot Proxy 配置（用于转发到外部代理服务）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.copilot.proxy")
public class CopilotProxyProperties {

    /**
     * Copilot Proxy 基础地址（OpenAI 兼容）。
     */
    private String baseUrl = "http://127.0.0.1:4141/v1";

    /**
     * 访问授权密钥，可选。
     */
    private String apiKey;

    /**
     * 额外头部，如 X-Relay-Key 等。
     */
    private Map<String, String> extraHeaders = new HashMap<>();

    /**
     * HTTP 连接超时（毫秒）。
     */
    private int connectTimeoutMs = 5000;

    /**
     * HTTP 读超时（毫秒）。
     */
    private int readTimeoutMs = 60000;

    /**
     * SSE 超时时间（毫秒）。
     */
    private long streamTimeoutMs = 300000L;

    /**
     * Claude Code 热身请求使用的小模型。
     * 当请求有 anthropic-beta 头且无 tools 时，会使用此模型替代请求的模型。
     * 用于优化 Claude Code 2.0.28+ 的热身请求，减少高级模型配额消耗。
     */
    private String warmupModel = "claude-haiku-4.5";
}
