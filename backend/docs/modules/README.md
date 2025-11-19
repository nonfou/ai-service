# 功能模块文档索引

## 📚 已完成的详细文档

### 🔐 [认证授权模块 (AUTH.md)](./AUTH.md)
- 邮箱验证码登录
- JWT Token 认证
- API Key 管理
- 权限控制

### 🔄 [账户调度系统 (ACCOUNT_SCHEDULER.md)](./ACCOUNT_SCHEDULER.md)
- 多账户管理
- 5种调度策略
- 会话粘性
- 健康检查和故障转移

---

## 📋 其他模块简介

### 🤖 AI 聊天服务模块

**核心文件**: `ChatController`, `ChatWorkflowService`

**功能**:
- OpenAI 兼容的聊天接口 `/api/v1/chat/completions`
- 支持流式(SSE)和非流式响应
- 请求编排：认证 → 配额检查 → 账户调度 → 代理调用 → 计费 → 日志
- Token 估算和计费

**关键类**:
- `CopilotProxyService`: Copilot 代理实现
- `OpenRouterProxyService`: OpenRouter 代理实现
- `ChatWorkflowService`: 聊天流程编排

---

### 💰 计费系统模块

**核心文件**: `BalanceService`, `CostCalculatorService`

**功能**:
- Token 精确计费（输入/输出/缓存Token分开）
- 多层价格倍率（模型倍率 × 账户倍率 × 平台加成）
- 实时余额扣除
- 余额变动日志

**计费公式**:
```
最终费用 = (
  输入Token × 输入价格 +
  输出Token × 输出价格 +
  缓存Token × 缓存价格
) / 1,000,000 × 模型倍率 × 账户倍率 × 平台加成 × 汇率
```

---

### 💳 支付集成模块

**核心文件**: `AlipayService`, `WechatPayService`

**支持的支付方式**:
- **支付宝**: PC 网站支付
- **微信支付**: Native 扫码支付

**支付流程**:
1. 创建订单
2. 生成支付链接/二维码
3. 用户完成支付
4. 接收异步回调
5. 验证签名
6. 更新订单状态
7. 发放余额

---

### 📦 订阅套餐模块

**核心文件**: `SubscriptionService`, `SubscriptionPlanService`

**套餐类型**:
- 体验卡：7天，10元额度
- Max 100：30天，120元额度
- Max 200：30天，250元额度

**功能**:
- 套餐管理（CRUD）
- 订阅购买
- 自动发放额度
- 订阅历史

---

### 📊 统计分析模块

**核心文件**: `StatisticsService`, `UsageMetricsService`

**统计维度**:
- **用户维度**: Token使用量、调用次数、总费用
- **模型维度**: 各模型使用情况
- **时间维度**: 每日/每周/每月趋势
- **账户维度**: 后端账户使用统计

**数据源**:
- `api_calls` 表：调用日志
- `v_user_stats` 视图：用户统计聚合
- Redis 缓存：实时统计

---

### 🎫 工单系统模块

**核心文件**: `TicketService`, `TicketController`

**功能**:
- 工单创建
- 工单列表（按状态筛选）
- 工单详情（含消息历史）
- 用户回复
- 管理员回复
- 状态管理（open/in_progress/closed）
- 优先级管理（low/normal/high/urgent）

---

### 🔧 管理后台模块

**核心文件**: `AdminController`, `AdminService`

**管理功能**:
- **用户管理**: 查询、禁用、余额调整
- **订单管理**: 充值订单查询和处理
- **模型管理**: 模型配置、价格设置
- **账户管理**: 后端账户管理
- **套餐管理**: 订阅套餐配置
- **工单管理**: 工单处理
- **统计报表**: 平台数据统计

**权限控制**:
```java
@RequireAdmin  // 管理员权限注解
public Result<?> adminOperation() {
    // ���有 ADMIN 角色可访问
}
```

---

### 🧩 模型管理模块

**核心文件**: `ModelService`, `ModelController`

**模型配置**:
- 模型名称和显示名称
- 提供商（copilot/openrouter）
- 价格配置（输入/输出/缓存Token分开）
- 价格倍率
- 最大Token数
- 是否支持流式
- 状态管理
- 标签系统（推荐、低价、新品等）
- 排序

**已配置模型**:
- GPT 系列：gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-4, gpt-3.5-turbo
- O1 系列：o1-preview, o1-mini
- Claude 系列：claude-3-5-sonnet, claude-3-opus, claude-3-haiku

---

### 📏 配额管理模块

**核心文件**: `QuotaService`, `UserQuotaService`

**配额类型**:
- **每日配额**: 每天 00:00 自动重置
- **每月配额**: 每月 1 号 00:00 自动重置
- **自定义配额**: 手动设置重置时间

**功能**:
- 配额设置
- 使用量统计
- 自动重置
- 告警机制（达到阈值时）
- 超限拦截

**实现**:
```java
public void checkQuota(Long userId) {
    UserQuota quota = quotaService.getUserQuota(userId);

    if (quota.getUsedAmount().compareTo(quota.getQuotaAmount()) >= 0) {
        throw new QuotaExceededException("配额已用尽");
    }

    // 达到告警阈值
    if (quota.getUsagePercentage() >= quota.getAlertThreshold()) {
        alertService.sendQuotaAlert(userId);
    }
}
```

---

## 🔗 模块依赖关系

```
┌─────────────────────────────────────────┐
│         ChatWorkflowService              │  ← 聊天请求入口
│         (聊天编排服务)                     │
└────────────┬────────────────────────────┘
             │
             ├──→ AuthService              (认证)
             ├──→ QuotaService             (配额检查)
             ├──→ AccountSchedulerService  (账户调度)
             ├──→ ModelProxy               (代理调用)
             ├──→ CostCalculatorService    (成本计算)
             ├──→ BalanceService           (余额扣除)
             └──→ ApiCallLogService        (日志记录)

┌─────────────────────────────────────────┐
│      AccountSchedulerService             │  ← 账户调度
└────────────┬────────────────────────────┘
             │
             ├──→ SessionStickinessService  (会话粘性)
             ├──→ BackendAccountService     (账户管理)
             ├──→ ConcurrencyLimitService   (并发控制)
             └──→ RateLimitService          (速率限制)

┌─────────────────────────────────────────┐
│         PaymentService                   │  ← 支付处理
└────────────┬────────────────────────────┘
             │
             ├──→ RechargeOrderService      (订单管理)
             ├──→ BalanceService            (余额发放)
             └──→ BalanceLogService         (日志记录)
```

---

## 📖 阅读顺序建议

### 新手入门
1. [README.md](../README.md) - 项目总览
2. [DEPLOYMENT.md](../DEPLOYMENT.md) - 快速部署
3. [API.md](../API.md) - API 接口使用

### 开发人员
1. [ARCHITECTURE.md](../ARCHITECTURE.md) - 系统架构
2. [DATABASE.md](../DATABASE.md) - 数据库设计
3. [AUTH.md](./AUTH.md) - 认证机制
4. [ACCOUNT_SCHEDULER.md](./ACCOUNT_SCHEDULER.md) - 调度系统
5. [DEVELOPMENT.md](../DEVELOPMENT.md) - 开发规范

### 运维人员
1. [DEPLOYMENT.md](../DEPLOYMENT.md) - 部署指南
2. [CONFIGURATION.md](../CONFIGURATION.md) - 配置详解
3. [TROUBLESHOOTING.md](../TROUBLESHOOTING.md) - 故障排查
4. [SECURITY.md](../SECURITY.md) - 安全加固

---

## 🤝 贡献文档

如需补充或完善文档，请：

1. Fork 项目
2. 创建文档分支
3. 编写或修改文档
4. 提交 Pull Request

**文档规范**:
- 使用 Markdown 格式
- 添加目录和章节标题
- 包含代码示例
- 添加图表说明（如需要）
- 标注最后更新时间

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
