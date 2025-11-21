package com.nonfou.github.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 工具类
 * 用于从 Spring Security 上下文中获取当前用户信息
 */
@Slf4j
public class SecurityUtil {

    /**
     * 获取当前登录用户的 ID
     *
     * @return 当前用户ID,如果未登录返回null
     */
    public static Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 检查是否已认证
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("用户未认证");
                return null;
            }

            // 检查是否为匿名用户
            if ("anonymousUser".equals(authentication.getPrincipal())) {
                log.debug("匿名用户");
                return null;
            }

            // 获取用户ID (JwtAuthenticationFilter中设置的是userId作为principal)
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }

            log.warn("无法从Principal中获取userId, principal类型: {}",
                principal != null ? principal.getClass().getName() : "null");
            return null;

        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 获取当前登录用户的 ID (强制要求已登录)
     *
     * @return 当前用户ID
     * @throws IllegalStateException 如果用户未登录
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
        }
        return userId;
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return true表示已登录,false表示未登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}
