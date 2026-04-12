package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.config.AdminSecurityProperties;
import com.nonfou.github.entity.Admin;
import com.nonfou.github.mapper.AdminMapper;
import com.nonfou.github.util.JwtUtil;
import com.nonfou.github.util.LogMaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 极简管理员服务，只保留登录能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final AdminSecurityProperties adminSecurityProperties;
    private final InMemoryCacheService cacheService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password) {
        if (adminSecurityProperties.isEnableLoginLock()) {
            String lockKey = "admin:login:lock:" + username;
            String locked = cacheService.get(lockKey);
            if ("1".equals(locked)) {
                throw new RuntimeException("账号已被锁定,请" + adminSecurityProperties.getLockDuration() + "分钟后重试");
            }
        }

        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            recordLoginFailure(username);
            throw new RuntimeException("用户名或密码错误");
        }

        if (admin.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        clearLoginFailure(username);

        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("role", admin.getRole());

        log.info("管理员登录成功: username={}", LogMaskUtil.mask(username, 2, 1));
        return result;
    }

    private void recordLoginFailure(String username) {
        if (!adminSecurityProperties.isEnableLoginLock()) {
            return;
        }

        String failKey = "admin:login:fail:" + username;
        String lockKey = "admin:login:lock:" + username;

        Long failCount = cacheService.increment(failKey);
        if (failCount == 1) {
            cacheService.expire(failKey, adminSecurityProperties.getLockDuration(), TimeUnit.MINUTES);
        }

        if (failCount >= adminSecurityProperties.getMaxLoginAttempts()) {
            cacheService.set(lockKey, "1", adminSecurityProperties.getLockDuration(), TimeUnit.MINUTES);
            log.warn("管理员账号已锁定: username={}, 失败次数={}", LogMaskUtil.mask(username, 2, 1), failCount);
        }
    }

    private void clearLoginFailure(String username) {
        if (!adminSecurityProperties.isEnableLoginLock()) {
            return;
        }

        String failKey = "admin:login:fail:" + username;
        String lockKey = "admin:login:lock:" + username;

        cacheService.delete(failKey);
        cacheService.delete(lockKey);
    }
}
