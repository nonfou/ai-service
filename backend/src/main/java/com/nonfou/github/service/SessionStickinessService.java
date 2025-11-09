package com.nonfou.github.service;

import com.nonfou.github.entity.SessionMapping;
import com.nonfou.github.mapper.SessionMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 会话粘性服务
 * 管理会话与账户的绑定关系
 */
@Slf4j
@Service
public class SessionStickinessService {

    private final SessionMappingMapper sessionMappingMapper;

    @Autowired(required = false)
    private RedisService redisService;

    @Value("${backend.scheduler.session-stickiness.ttl-hours:1}")
    private int sessionTtlHours;

    private static final String SESSION_PREFIX = "session:";

    public SessionStickinessService(SessionMappingMapper sessionMappingMapper) {
        this.sessionMappingMapper = sessionMappingMapper;
    }

    /**
     * 保存会话映射（简化版本 - 仅需要sessionHash和accountId）
     *
     * @param sessionHash       会话哈希
     * @param backendAccountId  后端账户ID
     */
    @Transactional
    public void saveMapping(String sessionHash, Long backendAccountId) {
        // 调用完整版本，apiKeyId 和 userId 设为 null（后续从上下文获取）
        saveSessionMapping(sessionHash, backendAccountId, null, null);
    }

    /**
     * 保存会话映射
     *
     * @param sessionHash       会话哈希
     * @param backendAccountId  后端账户ID
     * @param apiKeyId          API Key ID
     * @param userId            用户ID
     */
    @Transactional
    public void saveSessionMapping(String sessionHash, Long backendAccountId, Long apiKeyId, Long userId) {
        // 1. 保存到 Redis（快速查询）- 如果 Redis 可用
        if (redisService != null) {
            String redisKey = SESSION_PREFIX + sessionHash;
            try {
                redisService.set(redisKey, backendAccountId.toString(), (long)(sessionTtlHours * 3600), TimeUnit.SECONDS);
                log.debug("保存会话映射到 Redis: sessionHash={}, accountId={}", sessionHash, backendAccountId);
            } catch (Exception e) {
                log.error("保存会话映射到 Redis 失败", e);
            }
        } else {
            log.warn("Redis 服务不可用,跳过 Redis 缓存");
        }

        // 2. 保存到 MySQL（持久化 + 分析）
        SessionMapping existing = sessionMappingMapper.selectBySessionHash(sessionHash);
        if (existing != null) {
            // 更新现有映射
            existing.setBackendAccountId(backendAccountId);
            existing.setRequestCount(existing.getRequestCount() + 1);
            existing.setExpiresAt(LocalDateTime.now().plusHours(sessionTtlHours));
            sessionMappingMapper.updateById(existing);
            log.debug("更新会话映射: sessionHash={}, requestCount={}", sessionHash, existing.getRequestCount());
        } else {
            // 创建新映射
            SessionMapping mapping = new SessionMapping();
            mapping.setSessionHash(sessionHash);
            mapping.setBackendAccountId(backendAccountId);
            mapping.setApiKeyId(apiKeyId);
            mapping.setUserId(userId);
            mapping.setRequestCount(1);
            mapping.setExpiresAt(LocalDateTime.now().plusHours(sessionTtlHours));
            sessionMappingMapper.insert(mapping);
            log.debug("创建新会话映射: sessionHash={}, accountId={}", sessionHash, backendAccountId);
        }
    }

    /**
     * 根据会话哈希获取账户ID
     *
     * @param sessionHash 会话哈希
     * @return 账户ID，如果不存在或已过期则返回 null
     */
    public Long getAccountIdBySession(String sessionHash) {
        if (sessionHash == null || sessionHash.isEmpty()) {
            return null;
        }

        String redisKey = SESSION_PREFIX + sessionHash;

        // 1. 优先从 Redis 查询（如果可用）
        if (redisService != null) {
            try {
                String value = redisService.get(redisKey);
                if (value != null) {
                    log.debug("从 Redis 获取会话映射: sessionHash={}, accountId={}", sessionHash, value);
                    return Long.parseLong(value);
                }
            } catch (Exception e) {
                log.warn("从 Redis 获取会话映射失败", e);
            }
        }

        // 2. 降级从 MySQL 查询
        SessionMapping mapping = sessionMappingMapper.selectBySessionHash(sessionHash);
        if (mapping != null && !mapping.isExpired()) {
            // 回写到 Redis（如果可用）
            if (redisService != null) {
                try {
                    long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), mapping.getExpiresAt()).getSeconds();
                    if (remainingSeconds > 0) {
                        redisService.set(redisKey, mapping.getBackendAccountId().toString(), remainingSeconds, TimeUnit.SECONDS);
                    }
                } catch (Exception e) {
                    log.warn("回写会话映射到 Redis 失败", e);
                }
            }
            log.debug("从 MySQL 获取会话映射: sessionHash={}, accountId={}", sessionHash, mapping.getBackendAccountId());
            return mapping.getBackendAccountId();
        }

        return null;
    }

    /**
     * 删除会话映射
     */
    @Transactional
    public void deleteSessionMapping(String sessionHash) {
        // 1. 从 Redis 删除（如果可用）
        if (redisService != null) {
            String redisKey = SESSION_PREFIX + sessionHash;
            try {
                redisService.delete(redisKey);
            } catch (Exception e) {
                log.warn("从 Redis 删除会话映射失败", e);
            }
        }

        // 2. 从 MySQL 删除
        SessionMapping mapping = sessionMappingMapper.selectBySessionHash(sessionHash);
        if (mapping != null) {
            sessionMappingMapper.deleteById(mapping.getId());
        }

        log.info("删除会话映射: sessionHash={}", sessionHash);
    }

    /**
     * 清理过期的会话映射（定时任务调用）
     */
    @Transactional
    public int cleanupExpiredSessions() {
        int deleted = sessionMappingMapper.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("清理过期会话映射: count={}", deleted);
        }
        return deleted;
    }

    /**
     * 获取用户的活跃会话数
     */
    public int getActiveSessionCountByUser(Long userId) {
        return sessionMappingMapper.countActiveSessionsByUserId(userId);
    }

    /**
     * 获取账户的活跃会话数
     */
    public int getActiveSessionCountByAccount(Long accountId) {
        return sessionMappingMapper.countActiveSessionsByAccountId(accountId);
    }
}
