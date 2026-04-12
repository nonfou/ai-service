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
    private String baseUrl;

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
    private int connectTimeoutMs = 10000;

    /**
     * HTTP 读超时（毫秒）。
     * 注意：对于流式响应，此超时是指两次数据块之间的最大等待时间。
     * 设置为 0 表示无限等待（不推荐）。
     * 默认 10 分钟，适用于 Claude 大模型的长时间思考。
     */
    private int readTimeoutMs = 600000;

    /**
     * SSE 超时时间（毫秒）。
     * 这是 SseEmitter 的整体超时时间，包括整个流式响应的持续时间。
     * 设置为 0 表示无限等待。
     * 默认 30 分钟，适用于长时间的对话。
     */
    private long streamTimeoutMs = 1800000L;

}
