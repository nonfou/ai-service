package com.nonfou.github.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 验证码存储与限流服务。
 * 支持 Redis 与本地内存双写，以便在没有 Redis 的环境下仍可使用一次性验证码。
 */
@Slf4j
@Service
public class VerifyCodeService {

    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final String CODE_KEY_PREFIX = "verify_code:data:";
    private static final String COOLDOWN_KEY_PREFIX = "verify_code:cooldown:";
    private static final String HOURLY_KEY_PREFIX = "verify_code:hourly:";

    @Autowired(required = false)
    private RedisService redisService;

    @Value("${auth.verify-code.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${auth.verify-code.min-interval-seconds:60}")
    private long minIntervalSeconds;

    @Value("${auth.verify-code.max-per-hour:10}")
    private int maxPerHour;

    @Value("${auth.verify-code.hash-secret:${jwt.secret:verify-code-secret}}")
    private String hashSecret;

    private final Map<String, LocalCodeRecord> localCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> localCooldown = new ConcurrentHashMap<>();
    private final Map<String, LocalRateRecord> localHourly = new ConcurrentHashMap<>();

    /**
     * 发送验证码前检查限流。
     */
    public void ensureCanSend(String email) {
        if (redisService != null) {
            ensureCanSendWithRedis(email);
        } else {
            ensureCanSendLocally(email);
        }
    }

    /**
     * 存储验证码（哈希），并记录限流信息。
     */
    public void persistCode(String email, String plainCode) {
        String hashed = hash(email, plainCode);

        if (redisService != null) {
            redisService.set(buildCodeKey(email), hashed, ttlSeconds, TimeUnit.SECONDS);
            // 发送冷却
            redisService.set(buildCooldownKey(email), "1", minIntervalSeconds, TimeUnit.SECONDS);
            incrementHourlyCounterRedis(email);
        } else {
            localCodes.put(email, new LocalCodeRecord(hashed, Instant.now().plusSeconds(ttlSeconds)));
            localCooldown.put(email, System.currentTimeMillis() + minIntervalSeconds * 1000);
            incrementHourlyCounterLocal(email);
        }
    }

    /**
     * 校验验证码（成功后立即清除）。
     */
    public boolean verifyCode(String email, String plainCode) {
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
        if (redisService != null) {
            redisService.delete(buildCodeKey(email));
        } else {
            localCodes.remove(email);
        }
    }

    private void ensureCanSendWithRedis(String email) {
        // 冷却检查
        Boolean cooling = redisService.hasKey(buildCooldownKey(email));
        if (Boolean.TRUE.equals(cooling)) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
        }

        // 小时次数检查
        String hourlyKey = buildHourlyKey(email);
        String value = redisService.get(hourlyKey);
        int counter = value != null ? Integer.parseInt(value) : 0;
        if (counter >= maxPerHour) {
            throw new RuntimeException("验证码发送次数过多，请一小时后再试");
        }
    }

    private void ensureCanSendLocally(String email) {
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

    private void incrementHourlyCounterRedis(String email) {
        String hourlyKey = buildHourlyKey(email);
        String value = redisService.get(hourlyKey);
        int counter = value != null ? Integer.parseInt(value) : 0;
        counter++;
        Long expire = redisService.getExpire(hourlyKey);
        long ttl = (expire != null && expire > 0) ? expire : ONE_HOUR_SECONDS;
        redisService.set(hourlyKey, String.valueOf(counter), ttl, TimeUnit.SECONDS);
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
        if (redisService != null) {
            return redisService.get(buildCodeKey(email));
        }

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

    private String buildCodeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String buildCooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }

    private String buildHourlyKey(String email) {
        return HOURLY_KEY_PREFIX + email;
    }

    private record LocalCodeRecord(String hash, Instant expiresAt) {}

    private static class LocalRateRecord {
        private int count;
        private final long windowEndsAt;

        LocalRateRecord(int count, long windowEndsAt) {
            this.count = count;
            this.windowEndsAt = windowEndsAt;
        }

        void increment() {
            this.count++;
        }

        int getCount() {
            return count;
        }

        boolean isExpired(long now) {
            return now >= windowEndsAt;
        }
    }
}
