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
    private final HttpPoolProperties httpPoolProperties;
    private final StreamingProperties streamingProperties;

    /**
     * 跨域配置。
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
     * HTTP 连接池管理器
     * 复用 TCP 连接，减少连接建立开销
     */
    @Bean
    public PoolingHttpClientConnectionManager httpClientConnectionManager() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(copilotProxyProperties.getConnectTimeoutMs()))
                .setSocketTimeout(Timeout.ofMilliseconds(copilotProxyProperties.getReadTimeoutMs()))
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(httpPoolProperties.getValidateAfterInactivityMs()))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(httpPoolProperties.getMaxTotal())
                .setMaxConnPerRoute(httpPoolProperties.getMaxPerRoute())
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        log.info("HTTP 连接池初始化: maxTotal={}, maxPerRoute={}",
                httpPoolProperties.getMaxTotal(), httpPoolProperties.getMaxPerRoute());
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
    public TaskExecutor streamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(streamingProperties.getCorePoolSize());
        executor.setMaxPoolSize(streamingProperties.getMaxPoolSize());
        executor.setQueueCapacity(streamingProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(streamingProperties.getKeepAliveSeconds());
        executor.setThreadNamePrefix("sse-worker-");
        executor.initialize();
        return executor;
    }
}
