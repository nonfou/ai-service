package com.nonfou.github.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 验证码存储与限流服务（内存版）
 */
@Slf4j
@Service
public class VerifyCodeService {

    private static final long ONE_HOUR_SECONDS = 3600L;

    @Value("${auth.verify-code.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${auth.verify-code.min-interval-seconds:60}")
    private long minIntervalSeconds;

    @Value("${auth.verify-code.max-per-hour:10}")
    private int maxPerHour;

    @Value("${auth.verify-code.hash-secret:${jwt.secret:verify-code-secret}}")
    private String hashSecret;

    @Value("${auth.verify-code.default-code:}")
    private String defaultCode;

    private final Map<String, LocalCodeRecord> localCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> localCooldown = new ConcurrentHashMap<>();
    private final Map<String, LocalRateRecord> localHourly = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${auth.verify-code.cleanup-interval-ms:300000}")
    public void cleanupExpiredLocalEntries() {
        Instant now = Instant.now();
        long nowMs = System.currentTimeMillis();
        localCodes.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        localCooldown.entrySet().removeIf(e -> e.getValue() <= nowMs);
        localHourly.entrySet().removeIf(e -> e.getValue().isExpired(nowMs));
    }

    public void ensureCanSend(String email) {
        long now = System.currentTimeMillis();
        Long coolingUntil = localCooldown.get(email);
        if (coolingUntil != null && coolingUntil > now) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
        }

        LocalRateRecord rateRecord = localHourly.get(email);
        if (rateRecord != null && !rateRecord.isExpired(now) && rateRecord.getCount() >= maxPerHour) {
            throw new RuntimeException("验证码发送次数过多，请一小时后再试");
        }
    }

    public void persistCode(String email, String plainCode) {
        String hashed = hash(email, plainCode);
        localCodes.put(email, new LocalCodeRecord(hashed, Instant.now().plusSeconds(ttlSeconds)));
        localCooldown.put(email, System.currentTimeMillis() + minIntervalSeconds * 1000);
        incrementHourlyCounterLocal(email);
    }

    public boolean verifyCode(String email, String plainCode) {
        if (defaultCode != null && !defaultCode.isEmpty() && defaultCode.equals(plainCode)) {
            log.info("使用默认测试验证码登录: {}", email);
            return true;
        }

        String expectedHash = fetchStoredHash(email);
        if (expectedHash == null) {
            return false;
        }

        String actualHash = hash(email, plainCode);
        boolean matched = MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8)
        );

        if (matched) {
            deleteCode(email);
        }

        return matched;
    }

    public void deleteCode(String email) {
        localCodes.remove(email);
    }

    private void incrementHourlyCounterLocal(String email) {
        long now = System.currentTimeMillis();
        localHourly.compute(email, (key, record) -> {
            if (record == null || record.isExpired(now)) {
                return new LocalRateRecord(1, now + ONE_HOUR_SECONDS * 1000);
            }
            record.increment();
            return record;
        });
    }

    private String fetchStoredHash(String email) {
        LocalCodeRecord record = localCodes.get(email);
        if (record == null) {
            return null;
        }
        if (record.expiresAt().isBefore(Instant.now())) {
            localCodes.remove(email);
            return null;
        }
        return record.hash();
    }

    private String hash(String email, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = email + ":" + code + ":" + hashSecret;
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法初始化验证码哈希算法", e);
        }
    }

    private record LocalCodeRecord(String hash, Instant expiresAt) {}

    private static class LocalRateRecord {
        private int count;
        private final long windowEndsAt;

        LocalRateRecord(int count, long windowEndsAt) {
            this.count = count;
            this.windowEndsAt = windowEndsAt;
        }

        void increment() { this.count++; }
        int getCount() { return count; }
        boolean isExpired(long now) { return now >= windowEndsAt; }
    }
}
