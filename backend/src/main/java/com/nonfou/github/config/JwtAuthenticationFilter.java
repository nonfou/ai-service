package com.nonfou.github.config;

import com.nonfou.github.util.JwtUtil;
import com.nonfou.github.util.LogMaskUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
 * JWT 认证过滤器。
 * 仅从 Authorization Header 提取 Bearer Token，并设置认证上下文。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String clientIp = getClientIp(request);
        String token = extractTokenFromAuthorizationHeader(request);

        // 如果找到Token,进行验证
        if (token != null) {

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

                    // ✅ 成功认证的日志 (DEBUG级别)
                    log.debug("JWT认证成功: userId={}, role={}, uri={}", userId, role, requestUri);
                }

            } catch (ExpiredJwtException e) {
                // ✅ Token 过期 (正常情况,INFO级别)
                log.info("JWT Token已过期: uri={}, ip={}", requestUri, LogMaskUtil.maskIp(clientIp));
                SecurityContextHolder.clearContext();

            } catch (SignatureException e) {
                // ✅ 签名验证失败 (可能是攻击,WARN级别)
                log.warn("⚠️ JWT签名验证失败(可能是伪造攻击): uri={}, ip={}, error={}",
                    requestUri, LogMaskUtil.maskIp(clientIp), e.getMessage());
                SecurityContextHolder.clearContext();
                // TODO: 可以在这里记录可疑IP到黑名单

            } catch (MalformedJwtException e) {
                // ✅ Token 格式错误
                log.warn("JWT格式错误: uri={}, ip={}, error={}",
                    requestUri, LogMaskUtil.maskIp(clientIp), e.getMessage());
                SecurityContextHolder.clearContext();

            } catch (UnsupportedJwtException e) {
                // ✅ 不支持的Token类型
                log.warn("不支持的JWT类型: uri={}, ip={}, error={}",
                    requestUri, LogMaskUtil.maskIp(clientIp), e.getMessage());
                SecurityContextHolder.clearContext();

            } catch (IllegalArgumentException e) {
                // ✅ Token为空或null
                log.warn("JWT参数非法: uri={}, ip={}, error={}",
                    requestUri, LogMaskUtil.maskIp(clientIp), e.getMessage());
                SecurityContextHolder.clearContext();

            } catch (Exception e) {
                // ✅ 未知错误 (ERROR级别,需要关注)
                log.error("❌ JWT验证未知错误: uri={}, ip={}, error={}",
                    requestUri, LogMaskUtil.maskIp(clientIp), e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization Header 提取 Token
     */
    private String extractTokenFromAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端真实IP地址
     * 支持通过代理/负载均衡器的场景
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是多级代理,取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
