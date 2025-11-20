package com.nonfou.github.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.dto.request.LoginRequest;
import com.nonfou.github.dto.request.SendCodeRequest;
import com.nonfou.github.dto.response.LoginResponse;
import com.nonfou.github.entity.User;
import com.nonfou.github.mapper.UserMapper;
import com.nonfou.github.util.JwtUtil;
import com.nonfou.github.util.LogMaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户认证服务
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VerifyCodeService verifyCodeService;

    /**
     * 发送验证码
     */
    public void sendCode(SendCodeRequest request) {
        String email = request.getEmail();

        verifyCodeService.ensureCanSend(email);

        // 生成验证码
        String code = emailService.generateCode();

        // 发送邮件
        emailService.sendVerifyCode(email, code);
        verifyCodeService.persistCode(email, code);

        log.info("验证码已发送: {}", LogMaskUtil.maskEmail(email));
    }

    /**
     * 登录/注册
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (!verifyCodeService.verifyCode(email, code)) {
            log.warn("验证码校验失败: email={}", LogMaskUtil.maskEmail(email));
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查询用户
        User user = getUserByEmail(email);

        // 如果用户不存在，则注册
        if (user == null) {
            log.info("用户不存在，开始自动注册: {}", LogMaskUtil.maskEmail(email));
            user = registerUser(email);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("账户已被禁用: {}", LogMaskUtil.maskEmail(email));
            throw new RuntimeException("账户已被禁用");
        }

        // 生成 JWT Token (USER 角色)
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), "USER");
        log.info("用户登录成功: userId={}, email={}, role=USER", user.getId(), LogMaskUtil.maskEmail(email));

        // 构建响应
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .apiKey(user.getApiKey())
                .balance(user.getBalance())
                .build();
    }

    /**
     * 根据邮箱查询用户
     */
    private User getUserByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 注册新用户
     */
    private User registerUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setApiKey(generateApiKey());
        user.setBalance(BigDecimal.ZERO);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        log.info("新用户注册成功: {}", LogMaskUtil.maskEmail(email));

        return user;
    }

    /**
     * 生成 API Key
     */
    private String generateApiKey() {
        return "sk-" + IdUtil.fastSimpleUUID();
    }
}
