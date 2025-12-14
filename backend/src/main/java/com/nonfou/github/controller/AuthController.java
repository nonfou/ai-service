package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.LoginRequest;
import com.nonfou.github.dto.request.SendCodeRequest;
import com.nonfou.github.dto.response.LoginResponse;
import com.nonfou.github.dto.response.LoginStatusResponse;
import com.nonfou.github.entity.User;
import com.nonfou.github.service.AuthService;
import com.nonfou.github.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    private static final String AUTH_TOKEN_COOKIE_NAME = "auth_token";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7天

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody @Validated SendCodeRequest request) {
        authService.sendCode(request);
        return Result.success();
    }

    /**
     * 登录/注册
     * 改造: 将Token设置到HttpOnly Cookie,响应中不返回token字段
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @RequestBody @Validated LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(request);

        // 将Token设置到HttpOnly Cookie
        String cookieValue = String.format(
                "%s=%s; HttpOnly; %s SameSite=Strict; Path=/; Max-Age=%d",
                AUTH_TOKEN_COOKIE_NAME,
                loginResponse.getToken(),
                cookieSecure ? "Secure;" : "",
                COOKIE_MAX_AGE
        );
        response.addHeader("Set-Cookie", cookieValue);

        log.info("用户登录成功,Token已设置到Cookie: userId={}", loginResponse.getUserId());

        // 响应中移除token字段(设置为null)
        loginResponse.setToken(null);

        return Result.success(loginResponse);
    }

    /**
     * 登出
     * 清除HttpOnly Cookie
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        // 清除Cookie (设置MaxAge为0)
        String cookieValue = String.format(
                "%s=; HttpOnly; %s SameSite=Strict; Path=/; Max-Age=0",
                AUTH_TOKEN_COOKIE_NAME,
                cookieSecure ? "Secure;" : ""
        );
        response.addHeader("Set-Cookie", cookieValue);

        log.info("用户登出成功,Cookie已清除");
        return Result.success();
    }

    /**
     * 检查登录状态
     * 从Cookie中读取Token并验证
     */
    @GetMapping("/status")
    public Result<LoginStatusResponse> checkStatus(HttpServletRequest request) {
        // 从Cookie中获取Token
        String token = extractTokenFromCookie(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            return Result.success(LoginStatusResponse.builder()
                    .isLoggedIn(false)
                    .build());
        }

        // 获取用户信息
        String email = jwtUtil.getEmailFromToken(token);
        User user = authService.getUserByEmail(email);

        if (user == null || user.getStatus() == 0) {
            return Result.success(LoginStatusResponse.builder()
                    .isLoggedIn(false)
                    .build());
        }

        // 返回登录状态和用户信息
        return Result.success(LoginStatusResponse.builder()
                .isLoggedIn(true)
                .userInfo(LoginStatusResponse.UserInfo.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .username(user.getEmail()) // 如果有username字段可使用
                        .balance(user.getBalance())
                        .build())
                .build());
    }

    /**
     * 从Cookie中提取Token
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("API 正常运行");
    }
}
