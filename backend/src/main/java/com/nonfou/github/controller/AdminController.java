package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 极简管理后台接口，只保留登录能力。
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || password == null) {
                return Result.error("用户名和密码不能为空");
            }

            return Result.success(adminService.login(username, password));
        } catch (Exception e) {
            log.error("管理员登录失败", e);
            return Result.error(e.getMessage());
        }
    }
}
