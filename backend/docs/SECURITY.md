# 安全设计

## 🔐 概述

AI API Platform 采用多层安全防护措施，保护用户数据和系统安全。

## 🛡️ 认证机制

### 1. JWT Token 认证

**安全特性**:
- 无状态认证，不存储在服务器
- 使用 HS256 算法签名
- Token 包含有效期，默认 7 天
- 密钥长度至少 256 位

**安全建议**:
```yaml
jwt:
  # 使用强随机密钥
  secret: $(openssl rand -base64 32)

  # 合理设置有效期
  expiration: 604800000  # 7天
```

### 2. API Key 认证

**格式**: `sk-{32位随机字符}`

**安全特性**:
- 使用 UUID 生成，随机性强
- 支持多密钥管理
- 可独立禁用/删除
- 显示时脱敏处理

**最佳实践**:
- 定期轮换 API Key
- 不同环境使用不同 Key
- 泄露后立即禁用

## 🔒 数据加密

### 1. 密码加密

**算法**: BCrypt

```java
// 加密
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));

// 验证
boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
```

**特点**:
- 自动加盐
- 计算成本可配置
- 抗彩虹表攻击

### 2. 敏感数据加密

**算法**: AES-256-GCM

**加密内容**:
- 后端账户 Token
- 支付密钥
- 其他敏感配置

```java
@Component
public class EncryptionUtil {

    public String encrypt(String plainText) {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedText) {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted);
    }
}
```

### 3. 数据传输加密

**HTTPS/TLS**:
- 生产环境强制 HTTPS
- TLS 1.2 及以上版本
- 使用受信任的 SSL 证书

## 🚫 权限控制

### 1. 基于角色的访问控制（RBAC）

**角色定义**:
- `USER`: 普通用户
- `ADMIN`: 管理员

**实现方式**:
```java
@RequireAdmin
@GetMapping("/api/admin/users")
public Result<?> listUsers() {
    // 只有管理员可以访问
}
```

### 2. API 访问控制

**用户隔离**:
- 用户只能访问自己的数据
- 通过 JWT 中的 userId 验证

```java
public List<ApiCall> getUserApiCalls(Long userId) {
    // 从 SecurityContext 获取当前用户
    Long currentUserId = SecurityUtils.getCurrentUserId();

    // 验证权限
    if (!currentUserId.equals(userId)) {
        throw new ForbiddenException("无权访问其他用户数据");
    }

    return apiCallMapper.selectByUserId(userId);
}
```

## 🛠️ 输入验证

### 1. 参数验证

使用 JSR-303 Bean Validation:

```java
@Data
public class LoginRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字")
    private String code;
}
```

### 2. SQL 注入防护

**使用 MyBatis-Plus 参数化查询**:

```java
// 安全：参数化查询
User user = userMapper.selectOne(
    new LambdaQueryWrapper<User>()
        .eq(User::getEmail, email)
);

// 不安全：字符串拼接（避免）
// "SELECT * FROM users WHERE email = '" + email + "'"
```

### 3. XSS 防护

**输出转义**:
```java
// 使用 HtmlUtils 转义
String safeHtml = HtmlUtils.htmlEscape(userInput);
```

**Content Security Policy**:
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.headers()
            .contentSecurityPolicy("default-src 'self'");
        return http.build();
    }
}
```

## 🚦 频率限制

### 1. 验证码发送限制

```java
public void checkSendCodeRateLimit(String email) {
    String key = "send_code_limit:" + email;
    Long count = redisTemplate.opsForValue().increment(key);

    if (count == 1) {
        redisTemplate.expire(key, 1, TimeUnit.MINUTES);
    }

    if (count > 1) {
        throw new BusinessException("验证码发送过于频繁，请1分钟后再试");
    }
}
```

### 2. 登录失败锁定

```java
public void checkLoginFailureLimit(String email) {
    String key = "login_failure:" + email;
    Integer failures = (Integer) redisTemplate.opsForValue().get(key);

    if (failures != null && failures >= 5) {
        throw new BusinessException("登录失败次数过多，账户已锁定15分钟");
    }
}

public void incrementLoginFailure(String email) {
    String key = "login_failure:" + email;
    redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, 15, TimeUnit.MINUTES);
}
```

### 3. API 速率限制

```java
@Aspect
@Component
public class RateLimitAspect {

    @Around("@annotation(RateLimit)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        String key = "rate_limit:" + SecurityUtils.getCurrentUserId();
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        if (count > rateLimit.value()) {
            throw new BusinessException("请求过于频繁");
        }

        return pjp.proceed();
    }
}
```

## 🔍 审计日志

### 1. 关键操作记录

**记录内容**:
- 用户登录/登出
- API Key 创建/删除
- 充值订单
- 账户余额变动
- 管理员操作

**实现**:
```java
@Aspect
@Component
public class AuditLogAspect {

    @AfterReturning("@annotation(Audit)")
    public void logAudit(JoinPoint jp, Audit audit) {
        AuditLog log = new AuditLog();
        log.setUserId(SecurityUtils.getCurrentUserId());
        log.setAction(audit.value());
        log.setIpAddress(RequestUtils.getClientIP());
        log.setCreatedAt(LocalDateTime.now());

        auditLogMapper.insert(log);
    }
}
```

### 2. API 调用日志

**记录字段**:
- 用户 ID
- API Key
- 模型名称
- Token 使用量
- 费用
- 请求时间
- 响应状态

## 🌐 CORS 配置

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://yourdomain.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

## 🔒 Session 安全

### 1. CSRF 防护

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            // JWT 无状态，可禁用 CSRF
            .csrf().disable()

            // 或使用 Cookie 时启用
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        return http.build();
    }
}
```

### 2. Clickjacking 防护

```java
http.headers()
    .frameOptions().deny()  // 禁止被嵌入 iframe
    .xssProtection()
    .and()
    .contentTypeOptions();
```

## 📊 安全监控

### 1. 异常登录检测

**监控指标**:
- 短时间内多次失败登录
- 异常 IP 登录
- 异常时间登录
- 异常地理位置登录

### 2. API 异常监控

**监控指标**:
- 大量 401/403 错误
- 异常高频请求
- 大额消费
- 错误率突增

### 3. 告警机制

```java
@Service
public class SecurityAlertService {

    public void sendAlert(String type, String message) {
        // 发送邮件告警
        emailService.sendAlert(adminEmail, type, message);

        // 记录告警日志
        log.warn("安全告警: type={}, message={}", type, message);
    }
}
```

## 🔧 安全配置清单

### 部署前检查

**密钥和密码**:
- [ ] JWT Secret 已更换（不使用默认值）
- [ ] 数据库密码强度足够
- [ ] Redis 密码已设置
- [ ] 加密密钥已生成
- [ ] 支付密钥已配置
- [ ] 管理员默认密码已修改

**网络安全**:
- [ ] 生产环境使用 HTTPS
- [ ] 防火墙已配置
- [ ] 数据库不对外暴露
- [ ] Redis 不对外暴露
- [ ] ��理后台访问已限制

**应用安全**:
- [ ] 参数验证已启用
- [ ] CORS 配置正确
- [ ] XSS 防护已启用
- [ ] SQL 注入防护已验证
- [ ] 频率限制已配置
- [ ] 审计日志已启用

**数据安全**:
- [ ] 敏感数据已加密
- [ ] 数据备份已配置
- [ ] 日志定期归档
- [ ] 数据库定期备份

### 定期检查

**每月**:
- 检查异常登录记录
- 审计管理员操作日志
- 检查大额异常消费
- 更新依赖版本

**每季度**:
- 轮换 API 密钥
- 审计权限配置
- 漏洞扫描
- 渗透测试

## 📚 安全资源

### OWASP Top 10

1. 注入攻击
2. 失效的身份认证
3. 敏感数据泄露
4. XML 外部实体（XXE）
5. 失效的访问控制
6. 安全配置错误
7. 跨站脚本（XSS）
8. 不安全的反序列化
9. 使用含有已知漏洞的组件
10. 不足的日志记录和监控

### 参考资料

- [OWASP 安全指南](https://owasp.org/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [Spring Security 文档](https://spring.io/projects/spring-security)

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
