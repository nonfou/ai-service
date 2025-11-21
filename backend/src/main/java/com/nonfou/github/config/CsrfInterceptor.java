package com.nonfou.github.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * CSRF Token 验证拦截器
 * 用于防止跨站请求伪造(CSRF)攻击
 *
 * 工作原理:
 * 1. 只对修改数据的请求方法(POST, PUT, DELETE, PATCH)进行验证
 * 2. 从请求头 X-CSRF-Token 获取客户端提交的 Token
 * 3. 从 Session 中获取服务器生成的 Token
 * 4. 验证两者是否匹配
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {

        String method = request.getMethod();

        // 1. 只对修改数据的请求验证 CSRF Token
        if (!method.matches("POST|PUT|DELETE|PATCH")) {
            return true;
        }

        // 2. 从请求头获取 CSRF Token
        String headerToken = request.getHeader("X-CSRF-Token");

        // 3. 从 Session 获取存储的 CSRF Token
        HttpSession session = request.getSession(false);
        String sessionToken = session != null
            ? (String) session.getAttribute("CSRF_TOKEN")
            : null;

        // 4. 验证 Token
        if (headerToken == null || sessionToken == null || !headerToken.equals(sessionToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF Token验证失败\",\"data\":null}");
            return false;
        }

        return true;
    }
}
