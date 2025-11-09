package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.LoginRequest;
import com.nonfou.github.dto.request.SendCodeRequest;
import com.nonfou.github.dto.response.LoginResponse;
import com.nonfou.github.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("API 正常运行");
    }
}
