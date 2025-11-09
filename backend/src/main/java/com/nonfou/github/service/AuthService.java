package com.nonfou.github.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.dto.request.LoginRequest;
import com.nonfou.github.dto.request.SendCodeRequest;
import com.nonfou.github.dto.response.LoginResponse;
import com.nonfou.github.entity.User;
import com.nonfou.github.mapper.UserMapper;
import com.nonfou.github.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired(required = false)
    private RedisService redisService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${test.auth.fixed-code:123456}")
    private String testFixedCode;

    /**
     * 发送验证码
     */
    public void sendCode(SendCodeRequest request) {
        String email = request.getEmail();

        // 生成验证码
        String code = emailService.generateCode();

        // 保存到 Redis（5分钟过期）
        if (redisService != null) {
            redisService.saveVerifyCode(email, code);
        }

        // 发送邮件
        emailService.sendVerifyCode(email, code);

        log.info("验证码已发送: {}", email);
    }

    /**
     * 登录/注册
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        // 验证验证码
        if (redisService != null) {
            // 生产环境：从Redis验证
            String savedCode = redisService.getVerifyCode(email);
            if (savedCode == null) {
                log.warn("验证码已过期或不存在: {}", email);
                throw new RuntimeException("验证码已过期");
            }
            if (!savedCode.equals(code)) {
                log.warn("验证码错误: email={}, expected={}, actual={}", email, savedCode, code);
                throw new RuntimeException("验证码错误");
            }
            // 删除验证码（一次性使用）
            redisService.deleteVerifyCode(email);
            log.info("验证码验证成功: {}", email);
        } else {
            // 测试环境：使用固定验证码（仅用于开发测试）
            if (!testFixedCode.equals(code)) {
                log.warn("测试环境验证码错误: email={}, expected={}, actual={}", email, testFixedCode, code);
                throw new RuntimeException("验证码错误");
            }
            log.info("测试环境验证码验证成功: {}", email);
        }

        // 查询用户
        User user = getUserByEmail(email);

        // 如果用户不存在，则注册
        if (user == null) {
            log.info("用户不存在，开始自动注册: {}", email);
            user = registerUser(email);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("账户已被禁用: {}", email);
            throw new RuntimeException("账户已被禁用");
        }

        // 生成 JWT Token (USER 角色)
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), "USER");
        log.info("用户登录成功: userId={}, email={}, role=USER", user.getId(), email);

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
        log.info("新用户注册成功: {}", email);

        return user;
    }

    /**
     * 生成 API Key
     */
    private String generateApiKey() {
        return "sk-" + IdUtil.fastSimpleUUID();
    }
}
