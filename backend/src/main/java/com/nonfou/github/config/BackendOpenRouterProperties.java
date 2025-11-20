package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenRouter 后端配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "backend.openrouter")
public class BackendOpenRouterProperties {

    /**
     * OpenRouter API 基础地址
     */
    private String baseUrl = "https://openrouter.ai/api/v1";

    /**
     * 是否获取实时定价
     */
    private boolean fetchRealtimePricing = false;

    /**
     * OpenRouter 账户配置列表
     */
    private List<AccountConfig> accounts = new ArrayList<>();

    /**
     * OpenRouter 账户配置
     */
    @Data
    public static class AccountConfig {
        /**
         * 账户名称
         */
        private String name;

        /**
         * API Key
         */
        private String apiKey;

        /**
         * 优先级
         */
        private int priority = 1;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }
}
