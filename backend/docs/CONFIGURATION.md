# 配置参考

## 📋 概述

本文档详细说明 AI API Platform 的所有配置项，包括数据库、Redis、邮件、JWT、支付等模块的配置。

## 🗂️ 配置文件结构

```
backend/src/main/resources/
├── application.yml           # 主配置文件
├── application-dev.yml       # 开发环境配置
├── application-prod.yml      # 生产环境配置
└── .env                      # 环境变量（不纳入版本控制）
```

## 📝 环境变量配置

### .env 文件示例

```properties
# ==================== 数据库配置 ====================
DB_URL=jdbc:mysql://localhost:3306/ai_api_platform?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=your_db_password

# ==================== Redis 配置 ====================
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# ==================== JWT 配置 ====================
# 密钥长度至少 256 位
JWT_SECRET=your-jwt-secret-key-at-least-256-bits-long-please-replace-this
# Token 有效期（毫秒），默认 7 天
JWT_EXPIRATION=604800000

# ==================== 邮件配置 ====================
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-email@qq.com
# QQ 邮箱使用授权码，不是密码
MAIL_PASSWORD=your-authorization-code
MAIL_SSL_ENABLE=false
MAIL_STARTTLS_ENABLE=true

# ==================== 支付宝配置 ====================
ALIPAY_APP_ID=your_app_id
ALIPAY_PRIVATE_KEY=your_private_key
ALIPAY_PUBLIC_KEY=alipay_public_key
ALIPAY_GATEWAY_URL=https://openapi.alipay.com/gateway.do
ALIPAY_NOTIFY_URL=http://yourdomain.com/api/recharge/alipay/notify
ALIPAY_RETURN_URL=http://yourdomain.com/api/recharge/alipay/return

# ==================== 微信支付配置 ====================
WECHAT_APP_ID=your_wechat_app_id
WECHAT_MCH_ID=your_merchant_id
WECHAT_API_V3_KEY=your_api_v3_key
WECHAT_MERCHANT_SERIAL_NUMBER=your_serial_number
WECHAT_PRIVATE_KEY_PATH=/path/to/apiclient_key.pem
WECHAT_NOTIFY_URL=http://yourdomain.com/api/recharge/wechat/notify

# ==================== 后端 AI 服务配置 ====================
COPILOT_BASE_URL=https://api.githubcopilot.com
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1

# ==================== 加密密钥 ====================
# AES-256 密钥，32 字符
ENCRYPTION_SECRET_KEY=your-aes-256-secret-key-32chars

# ==================== 管理员配置 ====================
ADMIN_DEFAULT_USERNAME=admin
ADMIN_DEFAULT_PASSWORD=admin123
```

## ⚙️ application.yml 配置详解

### 1. Spring Boot 基础配置

```yaml
spring:
  application:
    name: AI API Platform

  # 环境配置
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  # 数据源配置
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

    # HikariCP 连接池配置
    hikari:
      # 最大连接数
      maximum-pool-size: 20
      # 最小空闲连接数
      minimum-idle: 5
      # 连接超时时间（毫秒）
      connection-timeout: 30000
      # 空闲超时时间（毫秒）
      idle-timeout: 600000
      # 最大生命周期（毫秒）
      max-lifetime: 1800000
      # 连接测试查询
      connection-test-query: SELECT 1
      # 连接池名称
      pool-name: HikariPool-AIService

  # Redis 配置
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 1000ms

  # 邮件配置
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: ${MAIL_STARTTLS_ENABLE:true}
            required: ${MAIL_STARTTLS_ENABLE:true}
          ssl:
            enable: ${MAIL_SSL_ENABLE:false}
    default-encoding: UTF-8

  # Jackson 配置
  jackson:
    time-zone: Asia/Shanghai
    date-format: yyyy-MM-dd HH:mm:ss
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
```

### 2. MyBatis-Plus 配置

```yaml
mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath*:/mapper/**/*.xml

  # 实体类包路径
  type-aliases-package: com.nonfou.github.entity

  # 全局配置
  global-config:
    db-config:
      # 主键类型：AUTO-数据库自增
      id-type: AUTO
      # 逻辑删除字段
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      # 字段填充策略
      insert-strategy: NOT_NULL
      update-strategy: NOT_NULL

  # MyBatis 配置
  configuration:
    # 驼峰命名转换
    map-underscore-to-camel-case: true
    # 缓存配置
    cache-enabled: true
    # 延迟加载
    lazy-loading-enabled: true
    # 日志实现
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 3. JWT 配置

```yaml
jwt:
  # 密钥（必须至少 256 位）
  secret: ${JWT_SECRET}

  # Token 有效期（毫秒）
  # 7 天 = 604800000
  # 30 天 = 2592000000
  expiration: ${JWT_EXPIRATION:604800000}

  # Token 前缀
  token-prefix: "Bearer "

  # Header 名称
  header: "Authorization"
```

### 4. 支付宝配置

```yaml
alipay:
  # 应用 ID
  app-id: ${ALIPAY_APP_ID}

  # 应用私钥
  private-key: ${ALIPAY_PRIVATE_KEY}

  # 支付宝公钥
  public-key: ${ALIPAY_PUBLIC_KEY}

  # 网关地址
  gateway-url: ${ALIPAY_GATEWAY_URL:https://openapi.alipay.com/gateway.do}

  # 编码格式
  charset: UTF-8

  # 签名类型
  sign-type: RSA2

  # 数据格式
  format: json

  # 异步通知地址
  notify-url: ${ALIPAY_NOTIFY_URL}

  # 同步跳转地址
  return-url: ${ALIPAY_RETURN_URL}
```

### 5. 微信支付配置

```yaml
wechat:
  pay:
    # 应用 ID
    app-id: ${WECHAT_APP_ID}

    # 商户号
    mch-id: ${WECHAT_MCH_ID}

    # API v3 密钥
    api-v3-key: ${WECHAT_API_V3_KEY}

    # 商户证书序列号
    merchant-serial-number: ${WECHAT_MERCHANT_SERIAL_NUMBER}

    # 商户私钥路径
    private-key-path: ${WECHAT_PRIVATE_KEY_PATH}

    # 异步通知地址
    notify-url: ${WECHAT_NOTIFY_URL}

    # 证书自动更新
    auto-update-cert: true
```

### 6. 后端服务配置

```yaml
backend:
  # Copilot 配置
  copilot:
    base-url: ${COPILOT_BASE_URL:https://api.githubcopilot.com}
    timeout: 60000

  # OpenRouter 配置
  openrouter:
    base-url: ${OPENROUTER_BASE_URL:https://openrouter.ai/api/v1}
    timeout: 60000

  # 路由配置
  routing:
    # 默认提供商
    default-provider: copilot

  # 调度器配置
  scheduler:
    # 调度策略：ROUND_ROBIN, LEAST_USED, PRIORITY, COST_OPTIMIZED, HYBRID
    strategy: HYBRID

    # 会话粘性配置
    session-stickiness:
      enabled: true
      ttl-hours: 1

    # 健康检查配置
    health-check:
      enabled: true
      interval-seconds: 30
      failure-threshold: 3

    # 混合策略权重
    hybrid-weights:
      priority: 0.3
      usage: 0.3
      error: 0.2
      cost: 0.2

  # 计费配置
  pricing:
    # 平台加成倍率（1.2 = 20% 利润）
    markup-rate: 1.2

    # 是否启用账户成本倍率
    enable-account-multiplier: true

    # 是否启用模型价格倍率
    enable-model-multiplier: true

    # 汇率（美元转人民币）
    exchange-rate: 7.2

  # 配额配置
  quota:
    # 默认每日限额（元）
    default-daily-limit: 100.00

    # 默认每月限额（元）
    default-monthly-limit: 2000.00

    # 是否强制执行配额
    enforce: true

    # 告警阈值（百分比）
    alert-threshold: 80.0
```

### 7. 日志配置

```yaml
logging:
  level:
    root: INFO
    com.nonfou.github: DEBUG
    org.springframework.web: INFO
    org.springframework.security: DEBUG
    com.zaxxer.hikari: INFO

  # 日志文件配置
  file:
    name: logs/ai-api-platform.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 3GB

  # 日志格式
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n%wEx"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
```

### 8. Spring Boot Actuator 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露的端点
        include: health,info,metrics,prometheus
      base-path: /actuator

  endpoint:
    health:
      # 显示详细健康信息
      show-details: when-authorized
      show-components: always

  # 指标配置
  metrics:
    export:
      prometheus:
        enabled: true

  # 健康检查配置
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

### 9. 服务器配置

```yaml
server:
  # 端口
  port: ${SERVER_PORT:8080}

  # 上下文路径
  servlet:
    context-path: /

  # Tomcat 配置
  tomcat:
    # 最大线程数
    threads:
      max: 200
      min-spare: 10

    # 最大连接数
    max-connections: 10000

    # 接受队列大小
    accept-count: 100

    # 连接超时（毫秒）
    connection-timeout: 20000

    # 最大请求体大小
    max-http-form-post-size: 10MB
```

## 🔐 加密配置

### 加密密钥配置

```yaml
encryption:
  # AES-256 密钥（32 字符）
  secret-key: ${ENCRYPTION_SECRET_KEY}

  # 加密算法
  algorithm: AES/GCM/NoPadding

  # GCM 标签长度
  gcm-tag-length: 128
```

## 📧 邮件服务配置

### 常见邮件服务器配置

#### QQ 邮箱
```properties
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=your-authorization-code  # 不是密码，是授权码
MAIL_STARTTLS_ENABLE=true
MAIL_SSL_ENABLE=false
```

#### 163 邮箱
```properties
MAIL_HOST=smtp.163.com
MAIL_PORT=25
MAIL_USERNAME=your-email@163.com
MAIL_PASSWORD=your-authorization-code
MAIL_STARTTLS_ENABLE=true
MAIL_SSL_ENABLE=false
```

#### Gmail
```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_STARTTLS_ENABLE=true
MAIL_SSL_ENABLE=false
```

#### 企业邮箱（腾讯）
```properties
MAIL_HOST=smtp.exmail.qq.com
MAIL_PORT=465
MAIL_USERNAME=your-email@yourcompany.com
MAIL_PASSWORD=your-password
MAIL_STARTTLS_ENABLE=false
MAIL_SSL_ENABLE=true
```

## 🔧 开发环境 vs 生产环境

### application-dev.yml (开发)

```yaml
spring:
  # 开发环境数据库
  datasource:
    url: jdbc:mysql://localhost:3306/ai_api_platform_dev
    hikari:
      maximum-pool-size: 10

  # 开发环境 Redis
  data:
    redis:
      host: localhost
      database: 1

# 开发环境日志
logging:
  level:
    root: DEBUG
    com.nonfou.github: DEBUG

# 开发环境不启用健康检查
backend:
  scheduler:
    health-check:
      enabled: false
```

### application-prod.yml (生产)

```yaml
spring:
  # 生产环境数据库
  datasource:
    url: ${DB_URL}
    hikari:
      maximum-pool-size: 50

  # 生产环境 Redis
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}

# 生产环境日志
logging:
  level:
    root: INFO
    com.nonfou.github: INFO

# 生产环境启用所有监控
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

# 生产环境完整配置
backend:
  scheduler:
    strategy: HYBRID
    health-check:
      enabled: true
      interval-seconds: 30
```

## 🌍 多环境切换

### 方式一：环境变量

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

### 方式二：启动参数

```bash
java -jar app.jar --spring.profiles.active=prod
```

### 方式三：配置文件

```bash
java -Dspring.profiles.active=prod -jar app.jar
```

## 📋 配置验证清单

部署前请确认以下配置：

**数据库**:
- [ ] 数据库地址和端口正确
- [ ] 用户名和密码正确
- [ ] 数据库已创建
- [ ] 初始化脚本已执行
- [ ] 连接池参数合理

**Redis**:
- [ ] Redis 地址和端口正确
- [ ] 密码配置正确（如有）
- [ ] 数据库编号正确

**JWT**:
- [ ] Secret 长度至少 256 位
- [ ] Secret 已更换（不使用默认值）
- [ ] Token 有效期合理

**邮件**:
- [ ] SMTP 服务器配置正确
- [ ] 使用授权码而非密码（QQ/163）
- [ ] SSL/STARTTLS 配置正确
- [ ] 发件人邮箱已验证

**支付**:
- [ ] 支付宝/微信支付参数已配置
- [ ] 回调地址可公网访问
- [ ] 证书文件路径正确

**后端服务**:
- [ ] AI 服务地址可访问
- [ ] 后端账户已添加
- [ ] Token 已加密存储

**安全**:
- [ ] 所有默认密码已更换
- [ ] 加密密钥已生成
- [ ] 敏感配置不在代码仓库中

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
