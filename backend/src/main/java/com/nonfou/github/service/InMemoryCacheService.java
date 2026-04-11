package com.nonfou.github.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存缓存服务，替代 Redis
 */
@Slf4j
@Service
public class InMemoryCacheService {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(String value, long expiresAtMillis) {
        boolean isExpired() {
            return expiresAtMillis > 0 && System.currentTimeMillis() > expiresAtMillis;
        }
    }

    public void set(String key, String value) {
        cache.put(key, new CacheEntry(value, 0));
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        long expiresAt = timeout > 0 ? System.currentTimeMillis() + unit.toMillis(timeout) : 0;
        cache.put(key, new CacheEntry(value, expiresAt));
    }

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    public Boolean delete(String key) {
        return cache.remove(key) != null;
    }

    public Boolean hasKey(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return false;
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return false;
        long expiresAt = timeout > 0 ? System.currentTimeMillis() + unit.toMillis(timeout) : 0;
        cache.put(key, new CacheEntry(entry.value(), expiresAt));
        return true;
    }

    public Long getExpire(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return -2L;
        if (entry.expiresAtMillis() <= 0) return -1L;
        long remaining = entry.expiresAtMillis() - System.currentTimeMillis();
        return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : -2L;
    }

    public Long increment(String key) {
        AtomicLong counter = new AtomicLong(0);
        // 简单实现：用 value 存数字字符串
        CacheEntry existing = cache.get(key);
        long newVal = (existing != null && !existing.isExpired()) ?
                Long.parseLong(existing.value()) + 1 : 1;
        long expiresAt = (existing != null) ? existing.expiresAtMillis() : 0;
        cache.put(key, new CacheEntry(String.valueOf(newVal), expiresAt));
        return newVal;
    }

    public Long increment(String key, long delta) {
        CacheEntry existing = cache.get(key);
        long currentVal = (existing != null && !existing.isExpired()) ?
                Long.parseLong(existing.value()) : 0;
        long newVal = currentVal + delta;
        long expiresAt = (existing != null) ? existing.expiresAtMillis() : 0;
        cache.put(key, new CacheEntry(String.valueOf(newVal), expiresAt));
        return newVal;
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        int sizeBefore = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = sizeBefore - cache.size();
        if (removed > 0) {
            log.debug("内存缓存清理: 移除 {} 个过期条目, 剩余 {}", removed, cache.size());
        }
    }
}
