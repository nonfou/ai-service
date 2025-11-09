package com.nonfou.github.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 服务
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置值
     */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值（带过期时间）
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 判断键是否存在
     */
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    /**
     * 保存验证码
     */
    public void saveVerifyCode(String email, String code) {
        String key = "verify_code:" + email;
        set(key, code, 5, TimeUnit.MINUTES); // 5分钟过期
    }

    /**
     * 获取验证码
     */
    public String getVerifyCode(String email) {
        String key = "verify_code:" + email;
        return get(key);
    }

    /**
     * 删除验证码
     */
    public void deleteVerifyCode(String email) {
        String key = "verify_code:" + email;
        delete(key);
    }
}
