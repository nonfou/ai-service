package com.nonfou.github.config;

import com.nonfou.github.filter.RateLimitFilter;
import com.nonfou.github.security.RestAccessDeniedHandler;
import com.nonfou.github.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置类
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    /**
     * CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 启用 CORS (使用 corsConfigurationSource Bean)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 禁用 CSRF(因为使用 JWT,不需要 CSRF 保护)
            .csrf(AbstractHttpConfigurer::disable)

            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 健康检查端点: 允许所有访问 (同时支持有/无 /api 前缀)
                .requestMatchers("/health", "/ready", "/live", "/api/health", "/api/ready", "/api/live", "/actuator/**").permitAll()
                // 公开接口:允许所有认证相关的请求(登录、发送验证码等)
                .requestMatchers("/api/auth/**").permitAll()
                // 公开接口:允许获取模型列表
                .requestMatchers("/api/models").permitAll()
                // 公开接口:允许 AI API 聊天接口(通过 API Key 验证)
                // 支持 OpenAI, Claude, Codex 等格式
                .requestMatchers("/v1/**").permitAll()
                // 公开接口:允许支付回调
                .requestMatchers("/api/recharge/alipay/notify", "/api/recharge/alipay/return").permitAll()
                .requestMatchers("/api/recharge/wechat/notify").permitAll()
                // 公开接口:允许 Stripe Webhook 回调
                .requestMatchers("/api/recharge/webhook").permitAll()
                .requestMatchers("/api/recharge/config").permitAll()
                // 公开接口:允许管理员登录
                .requestMatchers("/admin/login").permitAll()
                // ? [安全修复] 已删除不安全的重置密码接口 (CVSS 9.8 - CWE-306)
                // 该接口允许任何人无需身份验证即可重置管理员密码,存在严重安全风险
                // 管理员密码重置请通过数据库直接修改,或使用需要身份验证的修改密码功能

                // 管理端接口:仅管理员可访问
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/models/admin/**").hasRole("ADMIN")

                // 用户端接口:普通用户和管理员都可访问
                .anyRequest().hasAnyRole("USER", "ADMIN")
            )

            // Session管理策略
            // ? 改为IF_REQUIRED以支持CSRF Token存储到Session
            // JWT认证本身仍然是无状态的,Session仅用于CSRF防护
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // 统一处理未认证与权限不足的响应
            .exceptionHandling(handler -> handler
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )

            // 禁用默认登录页面
            .formLogin(AbstractHttpConfigurer::disable)

            // 禁用默认登出页面
            .logout(AbstractHttpConfigurer::disable)

            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 添加速率限制过滤器（在 JWT 认证之前）
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
