# 数据库设计文档

## 📊 概述

AI API Platform 使用 MySQL 8.0 作为主数据库，采用关系型数据模型设计。数据库包含 16 张核心数据表和 1 个统计视图，涵盖用户管理、财务系统、AI 服务调用、订阅管理、工单系统等模块。

### 数据库信息

- **数据库类型**: MySQL 8.0+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB
- **初始化脚本**: `src/main/resources/db/init_database.sql`

## 🗂️ 数据表概览

| 表名 | 说明 | 记录数量级 | 关键索引 |
|------|------|-----------|---------|
| users | 用户表 | 10K+ | email, status |
| api_keys | API密钥表 | 30K+ | api_key, user_id |
| backend_accounts | 后端账户配置表 | 10-100 | provider+status |
| user_account_bindings | 用户账户绑定表 | 10K+ | user_id |
| session_mappings | 会话粘性映射表 | 100K+ | session_hash, expires_at |
| recharge_orders | 充值订单表 | 100K+ | order_no, user_id |
| balance_log | 余额变动日志表 | 1M+ | user_id, created_at |
| api_calls | API调用日志表 | 10M+ | user_id, created_at, model |
| models | 模型配置表 | 100+ | model_name, status |
| subscription_plans | 订阅套餐表 | 10-50 | status, sort_order |
| subscriptions | 订阅记录表 | 50K+ | user_id, end_date |
| user_quotas | 用户配额管理表 | 20K+ | user_id, reset_at |
| tickets | 工单表 | 10K+ | user_id, status |
| ticket_messages | 工单消息表 | 50K+ | ticket_id |
| system_config | 系统配置表 | <100 | config_key |
| admins | 管理员表 | <20 | username |

## 📐 ER 关系图

```
┌──────────────┐         ┌──────────────┐         ┌──────────────────────┐
│    users     │◄───────┤   api_keys   │◄───────┤ user_account_bindings│
│ (用户表)      │         │  (API密钥表)  │         │  (用户账户绑定表)     │
└──────┬───────┘         └──────┬───────┘         └──────────────────────┘
       │                         │                           │
       │                         │                           │
       ├─────────────────────────┼───────────────────────────┤
       │                         │                           │
       ▼                         ▼                           ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────────────┐
│  api_calls   │         │session_      │         │  backend_accounts    │
│ (API调用日志) │         │mappings      │         │  (后端账户配置表)     │
└──────────────┘         │(会话粘性表)   │         └──────────────────────┘
                         └──────────────┘
       │
       ├────────────────┬────────────────┬────────────────┐
       │                │                │                │
       ▼                ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│recharge_     │  │ balance_log  │  │subscriptions │  │  tickets     │
│orders        │  │ (余额日志)    │  │  (订阅记录)   │  │  (工单)      │
│(充值订单)     │  └──────────────┘  └──────┬───────┘  └──────┬───────┘
└──────────────┘                             │                  │
                                             ▼                  ▼
                                    ┌──────────────┐  ┌──────────────┐
                                    │subscription_ │  │ticket_       │
                                    │plans         │  │messages      │
                                    │(订阅套餐)     │  │(工单消息)     │
                                    └──────────────┘  └──────────────┘

       ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
       │   models     │         │user_quotas   │         │system_config │
       │  (模型配置)   │         │ (用户配额)    │         │ (系统配置)    │
       └──────────────┘         └──────────────┘         └──────────────┘

       ┌──────────────┐
       │   admins     │
       │  (管理员)     │
       └──────────────┘
```

## 📋 详细表结构

### 1. users - 用户表

存储系统用户的基本信息。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 邮箱地址 |
| password | VARCHAR(255) | NULL | 密码（BCrypt加密，可为空） |
| api_key | VARCHAR(64) | NULL | 已废弃，使用 api_keys 表 |
| balance | DECIMAL(10,4) | DEFAULT 0.0000 | 账户余额（元） |
| status | TINYINT | DEFAULT 1 | 1-正常，0-禁用 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_email` (email) - 邮箱查询
- `idx_status` (status) - 状态过滤
- `idx_created_at` (created_at) - 时间排序

**业务规则**:
- 邮箱唯一，不可重复
- 新用户余额默认为 0
- 支持无密码登录（验证码登录）

---

### 2. api_keys - API密钥表

支持用户创建多个 API 密钥，用于不同应用或环境。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 密钥ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 所属用户 |
| key_name | VARCHAR(100) | NOT NULL | 密钥名称 |
| api_key | VARCHAR(64) | UNIQUE, NOT NULL | API密钥（sk-xxx格式） |
| status | TINYINT | DEFAULT 1 | 1-启用，0-禁用 |
| last_used_at | TIMESTAMP | NULL | 最后使用时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_user_id` (user_id) - 用户查询
- `idx_api_key` (api_key) - 密钥认证
- `idx_status` (status) - 状态过滤

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE

**业务规则**:
- API Key 格式: `sk-{uuid}` (去除连字符)
- 单用户可创建多个 Key
- 禁用 Key 后立即失效
- 删除用户时级联删除所有 Key

---

### 3. backend_accounts - 后端账户配置表

存储多个 AI 服务提供商的账户配置。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 账户ID |
| account_name | VARCHAR(100) | NOT NULL | 账户名称 |
| provider | ENUM | NOT NULL | copilot/openrouter |
| access_token | TEXT | NOT NULL | AES-256 加密的令牌 |
| priority | INT | DEFAULT 1 | 优先级 1-100，越小越高 |
| status | ENUM | DEFAULT 'active' | active/disabled/error |
| max_concurrent | INT | DEFAULT 10 | 最大并发请求数 |
| rate_limit_per_minute | INT | DEFAULT 60 | 每分钟请求限制 |
| cost_multiplier | DECIMAL(10,4) | DEFAULT 1.0000 | 成本倍率 |
| error_count | INT | DEFAULT 0 | 连续错误次数 |
| last_used_at | DATETIME | NULL | 最后使用时间 |
| last_error_at | DATETIME | NULL | 最后错误时间 |
| last_error_message | TEXT | NULL | 最后错误信息 |
| metadata | JSON | NULL | 扩展信息 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

**索引**:
- `idx_provider_status` (provider, status) - 提供商过滤
- `idx_priority` (priority) - 优先级排序
- `idx_last_used` (last_used_at) - 使用时间

**业务规则**:
- access_token 使用 AES-256-GCM 加密存储
- 错误次数达到阈值自动降级为 error 状态
- 支持热更新配置
- metadata 可存储区域、支持模型等扩展信息

---

### 4. user_account_bindings - 用户账户绑定表

将用户/API Key 绑定到特定的后端账户。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 绑定ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| api_key_id | BIGINT | FK → api_keys(id), NULL | API密钥ID（可选） |
| backend_account_id | BIGINT | FK → backend_accounts(id) | 后端账户ID |
| is_default | BOOLEAN | DEFAULT FALSE | 是否默认账户 |
| binding_type | ENUM | DEFAULT 'user' | user/api_key |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

**索引**:
- `uk_user_account` (user_id, backend_account_id) - 唯一约束
- `idx_user_id` (user_id)
- `idx_api_key_id` (api_key_id)

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE
- `api_key_id` → `api_keys(id)` ON DELETE CASCADE
- `backend_account_id` → `backend_accounts(id)` ON DELETE CASCADE

**业务规则**:
- 用户级绑定: api_key_id 为 NULL
- API Key 级绑定: api_key_id 不为 NULL
- 同一用户不能重复绑定同一账户

---

### 5. session_mappings - 会话粘性映射表

实现会话粘性，确保同一会话的请求路由到同一账户。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 映射ID |
| session_hash | VARCHAR(64) | UNIQUE, NOT NULL | 会话哈希（SHA-256） |
| backend_account_id | BIGINT | FK → backend_accounts(id) | 绑定的账户ID |
| api_key_id | BIGINT | FK → api_keys(id) | API密钥ID |
| user_id | BIGINT | FK → users(id) | 用户ID |
| request_count | INT | DEFAULT 1 | 请求次数 |
| expires_at | DATETIME | NOT NULL | 过期时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

**索引**:
- `uk_session` (session_hash) - 唯一约束
- `idx_expires` (expires_at) - 过期清理
- `idx_account` (backend_account_id) - 账户查询
- `idx_user` (user_id) - 用户查询

**外键**:
- `backend_account_id` → `backend_accounts(id)` ON DELETE CASCADE
- `api_key_id` → `api_keys(id)` ON DELETE CASCADE
- `user_id` → `users(id)` ON DELETE CASCADE

**业务规则**:
- session_hash 由用户ID、对话ID等生成
- 默认有效期 1 小时（可配置）
- 定期清理过期记录
- 账户不健康时自动失效

---

### 6. recharge_orders - 充值订单表

记录用户的充值订单信息。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 订单ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| order_no | VARCHAR(64) | UNIQUE, NOT NULL | 订单号 |
| amount | DECIMAL(10,2) | NOT NULL | 充值金额（元） |
| status | TINYINT | DEFAULT 0 | 0-待支付，1-已支付，2-已取消 |
| pay_method | VARCHAR(20) | NULL | alipay/wechat |
| trade_no | VARCHAR(100) | NULL | 第三方交易号 |
| pay_time | TIMESTAMP | NULL | 支付时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_user_id` (user_id)
- `idx_order_no` (order_no)
- `idx_status` (status)
- `idx_created_at` (created_at)
- `idx_user_created` (user_id, created_at) - 复合索引
- `idx_user_status` (user_id, status) - 复合索引

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE

**业务规则**:
- order_no 格式: `ORDER_{timestamp}_{random}`
- 支付成功后更新用户余额
- 记录到 balance_log 表
- 支持支付宝和微信支付

---

### 7. balance_log - 余额变动日志表

记录用户余额的所有变动。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 日志ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| amount | DECIMAL(10,4) | NOT NULL | 变动金额（正/负） |
| balance_after | DECIMAL(10,4) | NOT NULL | 变动后余额 |
| type | VARCHAR(20) | NOT NULL | recharge/consume |
| related_id | BIGINT | NULL | 关联ID |
| remark | VARCHAR(255) | NULL | 备注 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**:
- `idx_user_id` (user_id)
- `idx_type` (type)
- `idx_created_at` (created_at)
- `idx_user_created` (user_id, created_at)
- `idx_user_type` (user_id, type)

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE

**业务规则**:
- 充值: type=recharge, amount>0, related_id=order_id
- 消费: type=consume, amount<0, related_id=api_call_id
- 只增不改，用于审计
- 记录变动后余额，便于对账

---

### 8. api_calls - API调用日志表

记录每次 AI API 调用的详细信息。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 调用ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| api_key | VARCHAR(64) | NOT NULL | 使用的API Key |
| model | VARCHAR(50) | NOT NULL | 模型名称 |
| backend_account_id | BIGINT | NULL | 后端账户ID |
| provider | VARCHAR(50) | NULL | copilot/openrouter |
| input_tokens | INT | DEFAULT 0 | 输入token数 |
| output_tokens | INT | DEFAULT 0 | 输出token数 |
| cache_read_tokens | INT | DEFAULT 0 | 缓存读取tokens |
| cache_write_tokens | INT | DEFAULT 0 | 缓存写入tokens |
| session_hash | VARCHAR(64) | NULL | 会话哈希 |
| cost | DECIMAL(10,6) | DEFAULT 0 | 费用（元） |
| raw_cost | DECIMAL(20,10) | DEFAULT 0 | 原始成本（元） |
| markup_rate | DECIMAL(10,4) | DEFAULT 1.0000 | 加成倍率 |
| markup_cost | DECIMAL(20,10) | DEFAULT 0 | 加成金额（元） |
| request_time | TIMESTAMP | NOT NULL | 请求时间 |
| response_time | TIMESTAMP | NULL | 响应时间 |
| duration | INT | NULL | 耗时（毫秒） |
| status | TINYINT | DEFAULT 1 | 1-成功，0-失败 |
| error_msg | TEXT | NULL | 错误信息 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**:
- `idx_user_id` (user_id)
- `idx_api_key` (api_key)
- `idx_model` (model)
- `idx_status` (status)
- `idx_created_at` (created_at)
- `idx_backend_account` (backend_account_id)
- `idx_provider` (provider)
- `idx_session` (session_hash)
- `idx_user_created` (user_id, created_at) - 复合索引
- `idx_user_model` (user_id, model) - 复合索引
- `idx_user_status` (user_id, status) - 复合索引

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE

**性能优化**:
- 高频写入表，考虑分区策略（按月分区）
- 定期归档历史数据
- 统计查询使用复合索引

---

### 9. models - 模型配置表

存储所有支持的 AI 模型配置和价格信息。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 模型ID |
| model_name | VARCHAR(50) | UNIQUE, NOT NULL | 模型名称 |
| display_name | VARCHAR(100) | NOT NULL | 显示名称 |
| provider | VARCHAR(50) | NULL | copilot/openrouter/custom |
| price_multiplier | DECIMAL(5,2) | DEFAULT 1.00 | 价格倍率 |
| input_token_price | DECIMAL(20,10) | NULL | 输入价格（$/1M tokens） |
| output_token_price | DECIMAL(20,10) | NULL | 输出价格（$/1M tokens） |
| cache_read_token_price | DECIMAL(20,10) | NULL | 缓存读价格 |
| cache_write_token_price | DECIMAL(20,10) | NULL | 缓存写价格 |
| supports_streaming | BOOLEAN | DEFAULT TRUE | 是否支持流式 |
| max_tokens | INT | DEFAULT 4096 | 最大token数 |
| status | TINYINT | DEFAULT 1 | 1-启用，0-禁用 |
| description | TEXT | NULL | 模型说明 |
| tags | JSON | NULL | 标签列表 |
| sort_order | INT | DEFAULT 0 | 排序值 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_model_name` (model_name)
- `idx_provider` (provider)
- `idx_status` (status)
- `idx_sort_order` (sort_order)

**业务规则**:
- 价格单位: 美元/百万 tokens
- tags 示例: `["推荐", "低价", "新品"]`
- 支持动态添加新模型
- 价格更新不影响历史调用记录

---

### 10. subscription_plans - 订阅套餐表

配置订阅套餐信息。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 套餐ID |
| plan_name | VARCHAR(50) | UNIQUE, NOT NULL | 套餐标识 |
| display_name | VARCHAR(100) | NOT NULL | 显示名称 |
| description | TEXT | NULL | 套餐说明 |
| original_price | DECIMAL(10,2) | NOT NULL | 原价（元） |
| price | DECIMAL(10,2) | NOT NULL | 现价（元） |
| quota_amount | DECIMAL(10,2) | NOT NULL | 额度金额（元） |
| features | JSON | NULL | 功能特性列表 |
| color_theme | VARCHAR(20) | DEFAULT 'blue' | green/blue/pink |
| badge_text | VARCHAR(50) | NULL | 徽章文字 |
| status | TINYINT | DEFAULT 1 | 1-启用，0-禁用 |
| sort_order | INT | DEFAULT 0 | 排序 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_plan_name` (plan_name)
- `idx_status` (status)
- `idx_sort_order` (sort_order)

**示例数据**:
```json
{
  "plan_name": "max_100",
  "features": ["30天有效期", "120元额度", "支持所有模型", "优先技术支持"]
}
```

---

### 11. subscriptions - 订阅记录表

记录用户的订阅历史。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 订阅ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| plan_id | BIGINT | FK → subscription_plans(id) | 套餐ID |
| plan_name | VARCHAR(100) | NOT NULL | 套餐名称（冗余） |
| amount | DECIMAL(10,2) | NOT NULL | 支付金额（元） |
| quota_amount | DECIMAL(10,2) | NOT NULL | 获得额度（元） |
| start_date | DATE | NOT NULL | 开始日期 |
| end_date | DATE | NOT NULL | 结束日期 |
| status | VARCHAR(20) | DEFAULT 'active' | active/expired/cancelled |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_user_id` (user_id)
- `idx_plan_id` (plan_id)
- `idx_status` (status)
- `idx_end_date` (end_date)

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE
- `plan_id` → `subscription_plans(id)`

**业务规则**:
- 订阅成功后立即发放额度到余额
- 到期后状态自动变更为 expired
- 支持主动取消订阅

---

### 12. user_quotas - 用户配额管理表

管理用户的每日/每月配额限制。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 配额ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| quota_type | ENUM | NOT NULL | daily/monthly/custom |
| quota_amount | DECIMAL(20,2) | DEFAULT 0 | 配额金额（元） |
| used_amount | DECIMAL(20,2) | DEFAULT 0 | 已使用金额（元） |
| reset_at | DATETIME | NOT NULL | 下次重置时间 |
| is_enabled | BOOLEAN | DEFAULT TRUE | 是否启用 |
| alert_threshold | DECIMAL(5,2) | DEFAULT 80.00 | 告警阈值（%） |
| last_alert_at | DATETIME | NULL | 最后告警时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

**索引**:
- `uk_user_quota_type` (user_id, quota_type) - 唯一约束
- `idx_user_id` (user_id)
- `idx_reset_at` (reset_at)

**外键**:
- `user_id` → `users(id)` ON DELETE CASCADE

**业务规则**:
- 每日配额: 每天 00:00 重置
- 每月配额: 每月 1 号 00:00 重置
- 达到阈值时发送告警
- 超出配额后拒绝请求

---

### 13. tickets - 工单表

用户提交的工单。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 工单ID |
| user_id | BIGINT | FK → users(id), NOT NULL | 用户ID |
| subject | VARCHAR(200) | NOT NULL | 主题 |
| content | TEXT | NOT NULL | 内容 |
| status | VARCHAR(20) | DEFAULT 'open' | open/in_progress/closed |
| priority | VARCHAR(20) | DEFAULT 'normal' | low/normal/high/urgent |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_user_id` (user_id)
- `idx_status` (status)
- `idx_priority` (priority)
- `idx_created_at` (created_at)

---

### 14. ticket_messages - 工单消息表

工单的��话记录。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 消息ID |
| ticket_id | BIGINT | FK → tickets(id), NOT NULL | 工单ID |
| user_id | BIGINT | NULL | 用户ID（用户消息） |
| admin_id | BIGINT | NULL | 管理员ID（管理员回复） |
| message | TEXT | NOT NULL | 消息内容 |
| is_staff | TINYINT | DEFAULT 0 | 1-管理员，0-用户 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**:
- `idx_ticket_id` (ticket_id)
- `idx_created_at` (created_at)

---

### 15. system_config - 系统配置表

系统级配置项。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 配置ID |
| config_key | VARCHAR(100) | UNIQUE, NOT NULL | 配置键 |
| config_value | TEXT | NULL | 配置值 |
| description | VARCHAR(255) | NULL | 配置说明 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_config_key` (config_key)

**常用配置**:
- `copilot_api_url`: Copilot API 地址
- `rate_limit_per_minute`: 速率限制
- `daily_free_quota`: 每日免费额度
- `system_notice`: 系统公告

---

### 16. admins - 管理员表

后台管理员账户。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 管理员ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt） |
| role | VARCHAR(20) | DEFAULT 'admin' | 角色 |
| status | TINYINT | DEFAULT 1 | 1-正常，0-禁用 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

**索引**:
- `idx_username` (username)

**默认账户**:
- 用户名: `admin`
- 密码: `admin123`

---

## 📊 视图

### v_user_stats - 用户统计视图

聚合用户的调用统计信息。

```sql
CREATE OR REPLACE VIEW v_user_stats AS
SELECT
    u.id,
    u.email,
    u.balance,
    u.status,
    u.created_at,
    COUNT(DISTINCT ac.id) AS total_calls,
    COALESCE(SUM(ac.cost), 0) AS total_cost,
    COALESCE(SUM(ac.input_tokens), 0) AS total_input_tokens,
    COALESCE(SUM(ac.output_tokens), 0) AS total_output_tokens,
    MAX(ac.created_at) AS last_call_time
FROM users u
LEFT JOIN api_calls ac ON u.id = ac.user_id
GROUP BY u.id;
```

**用途**:
- 用户总览页面
- 管理后台用户列表
- 快速统计分析

---

## 🔐 安全设计

### 数据加密

- **密码**: BCrypt 哈希（work factor: 10）
- **后端账户 Token**: AES-256-GCM 加密
- **API Key**: 明文存储（本身是随机生成的安全字符串）

### 数据脱敏

展示给用户时脱敏处理：
- API Key: 显示 `sk-****xxxx` (前后各4位)
- 邮箱: `u***@example.com`

### 权限控制

- 用户只能访问自己的数据
- 外键级联删除保证数据一致性
- 管理员操作需要 ADMIN 角色

---

## 📈 性能优化

### 索引策略

1. **单列索引**: 高频查询字段
2. **复合索引**: 联合查询字段
3. **覆盖索引**: 避免回表查询
4. **前缀索引**: VARCHAR 字段适当截取

### 分区策略

**api_calls 表按月分区**:
```sql
ALTER TABLE api_calls
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    ...
);
```

### 查询优化

- 使用 EXPLAIN 分析查询计划
- 避免 SELECT *
- 合理使用 JOIN
- 定期 ANALYZE TABLE

---

## 🛠️ 数据迁移

### 初始化脚本

```bash
mysql -u root -p < src/main/resources/db/init_database.sql
```

### 版本管理

建议使用 Flyway 或 Liquibase 进行数据库版本管理：

```
db/
  ├── V1.0.0__init_database.sql
  ├── V1.1.0__add_cache_token_fields.sql
  └── V1.2.0__add_user_quotas_table.sql
```

---

## 📊 容量规划

| 表名 | 日增长量 | 月存储 | 年存储 | 建议分区 |
|------|---------|--------|--------|---------|
| users | 100 | 3K | 36K | 无 |
| api_calls | 100K | 3M | 36M | 按月 |
| balance_log | 50K | 1.5M | 18M | 按月 |
| recharge_orders | 1K | 30K | 360K | 无 |
| session_mappings | 20K | - | - | 定期清理 |

**存储估算** (1年):
- 数据表: ~50GB
- 索引: ~30GB
- 总计: ~80GB

---

## 🔍 监控指标

### 数据库性能

- 慢查询日志 (>1s)
- 连接池使用率
- 缓存命中率
- 死锁检测

### 业务指标

- 每日新增用户数
- 每日 API 调用量
- 每日充值金额
- 活跃用户数

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
