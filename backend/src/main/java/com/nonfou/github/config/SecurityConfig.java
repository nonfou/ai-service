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
                // 公开接口:允许重置密码(临时,仅用于开发)
                .requestMatchers("/api/admin/reset-password").permitAll()

                // 管理端接口:仅管理员可访问
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/models/admin/**").hasRole("ADMIN")

                // 用户端接口:普通用户和管理员都可访问
                .anyRequest().hasAnyRole("USER", "ADMIN")
            )

            // 禁用 Session(使用 JWT 无状态认证)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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
