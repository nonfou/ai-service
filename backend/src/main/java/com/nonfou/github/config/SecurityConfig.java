package com.nonfou.github.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF(因为使用 JWT,不需要 CSRF 保护)
            .csrf(AbstractHttpConfigurer::disable)

            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开接口:允许所有认证相关的请求(登录、发送验证码等)
                .requestMatchers("/api/auth/**").permitAll()
                // 公开接口:允许获取模型列表
                .requestMatchers("/api/models").permitAll()
                // 公开接口:允许聊天接口(通过 API Key 验证)
                .requestMatchers("/api/chat", "/api/v1/chat/completions").permitAll()
                // 公开接口:允许支付回调
                .requestMatchers("/api/recharge/alipay/notify", "/api/recharge/alipay/return").permitAll()
                .requestMatchers("/api/recharge/wechat/notify").permitAll()
                // 公开接口:允许管理员登录
                .requestMatchers("/api/admin/login").permitAll()
                // ✅ [安全修复] 已删除不安全的重置密码接口 (CVSS 9.8 - CWE-306)
                // 该接口允许任何人无需身份验证即可重置管理员密码,存在严重安全风险
                // 管理员密码重置请通过数据库直接修改,或使用需要身份验证的修改密码功能

                // 管理端接口:仅管理员可访问
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/models/admin/**").hasRole("ADMIN")

                // 用户端接口:普通用户和管理员都可访问
                .anyRequest().hasAnyRole("USER", "ADMIN")
            )

            // Session管理策略
            // ✅ 改为IF_REQUIRED以支持CSRF Token存储到Session
            // JWT认证本身仍然是无状态的,Session仅用于CSRF防护
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // 禁用默认登录页面
            .formLogin(AbstractHttpConfigurer::disable)

            // 禁用默认登出页面
            .logout(AbstractHttpConfigurer::disable)

            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
