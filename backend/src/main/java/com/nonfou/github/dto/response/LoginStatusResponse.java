package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录状态检查响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginStatusResponse {

    /**
     * 是否已登录
     */
    private Boolean isLoggedIn;

    /**
     * 用户信息(仅登录时返回)
     */
    private UserInfo userInfo;

    /**
     * 用户信息嵌套类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 用户名
         */
        private String username;

        /**
         * 余额
         */
        private java.math.BigDecimal balance;
    }
}
