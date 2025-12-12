package com.nonfou.github.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.concurrent.TimeUnit;

/**
 * Web 配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final CopilotProxyProperties copilotProxyProperties;

    /**
     * 跨域配置
     *
     * ✅ setAllowCredentials(true): 允许携带Cookie和认证信息
     *    这对于HttpOnly Cookie认证是必需的
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);  // ✅ 允许携带Cookie(HttpOnly Cookie必需)
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * HTTP 连接池管理器
     * 复用 TCP 连接，减少连接建立开销
     */
    @Bean
    public PoolingHttpClientConnectionManager httpClientConnectionManager(
            @Value("${http.pool.max-total:200}") int maxTotal,
            @Value("${http.pool.max-per-route:50}") int maxPerRoute,
            @Value("${http.pool.validate-after-inactivity-ms:2000}") int validateAfterInactivity
    ) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(copilotProxyProperties.getConnectTimeoutMs()))
                .setSocketTimeout(Timeout.ofMilliseconds(copilotProxyProperties.getReadTimeoutMs()))
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(validateAfterInactivity))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(maxTotal)
                .setMaxConnPerRoute(maxPerRoute)
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        log.info("HTTP 连接池初始化: maxTotal={}, maxPerRoute={}", maxTotal, maxPerRoute);
        return connectionManager;
    }

    /**
     * HttpClient Bean
     * 使用连接池管理器
     */
    @Bean
    public HttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(5000))
                .setResponseTimeout(Timeout.ofMilliseconds(copilotProxyProperties.getReadTimeoutMs()))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build();
    }

    /**
     * RestTemplate Bean
     * 使用 HTTP 连接池，提高并发性能
     */
    @Bean
    public RestTemplate restTemplate(HttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
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
