# API 接口文档

## 📡 API 概述

AI API Platform 提供 RESTful API 接口，支持 JSON 格式的请求和响应。所有接口遵循统一的响应格式和错误处理机制。

### Base URL

```
http://localhost:8080
```

### 认证方式

#### 1. JWT Token 认证（用户接口）

```http
Authorization: Bearer {jwt_token}
```

#### 2. API Key 认证（聊天接口）

```http
Authorization: Bearer {api_key}
```

或

```http
Authorization: sk-xxxxxxxxxx
```

### 统一响应格式

#### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 响应数据
  }
}
```

#### 错误响应

```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或认证失败 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

## 🔐 认证模块

### 1. 发送验证码

发送邮箱验证码用于登录/注册。

**请求**

```http
POST /api/auth/send-code
Content-Type: application/json
```

```json
{
  "email": "user@example.com"
}
```

**响应**

```json
{
  "code": 200,
  "message": "验证码已发送",
  "data": null
}
```

**说明**:
- 验证码有效期 5 分钟
- 同一邮箱 1 分钟内只能发送一次
- 验证码为 6 位数字

---

### 2. 登录/注册

使用验证码登录，新用户自动注册。

**请求**

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**响应**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "balance": 100.50,
      "status": 1,
      "createdAt": "2025-01-19T10:00:00"
    }
  }
}
```

**说明**:
- Token 有效期 7 天
- 新用户自动生成默认 API Key
- 余额默认为 0

---

## 👤 用户模块

### 1. 获取用户信息

**请求**

```http
GET /api/user/info
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "balance": 100.50,
    "status": 1,
    "createdAt": "2025-01-19T10:00:00",
    "updatedAt": "2025-01-19T12:00:00"
  }
}
```

---

### 2. 获取用户余额

**请求**

```http
GET /api/user/balance
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "balance": 100.50
  }
}
```

---

### 3. 获取用户统计数据

**请求**

```http
GET /api/user/stats
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCalls": 1523,
    "totalCost": 45.67,
    "totalInputTokens": 125000,
    "totalOutputTokens": 98000,
    "lastCallTime": "2025-01-19T12:30:00"
  }
}
```

---

## 🔑 API 密钥管理

### 1. 获取所有 API 密钥

**请求**

```http
GET /api/user/api-keys
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "keyName": "生产环境",
      "apiKey": "sk-1234****abcd",
      "status": 1,
      "lastUsedAt": "2025-01-19T12:00:00",
      "createdAt": "2025-01-01T10:00:00"
    },
    {
      "id": 2,
      "keyName": "测试环境",
      "apiKey": "sk-5678****efgh",
      "status": 1,
      "lastUsedAt": null,
      "createdAt": "2025-01-15T14:00:00"
    }
  ]
}
```

---

### 2. 创建 API 密钥

**请求**

```http
POST /api/user/api-keys
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "keyName": "新项目"
}
```

**响应**

```json
{
  "code": 200,
  "message": "API 密钥创建成功",
  "data": {
    "id": 3,
    "keyName": "新项目",
    "apiKey": "sk-9012abcd3456efgh7890ijkl1234mnop",
    "status": 1,
    "createdAt": "2025-01-19T13:00:00"
  }
}
```

**说明**:
- API Key 格式: `sk-{32位随机字符}`
- 创建后立即显示完整 Key，后续只显示脱敏版本
- 单用户最多创建 10 个 Key

---

### 3. 更新密钥状态

**请求**

```http
PUT /api/user/api-keys/{keyId}
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "status": 0
}
```

**响应**

```json
{
  "code": 200,
  "message": "API 密钥已禁用",
  "data": null
}
```

**说明**:
- status: 1-启用，0-禁用
- 禁用后立即失效

---

### 4. 删除 API 密钥

**请求**

```http
DELETE /api/user/api-keys/{keyId}
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "API 密钥已删除",
  "data": null
}
```

**说明**:
- 删除后无法恢复
- 建议先禁用再删除

---

### 5. 重新生成密钥

**请求**

```http
POST /api/user/api-keys/{keyId}/regenerate
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "API 密钥已重新生成",
  "data": {
    "apiKey": "sk-new1234567890abcdefghijklmnopqrst"
  }
}
```

**说明**:
- 旧密钥立即失效
- 生成新的随机密钥

---

## 🤖 AI 聊天模块

### 1. 聊天接口（OpenAI 兼容）

**请求**

```http
POST /api/v1/chat/completions
Authorization: Bearer {api_key}
Content-Type: application/json
```

#### 非流式请求

```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "system",
      "content": "你是一个有帮助的助手。"
    },
    {
      "role": "user",
      "content": "你好，请介绍一下自己。"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 1000,
  "stream": false
}
```

#### 非流式响应

```json
{
  "id": "chatcmpl-123456",
  "object": "chat.completion",
  "created": 1705654800,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！我是一个AI助手..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 25,
    "completion_tokens": 150,
    "total_tokens": 175,
    "prompt_tokens_details": {
      "cached_tokens": 0
    },
    "completion_tokens_details": {
      "reasoning_tokens": 0
    }
  }
}
```

#### 流式请求

```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "user",
      "content": "写一首关于春天的诗"
    }
  ],
  "stream": true
}
```

#### 流式响应（SSE）

```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1705654800,"model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1705654800,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"春"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1705654800,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"风"},"finish_reason":null}]}

...

data: [DONE]
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| model | string | 是 | 模型名称 |
| messages | array | 是 | 对话消息数组 |
| temperature | number | 否 | 随机性 0-2，默认 0.7 |
| max_tokens | integer | 否 | 最大生成 tokens |
| stream | boolean | 否 | 是否流式输出，默认 false |
| top_p | number | 否 | 核采样，默认 1 |
| frequency_penalty | number | 否 | 频率惩罚 -2.0~2.0 |
| presence_penalty | number | 否 | 存在惩罚 -2.0~2.0 |

**支持的模型**:
- gpt-4o
- gpt-4o-mini
- gpt-4-turbo
- gpt-4
- gpt-3.5-turbo
- o1-preview
- o1-mini
- claude-3-5-sonnet-20241022
- claude-3.5-sonnet
- claude-3-opus
- claude-3-haiku

---

### 2. 获取模型列表

**请求**

```http
GET /api/models
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "modelName": "gpt-4o",
      "displayName": "GPT-4o",
      "provider": "copilot",
      "inputTokenPrice": 10.00,
      "outputTokenPrice": 30.00,
      "supportsStreaming": true,
      "maxTokens": 4096,
      "description": "OpenAI GPT-4o 模型 - 最新旗舰模型",
      "tags": ["推荐", "最新"],
      "sortOrder": 10
    },
    {
      "id": 2,
      "modelName": "gpt-4o-mini",
      "displayName": "GPT-4o Mini",
      "provider": "copilot",
      "inputTokenPrice": 2.50,
      "outputTokenPrice": 10.00,
      "supportsStreaming": true,
      "maxTokens": 4096,
      "description": "OpenAI GPT-4o Mini - 经济高效",
      "tags": ["推荐", "低价"],
      "sortOrder": 20
    }
  ]
}
```

**说明**:
- 只返回 status=1 的启用模型
- 价格单位: 美元/百万 tokens
- 按 sortOrder 排序

---

## 💳 充值模块

### 1. 创建充值订单

**请求**

```http
POST /api/recharge/create
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "amount": 100.00,
  "payMethod": "alipay"
}
```

**响应**

```json
{
  "code": 200,
  "message": "订单创建成功",
  "data": {
    "orderId": 123,
    "orderNo": "ORDER_1705654800_abc123",
    "amount": 100.00,
    "payMethod": "alipay",
    "payUrl": "https://openapi.alipay.com/gateway.do?...",
    "qrCode": "data:image/png;base64,..." // 微信支付时返回
  }
}
```

**参数说明**:
- amount: 充值金额（元），最小 1 元
- payMethod: `alipay` 或 `wechat`

**支付方式**:
- **支付宝**: 返回支付 URL，PC 端跳转
- **微信支付**: 返回二维码，扫码支付

---

### 2. 查询订单状态

**请求**

```http
GET /api/recharge/query/{orderId}
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 123,
    "orderNo": "ORDER_1705654800_abc123",
    "amount": 100.00,
    "status": 1,
    "payMethod": "alipay",
    "tradeNo": "2025011922001234567890",
    "payTime": "2025-01-19T14:30:00",
    "createdAt": "2025-01-19T14:25:00"
  }
}
```

**订单状态**:
- 0: 待支付
- 1: 已支付
- 2: 已取消

---

### 3. 获取充值订单列表

**请求**

```http
GET /api/recharge/orders?page=1&pageSize=10
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "orderId": 123,
        "orderNo": "ORDER_1705654800_abc123",
        "amount": 100.00,
        "status": 1,
        "payMethod": "alipay",
        "payTime": "2025-01-19T14:30:00",
        "createdAt": "2025-01-19T14:25:00"
      }
    ],
    "total": 15,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 4. 获取订单详情

**请求**

```http
GET /api/recharge/orders/{orderId}
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 123,
    "orderNo": "ORDER_1705654800_abc123",
    "amount": 100.00,
    "status": 1,
    "payMethod": "alipay",
    "tradeNo": "2025011922001234567890",
    "payTime": "2025-01-19T14:30:00",
    "createdAt": "2025-01-19T14:25:00",
    "updatedAt": "2025-01-19T14:30:00"
  }
}
```

---

## 💰 余额模块

### 1. 获取余额变动历史

**请求**

```http
GET /api/balance/history?page=1&pageSize=20&type=consume
Authorization: Bearer {jwt_token}
```

**查询参数**:
- page: 页码，默认 1
- pageSize: 每页数量，默认 20
- type: 类型过滤，`recharge` 或 `consume`

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1001,
        "amount": -0.0456,
        "balanceAfter": 99.9544,
        "type": "consume",
        "relatedId": 5678,
        "remark": "API调用 - gpt-4o",
        "createdAt": "2025-01-19T14:35:00"
      },
      {
        "id": 1000,
        "amount": 100.00,
        "balanceAfter": 100.00,
        "type": "recharge",
        "relatedId": 123,
        "remark": "充值",
        "createdAt": "2025-01-19T14:30:00"
      }
    ],
    "total": 50,
    "page": 1,
    "pageSize": 20
  }
}
```

---

## 📦 订阅模块

### 1. 获取套餐列表

**请求**

```http
GET /api/subscriptions/plans
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "planName": "trial_card",
      "displayName": "体验卡",
      "description": "适合新用户体验",
      "originalPrice": 9.90,
      "price": 4.90,
      "quotaAmount": 10.00,
      "features": [
        "7天有效期",
        "10元额度",
        "支持所有模型",
        "基础技术支持"
      ],
      "colorTheme": "green",
      "badgeText": "新用户专享",
      "sortOrder": 1
    },
    {
      "id": 2,
      "planName": "max_100",
      "displayName": "Max 100",
      "description": "适合轻度使用",
      "originalPrice": 188.00,
      "price": 98.00,
      "quotaAmount": 120.00,
      "features": [
        "30天有效期",
        "120元额度",
        "支持所有模型",
        "优先技术支持",
        "API使用统计"
      ],
      "colorTheme": "blue",
      "badgeText": "推荐套餐",
      "sortOrder": 2
    }
  ]
}
```

---

### 2. 订阅套餐

**请求**

```http
POST /api/subscriptions/subscribe
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "planId": 2
}
```

**响应**

```json
{
  "code": 200,
  "message": "订阅成功",
  "data": {
    "subscriptionId": 456,
    "planName": "Max 100",
    "amount": 98.00,
    "quotaAmount": 120.00,
    "startDate": "2025-01-19",
    "endDate": "2025-02-18",
    "status": "active"
  }
}
```

**说明**:
- 从用户余额扣除套餐价格
- 立即发放额度到余额
- 设置有效期

---

### 3. 获取订阅历史

**请求**

```http
GET /api/subscriptions/history?page=1&pageSize=10
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 456,
        "planName": "Max 100",
        "amount": 98.00,
        "quotaAmount": 120.00,
        "startDate": "2025-01-19",
        "endDate": "2025-02-18",
        "status": "active",
        "createdAt": "2025-01-19T15:00:00"
      }
    ],
    "total": 3,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 4. 取消订阅

**请求**

```http
POST /api/subscriptions/{id}/cancel
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "订阅已取消",
  "data": null
}
```

**说明**:
- 立即取消订阅
- 不退款
- 已发放额度保留

---

## 📊 统计模块

### 1. Token 使用统计

**请求**

```http
GET /api/statistics/tokens?startDate=2025-01-01&endDate=2025-01-19
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalInputTokens": 125000,
    "totalOutputTokens": 98000,
    "totalTokens": 223000,
    "totalCost": 45.67
  }
}
```

---

### 2. 模型使用统计

**请求**

```http
GET /api/statistics/models?startDate=2025-01-01&endDate=2025-01-19
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "model": "gpt-4o",
      "callCount": 856,
      "totalCost": 28.45,
      "totalTokens": 145000
    },
    {
      "model": "gpt-4o-mini",
      "callCount": 667,
      "totalCost": 17.22,
      "totalTokens": 78000
    }
  ]
}
```

---

### 3. 趋势分析

**请求**

```http
GET /api/statistics/trends?period=7d
Authorization: Bearer {jwt_token}
```

**查询参数**:
- period: `7d`（7天）、`30d`（30天）、`90d`（90天）

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["2025-01-13", "2025-01-14", "2025-01-15", ...],
    "callCounts": [120, 145, 98, ...],
    "costs": [5.67, 6.89, 4.23, ...],
    "tokens": [25000, 30000, 20000, ...]
  }
}
```

---

## 🎫 工单模块

### 1. 获取工单列表

**请求**

```http
GET /api/tickets?page=1&pageSize=10&status=open
Authorization: Bearer {jwt_token}
```

**查询参数**:
- status: `open`、`in_progress`、`closed`

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "subject": "API 调用失败",
        "content": "调用 gpt-4o 时返回 500 错误",
        "status": "open",
        "priority": "high",
        "createdAt": "2025-01-19T10:00:00",
        "updatedAt": "2025-01-19T10:00:00"
      }
    ],
    "total": 5,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 2. 创建工单

**请求**

```http
POST /api/tickets
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "subject": "API 调用失败",
  "content": "调用 gpt-4o 时返回 500 错误，请帮忙查看",
  "priority": "high"
}
```

**响应**

```json
{
  "code": 200,
  "message": "工单创建成功",
  "data": {
    "id": 1,
    "subject": "API 调用失败",
    "status": "open",
    "priority": "high",
    "createdAt": "2025-01-19T10:00:00"
  }
}
```

**优先级**:
- low: 低
- normal: 正常
- high: 高
- urgent: 紧急

---

### 3. 获取工单详情

**请求**

```http
GET /api/tickets/{id}
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "subject": "API 调用失败",
    "content": "调用 gpt-4o 时返回 500 错误",
    "status": "in_progress",
    "priority": "high",
    "messages": [
      {
        "id": 1,
        "message": "调用 gpt-4o 时返回 500 错误，请帮忙查看",
        "isStaff": false,
        "createdAt": "2025-01-19T10:00:00"
      },
      {
        "id": 2,
        "message": "我们正在调查此问题，请稍候",
        "isStaff": true,
        "createdAt": "2025-01-19T10:30:00"
      }
    ],
    "createdAt": "2025-01-19T10:00:00",
    "updatedAt": "2025-01-19T10:30:00"
  }
}
```

---

### 4. 回复工单

**请求**

```http
POST /api/tickets/{id}/reply
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "message": "问题依然存在，请尽快处理"
}
```

**响应**

```json
{
  "code": 200,
  "message": "回复成功",
  "data": null
}
```

---

## 📈 配额模块

### 1. 获取配额使用情况

**请求**

```http
GET /api/quota/usage
Authorization: Bearer {jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "quotaType": "daily",
      "quotaAmount": 100.00,
      "usedAmount": 45.67,
      "remainingAmount": 54.33,
      "usagePercentage": 45.67,
      "resetAt": "2025-01-20T00:00:00",
      "isEnabled": true,
      "alertThreshold": 80.00
    },
    {
      "quotaType": "monthly",
      "quotaAmount": 2000.00,
      "usedAmount": 567.89,
      "remainingAmount": 1432.11,
      "usagePercentage": 28.39,
      "resetAt": "2025-02-01T00:00:00",
      "isEnabled": true,
      "alertThreshold": 80.00
    }
  ]
}
```

---

### 2. 配额设置

**请求**

```http
POST /api/quota/settings
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

```json
{
  "quotaType": "daily",
  "quotaAmount": 200.00,
  "isEnabled": true,
  "alertThreshold": 90.00
}
```

**响应**

```json
{
  "code": 200,
  "message": "配额设置成功",
  "data": null
}
```

---

## 🔧 管理后台 API

（需要 ADMIN 角色）

### 平台统计

**请求**

```http
GET /api/admin/statistics
Authorization: Bearer {admin_jwt_token}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 1523,
    "activeUsers": 856,
    "totalOrders": 3456,
    "totalRevenue": 123456.78,
    "todayCalls": 25000,
    "todayRevenue": 5678.90
  }
}
```

### 用户管理

详见 [管理后台文档](./modules/ADMIN.md)

---

## 📝 使用示例

### Python

```python
import requests

# 登录获取 Token
response = requests.post('http://localhost:8080/api/auth/login', json={
    'email': 'user@example.com',
    'code': '123456'
})
token = response.json()['data']['token']

# 获取 API Key
response = requests.get(
    'http://localhost:8080/api/user/api-keys',
    headers={'Authorization': f'Bearer {token}'}
)
api_key = response.json()['data'][0]['apiKey']

# 调用聊天接口
response = requests.post(
    'http://localhost:8080/api/v1/chat/completions',
    headers={'Authorization': f'Bearer {api_key}'},
    json={
        'model': 'gpt-4o',
        'messages': [
            {'role': 'user', 'content': '你好'}
        ]
    }
)
print(response.json())
```

### Node.js

```javascript
const axios = require('axios');

async function main() {
  // 登录
  const loginRes = await axios.post('http://localhost:8080/api/auth/login', {
    email: 'user@example.com',
    code: '123456'
  });
  const token = loginRes.data.data.token;

  // 获取 API Key
  const keysRes = await axios.get('http://localhost:8080/api/user/api-keys', {
    headers: { Authorization: `Bearer ${token}` }
  });
  const apiKey = keysRes.data.data[0].apiKey;

  // 调用聊天接口
  const chatRes = await axios.post(
    'http://localhost:8080/api/v1/chat/completions',
    {
      model: 'gpt-4o',
      messages: [{ role: 'user', content: '你好' }]
    },
    {
      headers: { Authorization: `Bearer ${apiKey}` }
    }
  );
  console.log(chatRes.data);
}

main();
```

### cURL

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","code":"123456"}'

# 聊天
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Authorization: Bearer sk-your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "你好"}]
  }'
```

---

## 🔍 常见问题

### Q: Token 过期怎么办？

A: Token 有效期 7 天，过期后需要重新登录获取新 Token。

### Q: API Key 泄露怎么办？

A: 立即在用户中心禁用或删除该 Key，然后创建新的 Key。

### Q: 如何使用流式输出？

A: 设置 `stream: true`，响应为 SSE 格式，需要使用支持 EventSource 或流式处理的客户端。

### Q: 余额不足会怎样？

A: API 调用前会检查余额，不足时返回 402 错误，需要先充值。

### Q: 支持哪些支付方式？

A: 目前支持支付宝 PC 支付和微信 Native 扫码支付。

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
