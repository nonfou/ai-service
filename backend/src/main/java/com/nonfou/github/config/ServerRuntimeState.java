package com.nonfou.github.config;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 服务运行时状态
 * <p>
 * 目前用于记录服务启动时间, 便于在鉴权阶段识别重启前签发的令牌并要求重新登录。
 * </p>
 */
@Component
public class ServerRuntimeState {

    /**
     * 服务启动时间(UTC)
     */
    private final Instant startTime = Instant.now();

    public Instant getStartTime() {
        return startTime;
    }
}
