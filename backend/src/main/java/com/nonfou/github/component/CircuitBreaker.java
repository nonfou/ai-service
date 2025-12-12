package com.nonfou.github.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简单的熔断器实现
 * 用于保护上游服务调用，防止级联失败
 */
@Slf4j
@Component
public class CircuitBreaker {

    /**
     * 熔断器状态
     */
    public enum State {
        CLOSED,      // 正常状态，允许请求通过
        OPEN,        // 熔断状态，拒绝所有请求
        HALF_OPEN    // 半开状态，允许有限请求进行测试
    }

    /**
     * 熔断器配置
     */
    public static class Config {
        private final int failureThreshold;     // 失败阈值
        private final Duration openDuration;    // 熔断持续时间
        private final int halfOpenRequests;     // 半开状态允许的请求数

        public Config(int failureThreshold, Duration openDuration, int halfOpenRequests) {
            this.failureThreshold = failureThreshold;
            this.openDuration = openDuration;
            this.halfOpenRequests = halfOpenRequests;
        }

        public static Config defaultConfig() {
            return new Config(5, Duration.ofSeconds(30), 3);
        }
    }

    /**
     * 单个熔断器实例
     */
    private static class CircuitBreakerInstance {
        private final String name;
        private final Config config;
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger halfOpenRequests = new AtomicInteger(0);
        private volatile Instant openTime;

        CircuitBreakerInstance(String name, Config config) {
            this.name = name;
            this.config = config;
        }

        boolean isCallPermitted() {
            State currentState = state.get();

            switch (currentState) {
                case CLOSED:
                    return true;

                case OPEN:
                    if (Instant.now().isAfter(openTime.plus(config.openDuration))) {
                        if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                            halfOpenRequests.set(0);
                            successCount.set(0);
                            log.info("熔断器 [{}] 进入半开状态", name);
                        }
                        return true;
                    }
                    return false;

                case HALF_OPEN:
                    return halfOpenRequests.incrementAndGet() <= config.halfOpenRequests;

                default:
                    return false;
            }
        }

        void recordSuccess() {
            State currentState = state.get();
            if (currentState == State.HALF_OPEN) {
                if (successCount.incrementAndGet() >= config.halfOpenRequests) {
                    if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                        failureCount.set(0);
                        log.info("熔断器 [{}] 恢复正常（关闭状态）", name);
                    }
                }
            } else if (currentState == State.CLOSED) {
                failureCount.set(0);
            }
        }

        void recordFailure() {
            State currentState = state.get();
            if (currentState == State.HALF_OPEN) {
                if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                    openTime = Instant.now();
                    log.warn("熔断器 [{}] 半开状态测试失败，重新开启熔断", name);
                }
            } else if (currentState == State.CLOSED) {
                if (failureCount.incrementAndGet() >= config.failureThreshold) {
                    if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                        openTime = Instant.now();
                        log.warn("熔断器 [{}] 触发熔断，失败次数: {}", name, failureCount.get());
                    }
                }
            }
        }

        State getState() {
            return state.get();
        }
    }

    private final ConcurrentHashMap<String, CircuitBreakerInstance> breakers = new ConcurrentHashMap<>();

    /**
     * 获取或创建熔断器
     */
    private CircuitBreakerInstance getBreaker(String name) {
        return breakers.computeIfAbsent(name, k -> new CircuitBreakerInstance(k, Config.defaultConfig()));
    }

    /**
     * 检查是否允许调用
     */
    public boolean isCallPermitted(String serviceName) {
        return getBreaker(serviceName).isCallPermitted();
    }

    /**
     * 记录成功调用
     */
    public void recordSuccess(String serviceName) {
        getBreaker(serviceName).recordSuccess();
    }

    /**
     * 记录失败调用
     */
    public void recordFailure(String serviceName) {
        getBreaker(serviceName).recordFailure();
    }

    /**
     * 获取熔断器状态
     */
    public State getState(String serviceName) {
        return getBreaker(serviceName).getState();
    }

    /**
     * 执行受保护的调用
     */
    public <T> T execute(String serviceName, java.util.function.Supplier<T> action,
                         java.util.function.Supplier<T> fallback) {
        if (!isCallPermitted(serviceName)) {
            log.warn("熔断器 [{}] 处于开启状态，执行降级逻辑", serviceName);
            return fallback.get();
        }

        try {
            T result = action.get();
            recordSuccess(serviceName);
            return result;
        } catch (Exception e) {
            recordFailure(serviceName);
            log.error("服务调用失败 [{}]: {}", serviceName, e.getMessage());
            return fallback.get();
        }
    }
}
