# 认证授权模块

## 📖 概述

认证授权模块负责用户身份验证和权限管理，采用无密码登录方式（邮箱验证码），结合 JWT Token 和 API Key 双重认证机制。

## 🎯 核心功能

- 邮箱验证码登录/注册
- JWT Token 认证
- API Key 认证
- 多 API Key 管理
- 角色权限控制（USER/ADMIN）

## 🔐 认证流程

### 1. 用户登录流程

```
用户
  ↓
[1] 输入邮箱
  ↓
[2] 后端发送验证码到邮箱 (有效期 5 分钟)
  ↓
[3] 用户输入验证码
  ↓
[4] 后端验证验证码
  ↓         ↙ 验证成功
[5] 查询用户是否存在
  ↓         ↙ 不存在        ↘ 存在
[6] 创建新用户              [7] 获取现有用户
  ├─ 生成初始 API Key
  └─ 设置初始余额为 0
  ↓
[8] 生成 JWT Token (有效期 7 天)
  ↓
[9] 返回 Token 和用户信息
```

### 2. API 请求认证流程

```
客户端请求
  ↓
判断接口类型
  ├─ 用户接口 (/api/user/*, /api/admin/*)
  │   ↓
  │ JwtAuthenticationFilter
  │   ├─ 提取 Authorization Header
  │   ├─ 解析 JWT Token
  │   ├─ 验证签名和有效期
  │   ├─ 提取用户信息 (userId, email, role)
  │   └─ 存入 SecurityContext
  │
  └─ 聊天接口 (/api/v1/chat/completions)
      ↓
    ChatAuthenticationFilter
      ├─ 提取 Authorization Header
      ├─ 验证 API Key 格式 (sk-xxx)
      ├─ 查询 API Key 是否存在
      ├─ 验证 API Key 状态
      ├─ 更新最后使用时间
      └─ 提取用户信息
```

## 📝 核心组件

### 1. AuthController - 认证控制器

**位置**: `com.nonfou.github.controller.AuthController`

**核心接口**:
- **POST /api/auth/send-code**: 发送验证码
  - 接收邮箱地址
  - 生成6位随机验证码
  - 发送到用户邮箱
  - 返回发送成功状态

- **POST /api/auth/login**: 登录/注册
  - 接收邮箱和验证码
  - 验证验证码有效性
  - 查询或创建用户
  - 返回JWT Token和用户信息

- **GET /api/auth/test**: 测试接口
  - 验证认证系统是否正常
  - 返回测试成功信息

---

### 2. AuthService - 认证服务

**位置**: `com.nonfou.github.service.AuthService`

**核心方法**:

#### sendVerifyCode(String email)
发送验证码到用户邮箱:
1. 生成6位随机数字验证码
2. 将验证码存储到Redis，键格式: `verify_code:{email}`
3. 设置过期时间为5分钟
4. 调用邮件服务发送验证码

#### login(String email, String code)
验证码登录流程:
1. 从Redis获取存储的验证码，键: `verify_code:{email}`
2. 比较用户输入的验证码与缓存验证码
3. 验证失败抛出"验证码错误或已过期"异常
4. 验证成功后根据邮箱查询用户
5. 如果用户不存在，调用createNewUser创建新用户
6. 使用JwtUtil生成JWT Token
7. 删除Redis中的验证码
8. 返回LoginResponse(包含token和用户信息)

#### createNewUser(String email)
创建新用户流程:
1. 创建User对象，设置邮箱
2. 设置初始余额为0
3. 设置状态为1(正常)
4. 插入users表
5. 创建默认API Key:
   - 密钥名称: "默认密钥"
   - 调用generateApiKey生成API Key
   - 状态设置为1(启用)
   - 插入api_keys表
6. 返回创建的用户对象

#### validateApiKey(String apiKey)
验证API Key:
- 根据API Key查询数据库
- 验证Key是否存在
- 验证Key状态是否启用
- 返回ApiKey对象

#### generateToken(User user)
生成JWT Token:
- 将用户信息封装到token中
- 设置有效期
- 使用密钥签名
- 返回token字符串

---

### 3. JwtAuthenticationFilter - JWT 认证过滤器

**位置**: `com.nonfou.github.interceptor.JwtAuthenticationFilter`

**实现机制**:

继承OncePerRequestFilter，确保每个请求只过滤一次。

**doFilterInternal 方法流程**:

1. **提取Authorization Header**
   - 从请求头获取Authorization字段
   - 检查是否存在且以"Bearer "开头
   - 如果不存在，直接放行请求

2. **提取Token**
   - 从Authorization Header中截取Bearer后面的内容
   - 去掉"Bearer "前缀，获取纯token字符串

3. **验证并解析Token**
   - 使用JwtUtil.parseToken解析token
   - 验证签名是否正确
   - 验证是否过期
   - 解析失败抛出JwtException

4. **提取用户信息**
   - 从Claims中获取userId
   - 从Claims中获取email
   - 从Claims中获取role

5. **创建认证对象**
   - 创建UserDetails对象
   - 根据role获取权限列表
   - 创建UsernamePasswordAuthenticationToken
   - 设置认证详情

6. **设置到SecurityContext**
   - 将认证对象存入SecurityContextHolder
   - 后续接口可从SecurityContext获取用户信息

7. **异常处理**
   - 捕获JwtException
   - 返回401状态码
   - 返回JSON错误信息: "Token无效或已过期"

8. **继续过滤链**
   - 调用filterChain.doFilter继续处理请求

---

### 4. API Key 管理

**控制器**: `ApiKeyController`

**功能模块**:
- **创建API Key**: 生成新的API Key并保存
- **查询API Key列表**: 获取用户所有API Key
- **更新API Key状态**: 启用或禁用指定Key
- **删除API Key**: 从数据库删除Key
- **重新生成API Key**: 为已有Key生成新的Key值

**API Key 格式**:
- 前缀: `sk-`
- 长度: 32位随机字符
- 生成方法: UUID去掉连字符后拼接
- 示例: `sk-9012abcd3456efgh7890ijkl1234mnop`

---

## 🔒 JWT Token 设计

### Token 结构

JWT Token由三部分组成:

**Header (头部)**:
- alg: 签名算法，使用HS256
- typ: Token类型，值为JWT

**Payload (载荷)**:
- userId: 用户ID
- email: 用户邮箱
- role: 用户角色(USER/ADMIN)
- iat: 签发时间戳
- exp: 过期时间戳

**Signature (签名)**:
- 使用密钥对header和payload进行签名
- 确保token未被篡改

### Token 生成

**JwtUtil.generateToken 方法**:

1. 创建claims映射，存入用户信息:
   - userId: 用户ID
   - email: 用户邮箱
   - role: 用户角色

2. 使用Jwts.builder构建token:
   - setClaims: 设置载荷数据
   - setIssuedAt: 设置签发时间为当前时间
   - setExpiration: 设置过期时间(当前时间 + expiration配置)
   - signWith: 使用HS256算法和密钥签名
   - compact: 生成最终的token字符串

**解析Token**:
- 使用Jwts.parser解析token
- 设置签名密钥
- 调用parseClaimsJws验证签名
- 获取Claims对象

**检查过期**:
- 从Claims获取过期时间
- 与当前时间比较
- 判断是否已过期

---

## 🛡️ 权限控制

### 角色定义

**Role 枚举**:
- **USER**: 普通用户，可访问用户接口
- **ADMIN**: 管理员，可访问管理接口

### 权限注解

**@RequireAdmin 注解**:
- 目标: 方法级别
- 保留策略: 运行时
- 用途: 标记需要管理员权限的接口

### AOP 权限检查

**AdminAuthAspect 切面**:

1. **拦截@RequireAdmin注解的方法**
   - 使用@Around环绕通知
   - 在方法执行前进行权限检查

2. **获取当前用户认证信息**
   - 从SecurityContextHolder获取Authentication
   - 如果为null，抛出"未登录"异常(401)

3. **检查角色权限**
   - 从Authentication获取authorities
   - 遍历权限列表
   - 检查是否包含ROLE_ADMIN权限
   - 如果不是管理员，抛出"无权限访问"异常(403)

4. **继续执行方法**
   - 权限检查通过后，调用joinPoint.proceed()
   - 返回方法执行结果

---

## 📧 邮件服务

### EmailService - 邮件发送服务

**sendVerifyCode 方法**:

1. 创建MimeMessage消息对象
2. 使用MimeMessageHelper包装消息
3. 设置发件人(from)
4. 设置收件人(to)
5. 设置邮件主题: "AI API Platform - 验证码"
6. 调用buildVerifyCodeEmail构建HTML邮件内容
7. 使用JavaMailSender发送邮件
8. 捕获异常抛出"邮件发送失败"业务异常

**buildVerifyCodeEmail 方法**:

构建HTML格式的验证码邮件:
- 标题: "AI API Platform 登录验证码"
- 验证码显示: 大号红色字体突出显示
- 有效期提示: 5分钟有效
- 安全提示: 提醒勿泄露验证码
- 防误操作提示: 非本人操作可忽略

### 验证码服务

**VerifyCodeService 核心方法**:

#### generateCode()
生成随机验证码:
- 使用Random生成6位数字
- 每位数字范围0-9
- 拼接成字符串返回

#### saveCode(String email, String code)
保存验证码到Redis:
- Redis键格式: `verify_code:{email}`
- 存储值: 验证码字符串
- TTL: 5分钟
- 使用TimeUnit.MINUTES设置过期单位

#### verifyCode(String email, String code)
验证验证码:
- 根据邮箱从Redis获取缓存的验证码
- 比较输入验证码与缓存验证码
- 相等返回true，否则返回false

#### deleteCode(String email)
删除验证码:
- 根据邮箱构建Redis键
- 调用redisTemplate.delete删除
- 验证成功后清理，防止重复使用

---

## 🔐 安全特性

### 1. 防暴力破解

**RateLimitService 速率限制服务**:

**checkRateLimit 方法**:
- Redis键格式: `rate_limit:{email}`
- 获取尝试次数
- 如果达到最大尝试次数(5次)，抛出异常
- 提示用户15分钟后重试

**incrementAttempts 方法**:
- 使用Redis INCR命令增加尝试次数
- 设置过期时间15分钟
- 自动清理过期数据

**resetAttempts 方法**:
- 验证成功后重置尝试次数
- 删除Redis中的速率限制键

### 2. API Key 脱敏

**maskApiKey 方法**:

脱敏规则:
- 检查Key长度是否小于12位
- 保留前7位(包含sk-前缀)
- 保留后4位
- 中间部分用`****`替换

示例:
- 原始: `sk-9012abcd3456efgh7890ijkl1234mnop`
- 脱敏: `sk-9012****mnop`

用途:
- 前端显示API Key列表
- 日志记录
- 避免完整Key泄露

### 3. 敏感信息加密

**EncryptionUtil 加密工具**:

**encrypt 方法** - 加密:
1. 从配置读取32字符的密钥
2. 创建SecretKeySpec对象，算法AES
3. 获取Cipher实例，模式AES/GCM/NoPadding
4. 初始化加密模式
5. 加密明文字节数组
6. Base64编码加密结果
7. 返回编码后的字符串

**decrypt 方法** - 解密:
1. 创建SecretKeySpec对象
2. 获取Cipher实例
3. 初始化解密模式
4. Base64解码加密文本
5. 解密字节数组
6. 转换为字符串返回

**应用场景**:
- 后端账户Token加密存储
- 敏感配置信息保护
- 避免明文泄露风险

---

## 📊 数据模型

### User 实体

**表名**: users

**字段说明**:
- **id**: 主键ID，Long类型
- **email**: 邮箱地址，登录凭证
- **password**: 密码字段(无密码登录时为空)
- **balance**: 账户余额，BigDecimal类型
- **status**: 状态，1-正常，0-禁用
- **createdAt**: 创建时间
- **updatedAt**: 更新时间

### ApiKey 实体

**表名**: api_keys

**字段说明**:
- **id**: 主键ID，Long类型
- **userId**: 用户ID，外键关联users表
- **keyName**: 密钥名称，用户自定义
- **apiKey**: API Key字符串，sk-开头
- **status**: 状态，1-启用，0-禁用
- **lastUsedAt**: 最后使用时间，每次调用更新
- **createdAt**: 创建时间
- **updatedAt**: 更新时间

---

## 🧪 使用示例

### 1. 登录流程

**步骤1: 发送验证码**
- 接口: POST /api/auth/send-code
- 请求头: Content-Type: application/json
- 请求体: {"email":"user@example.com"}
- 响应: 发送成功提示

**步骤2: 登录**
- 接口: POST /api/auth/login
- 请求头: Content-Type: application/json
- 请求体: {"email":"user@example.com","code":"123456"}
- 响应: 包含token和用户信息的JSON

### 2. 使用 JWT Token

- 接口: GET /api/user/info
- 请求头: Authorization: Bearer {token}
- 说明: token从登录接口获取
- 响应: 返回用户详细信息

### 3. 使用 API Key

- 接口: POST /api/v1/chat/completions
- 请求头:
  - Authorization: Bearer sk-xxx
  - Content-Type: application/json
- 请求体: 包含model和messages的聊天请求
- 说明: API Key从用户后台获取

---

## ⚙️ 配置

### application.yml 配置说明

**JWT 配置**:
- jwt.secret: JWT签名密钥，至少256位
- jwt.expiration: Token有效期，单位毫秒，默认7天(604800000ms)

**邮件配置**:
- spring.mail.host: SMTP服务器地址
- spring.mail.port: SMTP端口，587
- spring.mail.username: 发件邮箱地址
- spring.mail.password: 邮箱授权码(非登录密码)
- spring.mail.properties.mail.smtp.auth: 启用SMTP认证
- spring.mail.properties.mail.smtp.starttls.enable: 启用TLS加密
- spring.mail.properties.mail.smtp.starttls.required: 要求TLS加密

**加密密钥配置**:
- encryption.secret-key: AES-256加密密钥，32个字符

---

## 🔍 常见问题

### Q: 为什么使用无密码登录？

A:
- 提升用户体验，减少注册流程
- 避免弱密码问题
- 减少密码泄露风险
- 简化密码找回流程

### Q: JWT Token 可以续期吗？

A: 目前不支持自动续期，Token 过期后需要重新登录。未来可以考虑实现刷新 Token 机制。

### Q: API Key 有什么限制？

A:
- 单用户最多 10 个 Key
- Key 格式固定：sk-{32位字符}
- 禁用后立即失效
- 删除后无法恢复

### Q: 验证码收不到怎么办？

A:
- 检查邮箱地址是否正确
- 查看垃圾邮件文件夹
- 确认邮件服务器配置正确
- 检查发送频率限制

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
