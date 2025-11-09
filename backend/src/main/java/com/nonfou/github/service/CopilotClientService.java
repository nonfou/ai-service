package com.nonfou.github.service;

import com.nonfou.github.config.CopilotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copilot API 客户端服务
 * 支持多端点和自动故障转移
 */
@Slf4j
@Service
public class CopilotClientService implements InitializingBean {

    @Autowired
    private CopilotConfig copilotConfig;

    @Autowired
    private RestTemplate restTemplate;

    private final AtomicInteger currentEndpointIndex = new AtomicInteger(0);

    /**
     * Bean 初始化后执行
     */
    @Override
    public void afterPropertiesSet() {
        List<CopilotConfig.Endpoint> endpoints = copilotConfig.getEnabledEndpoints();
        log.info("Copilot服务初始化完成，已启用{}个端点", endpoints.size());
        for (CopilotConfig.Endpoint endpoint : endpoints) {
            log.info("  - {} (优先级:{}) : {}", endpoint.getName(), endpoint.getPriority(), endpoint.getUrl());
        }
    }

    /**
     * 发送请求到 Copilot API（带故障转移）
     *
     * @param path 请求路径
     * @param method HTTP方法
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应数据
     */
    public <T> T sendRequest(String path, HttpMethod method, Object requestBody, Class<T> responseType) {
        List<CopilotConfig.Endpoint> endpoints = copilotConfig.getEnabledEndpoints();

        if (endpoints.isEmpty()) {
            throw new RuntimeException("没有可用的Copilot端点");
        }

        Exception lastException = null;

        // 遍历所有启用的端点（按优先级）
        for (CopilotConfig.Endpoint endpoint : endpoints) {
            // 跳过不健康的端点
            if (endpoint.getStatus() == CopilotConfig.EndpointStatus.UNHEALTHY) {
                log.warn("跳过不健康的端点: {}", endpoint.getName());
                continue;
            }

            // 尝试发送请求（带重试）
            for (int retry = 0; retry < copilotConfig.getFailover().getMaxRetries(); retry++) {
                try {
                    T response = executeRequest(endpoint, path, method, requestBody, responseType);

                    // 请求成功，重置失败计数
                    endpoint.setConsecutiveFailures(0);
                    endpoint.setStatus(CopilotConfig.EndpointStatus.HEALTHY);

                    log.info("请求成功: endpoint={}, path={}, retry={}", endpoint.getName(), path, retry);
                    return response;

                } catch (Exception e) {
                    lastException = e;
                    log.warn("请求失败: endpoint={}, path={}, retry={}/{}, error={}",
                        endpoint.getName(), path, retry + 1,
                        copilotConfig.getFailover().getMaxRetries(), e.getMessage());

                    // 增加失败计数
                    endpoint.setConsecutiveFailures(endpoint.getConsecutiveFailures() + 1);

                    // 如果连续失败次数过多，标记为不健康
                    if (endpoint.getConsecutiveFailures() >= copilotConfig.getFailover().getMaxRetries()) {
                        endpoint.setStatus(CopilotConfig.EndpointStatus.UNHEALTHY);
                        log.error("端点标记为不健康: {}, 连续失败{}次",
                            endpoint.getName(), endpoint.getConsecutiveFailures());
                        break; // 跳到下一个端点
                    }

                    // 最后一次重试失败，跳到下一个端点
                    if (retry == copilotConfig.getFailover().getMaxRetries() - 1) {
                        break;
                    }

                    // 重试前等待一小段时间
                    try {
                        Thread.sleep(100 * (retry + 1)); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // 所有端点都失败
        String errorMsg = String.format("所有Copilot端点都不可用，共尝试%d个端点", endpoints.size());
        log.error(errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }

    /**
     * 执行单个请求
     */
    private <T> T executeRequest(CopilotConfig.Endpoint endpoint, String path,
                                   HttpMethod method, Object requestBody, Class<T> responseType) {
        String url = endpoint.getUrl() + path;

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + endpoint.getToken());

        // 构建请求实体
        HttpEntity<?> entity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<T> response = restTemplate.exchange(
            url, method, entity, responseType
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("请求失败: HTTP " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * 健康检查（定时任务）
     * 每隔一段时间检查不健康的端点是否恢复
     */
    @Scheduled(fixedDelayString = "${copilot.failover.health-check-interval:30}000")
    public void healthCheck() {
        if (!copilotConfig.getFailover().isEnabled()) {
            return;
        }

        List<CopilotConfig.Endpoint> endpoints = copilotConfig.getEnabledEndpoints();

        for (CopilotConfig.Endpoint endpoint : endpoints) {
            // 只检查不健康的端点
            if (endpoint.getStatus() != CopilotConfig.EndpointStatus.UNHEALTHY) {
                continue;
            }

            try {
                // 尝试发送健康检查请求（可以是一个简单的 ping 或 status 接口）
                String url = endpoint.getUrl() + "/health";

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + endpoint.getToken());
                HttpEntity<?> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    // 恢复健康
                    endpoint.setStatus(CopilotConfig.EndpointStatus.HEALTHY);
                    endpoint.setConsecutiveFailures(0);
                    log.info("端点恢复健康: {}", endpoint.getName());
                }
            } catch (Exception e) {
                log.debug("健康检查失败: endpoint={}, error={}", endpoint.getName(), e.getMessage());
                // 继续保持不健康状态
            }

            endpoint.setLastHealthCheckTime(System.currentTimeMillis());
        }
    }

    /**
     * 获取当前端点状态
     */
    public List<CopilotConfig.Endpoint> getEndpointStatus() {
        return copilotConfig.getEnabledEndpoints();
    }

    /**
     * 手动标记端点为健康状态
     */
    public void markEndpointHealthy(String endpointName) {
        copilotConfig.getEndpoints().stream()
            .filter(e -> e.getName().equals(endpointName))
            .findFirst()
            .ifPresent(endpoint -> {
                endpoint.setStatus(CopilotConfig.EndpointStatus.HEALTHY);
                endpoint.setConsecutiveFailures(0);
                log.info("手动标记端点为健康: {}", endpointName);
            });
    }
}
