# 系统架构设计

## 📐 架构概述

AI API Platform 采用经典的分层架构设计，结合领域驱动设计（DDD）的理念，通过合理的模块划分和设计模式，实现了高内聚、低耦合的系统架构。

## 🏗️ 整体架构

### 分层架构图

```
┌───────────────────────────────────────────────────────────────────────┐
│                            客户端层 (Client)                           │
│   Web前端 │ Mobile App │ 第三方应用 (OpenAI SDK) │ CLI 工具           │
└───────────────────────────────────────────────────────────────────────┘
                                   ↓ HTTP/HTTPS
┌───────────────────────────────────────────────────────────────────────┐
│                        网关层 (Gateway & Filter)                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ CORS 配置        │  │ JWT 认证过滤器    │  │ API Key 过滤器   │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ 全局异常处理      │  │ 请求日志拦截器    │  │ 限流拦截��       │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
                                   ↓
┌───────────────────────────────────────────────────────────────────────┐
│                         控制器层 (Controller)                          │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │
│  │ 用户控制器      │  │ 聊天控制器      │  │ 充值控制器      │         │
│  │ UserController │  │ ChatController │  │RechargeCtrl    │         │
│  └────────────────┘  └────────────────┘  └────────────────┘         │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │
│  │ 订阅控制器      │  │ 工单控制器      │  │ 管理控制器      │         │
│  │SubscriptionCtrl│  │ TicketCtrl     │  │ AdminCtrl      │         │
│  └────────────────┘  └────────────────┘  └────────────────┘         │
│               请求参数验证 │ DTO 转换 │ 响应封装                       │
└───────────────────────────────────────────────────────────────────────┘
                                   ↓
┌───────────────────────────────────────────────────────────────────────┐
│                        业务逻辑层 (Service)                            │
│                                                                       │
│  ┌─────────────────────── 核心服务 ─────────────────────────┐        │
│  │                                                           │        │
│  │  ┌───────────────────────────────────────────────┐       │        │
│  │  │        聊天编排服务 (ChatWorkflowService)      │       │        │
│  │  │  认证 → 配额检查 → 账户调度 → 代理调用         │       │        │
│  │  │         → Token计费 → 日志记录                 │       │        │
│  │  └───────────────────────────────────────────────┘       │        │
│  │                          ↓                                │        │
│  │  ┌──────────────────┐  ┌──────────────────┐             │        │
│  │  │ 账户调度服务      │  │ 会话粘性服务      │             │        │
│  │  │AccountScheduler  │  │SessionStickiness │             │        │
│  │  └──────────────────┘  └──────────────────┘             │        │
│  │                          ↓                                │        │
│  │  ┌──────────────────┐  ┌──────────────────┐             │        │
│  │  │ Copilot代理服务   │  │ OpenRouter代理   │             │        │
│  │  │ CopilotProxy     │  │ OpenRouterProxy  │             │        │
│  │  └──────────────────┘  └──────────────────┘             │        │
│  │            (实现 ModelProxy 接口)                         │        │
│  └───────────────────────────────────────────────────────────┘        │
│                                                                       │
│  ┌─────────────────────── 业务服务 ─────────────────────────┐        │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐         │        │
│  │  │ 用户服务    │  │ 计费服务    │  │ 支付服务    │         │        │
│  │  │UserService │  │BillingServ │  │PaymentServ │         │        │
│  │  └────────────┘  └────────────┘  └────────────┘         │        │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐         │        │
│  │  │ 订阅服务    │  │ 配额服务    │  │ 统计服务    │         │        │
│  │  │Subscription│  │QuotaService│  │StatsSvc    │         │        │
│  │  └────────────┘  └────────────┘  └────────────┘         │        │
│  └───────────────────────────────────────────────────────────┘        │
│                                                                       │
│  ┌─────────────────────── 基础服务 ─────────────────────────┐        │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐         │        │
│  │  │ 邮件服务    │  │ 验证码服务  │  │ Redis服务   │         │        │
│  │  │EmailServ   │  │VerifyCode  │  │RedisServ   │         │        │
│  │  └────────────┘  └────────────┘  └────────────┘         │        │
│  └───────────────────────────────────────────────────────────┘        │
└───────────────────────────────────────────────────────────────────────┘
                                   ↓
┌───────────────────────────────────────────────────────────────────────┐
│                      数据访问层 (Data Access)                          │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │
│  │ MyBatis Mapper │  │ Redis Template │  │ 事务管理        │         │
│  │ (16个Mapper)   │  │ (缓存操作)      │  │ Transaction    │         │
│  └────────────────┘  └────────────────┘  └────────────────┘         │
└───────────────────────────────────────────────────────────────────────┘
                                   ↓
┌───────────────────────────────────────────────────────────────────────┐
│                         数据存储层 (Storage)                           │
│  ┌────────────────────────────┐  ┌──────────────────────────┐        │
│  │        MySQL 8.0           │  │        Redis 6.0+        │        │
│  │  • 用户数据                 │  │  • 会话缓存              │        │
│  │  • 订单数据                 │  │  • 验证码                │        │
│  │  • 配置数据                 │  │  • 配额使用统计          │        │
│  │  • 日志数据                 │  │  • 账户健康状态          │        │
│  └────────────────────────────┘  └──────────────────────────┘        │
└───────────────────────────────────────────────────────────────────────┘
                                   ↓
┌───────────────────────────────────────────────────────────────────────┐
│                      外部服务集成层 (External)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ Copilot API │  │OpenRouter   │  │ 支付宝       │  │ 微信支付     │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘ │
│  ┌─────────────┐                                                     │
│  │ 邮件服务器   │                                                     │
│  └─────────────┘                                                     │
└───────────────────────────────────────────────────────────────────────┘
```

## 🎯 核心设计模式

### 1. 代理模式 (Proxy Pattern)

**应用场景**: AI 服务提供商的统一抽象

```java
// 代理接口
public interface ModelProxy {
    ChatResponse chat(ChatRequest request, BackendAccount account);
    StreamChatResponse chatStream(ChatRequest request, BackendAccount account);
}

// Copilot 实现
@Service
public class CopilotProxyService implements ModelProxy {
    @Override
    public ChatResponse chat(ChatRequest request, BackendAccount account) {
        // Copilot 特定实现
    }
}

// OpenRouter 实现
@Service
public class OpenRouterProxyService implements ModelProxy {
    @Override
    public ChatResponse chat(ChatRequest request, BackendAccount account) {
        // OpenRouter 特定实现
    }
}
```

**优势**:
- 隔离不同 AI 服务商的差异
- 方便新增其他服务商（如 Claude、Gemini 等）
- 统一的错误处理和重试逻辑

### 2. 策略模式 (Strategy Pattern)

**应用场景**: 账户调度策略

```java
public enum SchedulerStrategy {
    ROUND_ROBIN,      // 轮询
    LEAST_USED,       // 最少使用
    PRIORITY,         // 优先级
    COST_OPTIMIZED,   // 成本优化
    HYBRID            // 混合策略
}

@Service
public class AccountSchedulerService {
    public BackendAccount selectAccount(List<BackendAccount> accounts,
                                       SchedulerStrategy strategy,
                                       SchedulerContext context) {
        return switch (strategy) {
            case ROUND_ROBIN -> selectByRoundRobin(accounts);
            case LEAST_USED -> selectByLeastUsed(accounts);
            case PRIORITY -> selectByPriority(accounts);
            case COST_OPTIMIZED -> selectByCost(accounts, context);
            case HYBRID -> selectByHybrid(accounts, context);
        };
    }
}
```

**优势**:
- 灵活切换调度策略
- 易于添加新的调度算法
- 支持运行时动态配置

### 3. 编排模式 (Orchestration Pattern)

**应用场景**: 聊天请求处理流程编排

```java
@Service
public class ChatWorkflowService {

    public ChatResponse handleChat(ChatRequest request) {
        // 1. 认证和鉴权
        ApiKey apiKey = authenticate(request.getApiKey());

        // 2. 配额检查
        quotaService.checkQuota(apiKey.getUserId());

        // 3. 账户调度
        BackendAccount account = accountScheduler.selectAccount(
            request.getModel(),
            apiKey
        );

        // 4. 代理调用
        ModelProxy proxy = getProxy(account.getProvider());
        ChatResponse response = proxy.chat(request, account);

        // 5. Token 计费
        costCalculator.calculate(response);
        balanceService.deduct(apiKey.getUserId(), cost);

        // 6. 记录日志
        apiCallLogService.log(request, response, cost);

        return response;
    }
}
```

**优势**:
- 流程清晰，易于理解和维护
- 每个步骤可独立测试
- 支持流式和非流式两种模式

### 4. 观察者模式 (Observer Pattern)

**应用场景**: 账户健康检查和自动降级

```java
@Service
public class AccountHealthMonitor {

    private final List<AccountHealthListener> listeners = new ArrayList<>();

    @Scheduled(fixedDelay = 30000) // 每 30 秒检查一次
    public void checkHealth() {
        for (BackendAccount account : getAllAccounts()) {
            boolean healthy = performHealthCheck(account);

            if (!healthy) {
                notifyListeners(new AccountUnhealthyEvent(account));
            }
        }
    }

    private void notifyListeners(AccountUnhealthyEvent event) {
        listeners.forEach(listener -> listener.onAccountUnhealthy(event));
    }
}

@Component
public class AccountDowngradeListener implements AccountHealthListener {

    @Override
    public void onAccountUnhealthy(AccountUnhealthyEvent event) {
        // 降级账户
        backendAccountService.downgrade(event.getAccount());

        // 发送告警
        alertService.sendAlert(event);
    }
}
```

## 🔄 数据流转

### 聊天请求完整流程

```
用户请求
   ↓
[1] JWT/API Key 认证
   ↓
[2] 参数验证和转换
   ↓
[3] 配额检查
   ↓         ↙ 是
[4] 查询会话映射
   ↓         ↘ 否
[5] 账户调度选择
   ↓
[6] 获取对应代理服务
   ↓
[7] 调用上游 AI 服务
   ↓
[8] 流式/非流式响应处理
   ↓
[9] Token 统计
   ↓
[10] 成本计算
   ↓
[11] 余额扣除
   ↓
[12] 记录调用日志
   ↓
[13] 更新会话映射
   ↓
[14] 返回响应给用户
```

### 计费流程

```
API 调用完成
   ↓
[1] 统计 Token 使用
   ├─ 输入 Token
   ├─ 输出 Token
   ├─ 缓存读取 Token
   └─ 缓存写入 Token
   ↓
[2] 获取模型价格
   ├─ 输入价格 ($/1M tokens)
   ├─ 输出价格 ($/1M tokens)
   └─ 缓存价格
   ↓
[3] 计算原始成本
   cost = (inputTokens × inputPrice +
          outputTokens × outputPrice +
          cacheTokens × cachePrice) / 1,000,000
   ↓
[4] 应用账户倍率
   cost × account.costMultiplier
   ↓
[5] 应用模型倍率
   cost × model.priceMultiplier
   ↓
[6] 应用平台利润率
   cost × markupRate
   ↓
[7] 扣除用户余额
   ↓
[8] 记录余额变动日志
```

### 账户调度流程（混合策略）

```
收到聊天请求
   ↓
[1] 检查会话粘性
   ↓ 存在且健康
   使用已绑定账户
   ↓ 不存在
[2] 获取可用账户列表
   ├─ 过滤禁用账户
   ├─ 过滤达到并发限制账户
   ├─ 过滤达到速率限制账户
   └─ 过滤错误率过高账户
   ↓
[3] 应用混合策略评分
   score = w1×priority +
          w2×(1-usageRate) +
          w3×(1-errorRate) +
          w4×(1-costRate)
   ↓
[4] 选择得分最高账户
   ↓
[5] 检查并发和速率限制
   ↓
[6] 创建会话粘性映射
   ↓
返回选中账户
```

## 🔐 安全架构

### 认证授权流程

```
┌─────────────────────────────────────────┐
│          用户登录流程                    │
├─────────────────────────────────────────┤
│  1. 用户输入邮箱                         │
│  2. 后端发送验证码到邮箱                 │
│  3. 用户输入验证码                       │
│  4. 后端验证验证码                       │
│  5. 生成 JWT Token (有效期 7 天)        │
│  6. 返回 Token 和用户信息                │
└─────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────┐
│      API 请求认证流程                    │
├─────────────────────────────────────────┤
│  用户接口:                               │
│    Header: Authorization: Bearer {jwt}  │
│    → JwtAuthenticationFilter            │
│    → 验证 Token 签名和有效期             │
│    → 提取用户信息到 SecurityContext     │
│                                         │
│  聊天接口:                               │
│    Header: Authorization: Bearer {key}  │
│    → ChatAuthenticationFilter           │
│    → 验证 API Key 状态                  │
│    → 提取用户信息                        │
└─────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────┐
│         权限控制                         │
├─────────────────────────────────────────┤
│  @RequireAdmin 注解                     │
│    ↓                                    │
│  AdminAuthAspect 切面                   │
│    ↓                                    │
│  检查用户角色是否为 ADMIN                │
│    ↓                                    │
│  允许/拒绝访问                           │
└─────────────────────────────────────────┘
```

### 数据安全

```
┌─────────────────────────────────────────┐
│         敏感数据加密                     │
├─────────────────────────────────────────┤
│  后端账户 Token                          │
│    ↓                                    │
│  AES-256-GCM 加密                       │
│    ↓                                    │
│  Base64 编码                            │
│    ↓                                    │
│  存储到数据库                            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│         用户密码处理                     │
├─────────────────────────────────────────┤
│  用户密码（如有）                        │
│    ↓                                    │
│  BCrypt 哈希（自动加盐）                │
│    ↓                                    │
│  存储到数据库                            │
│    ↓                                    │
│  验证时使用 BCrypt.matches()            │
└─────────────────────────────────────────┘
```

## 📊 性能优化

### 1. 数据库优化

**连接池配置**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**索引策略**:
- 用户表: email, status, created_at
- API密钥: api_key, user_id, status
- 调用日志: user_id, created_at, model, status
- 后端账户: provider+status, priority

**查询优化**:
- 使用 MyBatis-Plus 分页插件
- 避免 N+1 查询问题
- 合理使用 JOIN 和子查询
- 定期分析慢查询日志

### 2. 缓存策略

**Redis 缓存应用**:
```java
// 验证码缓存 (TTL: 5 分钟)
redisTemplate.opsForValue().set(
    "verify_code:" + email,
    code,
    5,
    TimeUnit.MINUTES
);

// 会话粘性缓存 (TTL: 1 小时)
redisTemplate.opsForValue().set(
    "session:" + sessionHash,
    accountId,
    1,
    TimeUnit.HOURS
);

// 系统配置缓存 (TTL: 10 分钟)
@Cacheable(value = "system_config", key = "#configKey")
public String getConfig(String configKey) {
    return systemConfigMapper.selectByKey(configKey);
}

// 账户健康状态缓存 (TTL: 30 秒)
redisTemplate.opsForValue().set(
    "account_health:" + accountId,
    healthStatus,
    30,
    TimeUnit.SECONDS
);
```

### 3. 异步处理

**流式响应异步发送**:
```java
@Bean
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("stream-");
    executor.initialize();
    return executor;
}

// 异步处理流式响应
@Async("taskExecutor")
public void handleStreamResponse(SseEmitter emitter, ChatRequest request) {
    // 处理流式数据...
}
```

### 4. 批量操作

**批量插入日志**:
```java
// 使用 MyBatis-Plus 批量插入
apiCallLogService.saveBatch(logs, 1000); // 每批 1000 条
```

## 🔧 扩展性设计

### 1. 新增 AI 服务提供商

只需实现 `ModelProxy` 接口：

```java
@Service
public class ClaudeProxyService implements ModelProxy {

    @Override
    public ChatResponse chat(ChatRequest request, BackendAccount account) {
        // Claude API 调用实现
    }

    @Override
    public StreamChatResponse chatStream(ChatRequest request, BackendAccount account) {
        // Claude 流式调用实现
    }
}
```

### 2. 新增调度策略

在 `AccountSchedulerService` 中添加新策略：

```java
public enum SchedulerStrategy {
    // ... 现有策略
    REGIONAL_OPTIMIZED,  // 区域优化
    LATENCY_OPTIMIZED    // 延迟优化
}

private BackendAccount selectByRegion(List<BackendAccount> accounts,
                                      SchedulerContext context) {
    // 根据用户地理位置选择最近的账户
}
```

### 3. 新增支付渠道

实现 `PaymentService` 接口：

```java
public interface PaymentService {
    CreateOrderResponse createOrder(PaymentRequest request);
    void handleNotify(Map<String, String> params);
    OrderStatus queryOrder(String orderId);
}

@Service
public class PayPalService implements PaymentService {
    // PayPal 支付实现
}
```

## 📈 监控和可观测性

### 日志分级

```
ERROR - 系统错误，需要立即处理
 ├─ 数据库连接失败
 ├─ 外部服务调用失败
 └─ 未捕获异常

WARN - 警告信息，需要关注
 ├─ 账户健康检查失败
 ├─ 余额不足
 ├─ 配额超限
 └─ 请求重试

INFO - 重要业务操作
 ├─ 用户登录
 ├─ 充值成功
 ├─ API 调用
 └─ 订阅套餐

DEBUG - 调试信息（生产环境关闭）
 ├─ 请求参数
 ├─ 响应数据
 └─ 中间计算结果
```

### 关键指标

**业务指标**:
- 用户注册数
- API 调用量（按模型、按用户）
- 充值金额和订单数
- Token 使用量
- 平均响应时间

**技术指标**:
- 接口响应时间 (P50, P95, P99)
- 错误率和成功率
- 数据库连接池使用率
- Redis 缓存命中率
- 后端账户健康状态

**告警规则**:
- API 错误率 > 5%
- P99 响应时间 > 2s
- 数据库连接池使用率 > 90%
- 后端账户可用数 < 2
- Redis 内存使用率 > 80%

## 🎯 关键技术决策

### 1. 为什么选择无密码登录？

- **用户体验**: 减少注册流程，降低用户门槛
- **安全性**: 避免弱密码、密码泄露等问题
- **简化管理**: 无需密码找回、重置功能

### 2. 为什么使用会话粘性？

- **对话连续性**: 确保上下文信息在同一账户处理
- **性能优化**: 减少上下文重新加载
- **用户体验**: 提高响应速度和一致性

### 3. 为什么支持多 API Key？

- **隔离性**: 不同应用使用不同 Key，方便管理
- **安全性**: Key 泄露时只需禁用单个，不影响其他
- **灵活性**: 支持为不同环境创建不同 Key

### 4. 为什么使用混合调度策略？

- **综合优化**: 平衡性能、成本、可用性
- **灵活性**: 根据实际情况动态调整权重
- **扩展性**: 易于添加新的评分因子

## 📚 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [OpenAI API 文档](https://platform.openai.com/docs/api-reference)
- [支付宝开放平台](https://opendocs.alipay.com/)
- [微信支付文档](https://pay.weixin.qq.com/wiki/doc/api/)

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
