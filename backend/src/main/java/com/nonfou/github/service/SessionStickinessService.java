package com.nonfou.github.service;

import com.nonfou.github.entity.SessionMapping;
import com.nonfou.github.mapper.SessionMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 会话粘性服务（纯数据库版）
 */
@Slf4j
@Service
public class SessionStickinessService {

    private final SessionMappingMapper sessionMappingMapper;

    @Value("${backend.scheduler.session-stickiness.ttl-hours:1}")
    private int sessionTtlHours;

    public SessionStickinessService(SessionMappingMapper sessionMappingMapper) {
        this.sessionMappingMapper = sessionMappingMapper;
    }

    @Transactional
    public void saveMapping(String sessionHash, Long backendAccountId) {
        saveSessionMapping(sessionHash, backendAccountId, null, null);
    }

    @Transactional
    public void saveSessionMapping(String sessionHash, Long backendAccountId, Long apiKeyId, Long userId) {
        SessionMapping existing = sessionMappingMapper.selectBySessionHash(sessionHash);
        if (existing != null) {
            existing.setBackendAccountId(backendAccountId);
            existing.setRequestCount(existing.getRequestCount() + 1);
            existing.setExpiresAt(LocalDateTime.now().plusHours(sessionTtlHours));
            sessionMappingMapper.updateById(existing);
            log.debug("更新会话映射: sessionHash={}, requestCount={}", sessionHash, existing.getRequestCount());
        } else {
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

    public Long getAccountIdBySession(String sessionHash) {
        if (sessionHash == null || sessionHash.isEmpty()) {
            return null;
        }

        SessionMapping mapping = sessionMappingMapper.selectBySessionHash(sessionHash);
        if (mapping != null && !mapping.isExpired()) {
            log.debug("从数据库获取会话映射: sessionHash={}, accountId={}", sessionHash, mapping.getBackendAccountId());
            return mapping.getBackendAccountId();
        }

        return null;
    }

    @Transactional
    public void deleteSessionMapping(String sessionHash) {
        SessionMapping mapping = sessionMappingMapper.selectBySessionHash(sessionHash);
        if (mapping != null) {
            sessionMappingMapper.deleteById(mapping.getId());
        }
        log.info("删除会话映射: sessionHash={}", sessionHash);
    }

    @Transactional
    public int cleanupExpiredSessions() {
        int deleted = sessionMappingMapper.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("清理过期会话映射: count={}", deleted);
        }
        return deleted;
    }

    public int getActiveSessionCountByUser(Long userId) {
        return sessionMappingMapper.countActiveSessionsByUserId(userId);
    }

    public int getActiveSessionCountByAccount(Long accountId) {
        return sessionMappingMapper.countActiveSessionsByAccountId(accountId);
    }
}
