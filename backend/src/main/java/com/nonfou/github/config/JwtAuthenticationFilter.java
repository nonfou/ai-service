package com.nonfou.github.config;

import com.nonfou.github.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 从请求头中提取 JWT Token,验证并设置 Spring Security 认证上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 从请求头获取 Authorization
        String authHeader = request.getHeader("Authorization");

        // 检查是否是 Bearer Token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // 去掉 "Bearer " 前缀

            try {
                // 验证 Token 是否有效
                if (jwtUtil.validateToken(token)) {
                    // 提取用户信息
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String email = jwtUtil.getEmailFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);

                    // 如果 Token 中没有角色信息,默认为 USER
                    if (role == null || role.isEmpty()) {
                        role = "USER";
                    }

                    // 创建 Spring Security 认证对象
                    // 注意: Spring Security 的角色需要 ROLE_ 前缀
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userId,  // principal: 用户ID
                            null,    // credentials: 密码(JWT 不需要)
                            Collections.singletonList(authority)  // authorities: 角色权限
                        );

                    // 设置请求详情
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 将认证信息设置到 Spring Security 上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Token 验证失败,清空认证上下文
                SecurityContextHolder.clearContext();
            }
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
}
