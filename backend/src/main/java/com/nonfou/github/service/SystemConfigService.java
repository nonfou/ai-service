package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.entity.SystemConfig;
import com.nonfou.github.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 */
@Slf4j
@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Value("${system-config.cache-ttl-seconds:60}")
    private long cacheTtlSeconds;

    private final Map<String, CacheEntry> configCache = new ConcurrentHashMap<>();

    /**
     * 获取配置值
     */
    public String get(String key) {
        CacheEntry cached = configCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        return loadAndCache(key);
    }

    /**
     * 获取配置值（带默认值）
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 BigDecimal 配置
     */
    public BigDecimal getBigDecimal(String key) {
        String value = get(key);
        return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
    }

    /**
     * 获取 Integer 配置
     */
    public Integer getInteger(String key) {
        String value = get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * 更新配置
     */
    public void update(String key, String value) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config != null) {
            config.setConfigValue(value);
            systemConfigMapper.updateById(config);
            // 更新缓存
            configCache.put(key, new CacheEntry(value, expireAt()));
            log.info("配置已更新: {} = {}", key, value);
        }
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        configCache.clear();
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        for (SystemConfig config : configs) {
            configCache.put(
                    config.getConfigKey(),
                    new CacheEntry(config.getConfigValue(), expireAt())
            );
        }
        log.info("配置缓存已刷新，共 {} 项", configs.size());
    }

    private String loadAndCache(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config == null) {
            configCache.remove(key);
            return null;
        }

        CacheEntry entry = new CacheEntry(config.getConfigValue(), expireAt());
        configCache.put(key, entry);
        return entry.value();
    }

    private Instant expireAt() {
        if (cacheTtlSeconds <= 0) {
            // 0 或负数表示不缓存
            return Instant.EPOCH;
        }
        return Instant.now().plusSeconds(cacheTtlSeconds);
    }

    private record CacheEntry(String value, Instant expiresAt) {
        boolean isExpired() {
            if (expiresAt.equals(Instant.EPOCH)) {
                return false;
            }
            return Instant.now().isAfter(expiresAt);
        }
    }
}
