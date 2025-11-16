package com.nonfou.github.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;

/**
 * Web 配置类
 */
@Configuration
public class WebConfig {

    @Value("${copilot.failover.timeout:5000}")
    private int copilotTimeout;

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * RestTemplate Bean
     * 用于调用 Copilot API，配置超时时间
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(copilotTimeout))
                .setReadTimeout(Duration.ofMillis(copilotTimeout))
                .build();
    }

    /**
     * SSE/流式请求专用线程池，避免每次请求都新建线程。
     */
    @Bean(name = "streamTaskExecutor")
    public TaskExecutor streamTaskExecutor(
            @Value("${backend.streaming.core-pool-size:4}") int corePoolSize,
            @Value("${backend.streaming.max-pool-size:16}") int maxPoolSize,
            @Value("${backend.streaming.queue-capacity:200}") int queueCapacity,
            @Value("${backend.streaming.keep-alive-seconds:60}") int keepAliveSeconds
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("sse-worker-");
        executor.initialize();
        return executor;
    }
}
