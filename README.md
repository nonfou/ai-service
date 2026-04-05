# xCoder — AI API 聚合服务平台

基于 GitHub Copilot 的 AI API 中继服务平台，对外提供 OpenAI / Anthropic / Codex 兼容的统一 API 接口，支持多后端账户池、智能调度、Token 计费。

## 核心功能

### API 协议兼容

| 协议 | 端点 | 说明 |
|------|------|------|
| OpenAI Chat | `POST /v1/chat/completions` | 聊天补全，支持流式 (SSE) |
| OpenAI Models | `GET /v1/models` | 可用模型列表 |
| OpenAI Embeddings | `POST /v1/embeddings` | 文本向量化 |
| OpenAI Codex | `POST /v1/responses` | Codex Responses API |
| Anthropic Claude | `POST /v1/messages` | Claude Messages API，支持流式 |
| Claude Token Count | `POST /v1/messages/count_tokens` | Token 计数 |

- 支持 OpenAI SDK / Anthropic SDK / Claude Code / Codex CLI 直接接入
- 自动模型别名映射（如 `claude-sonnet-4-5` → `claude-sonnet-4.5`）

### 多账户中继系统

- 支持多个上游 Provider（Copilot、OpenRouter）
- 5 种调度策略：轮询 (ROUND_ROBIN)、最少使用 (LEAST_USED)、优先级 (PRIORITY)、成本优化 (COST_OPTIMIZED)、混合 (HYBRID)
- 会话粘性：同一对话路由到同一后端账户，Redis + MySQL 双存储
- 健康检查 + 熔断器（CLOSED / OPEN / HALF_OPEN 三态）
- AES-256 加密存储上游 Access Token

### 成本与计费

- 三层倍率加价：全局倍率 × 模型倍率 × 账户倍率
- 分项 Token 计价：输入 / 输出 / Cache 读取 / Cache 写入
- 用户配额管理：每日 / 每月限额，超限告警

### 用户端功能

- **邮箱验证码登录** — HttpOnly Cookie 认证，JWT 无状态
- **多 API 密钥管理** — 创建、启用/禁用、重新生成
- **Stripe 充值** — 信用卡支付，余额管理
- **订阅套餐** — 购买套餐获取额度
- **API 调用统计** — Token 消耗、费用趋势、模型分布、24h 时段分布
- **余额日志** — 完整变动记录
- **工单系统** — 创建工单、与管理员对话、邮件通知
- **快速开始向导** — Claude Code / Codex 安装配置指引

### 管理端功能

- **平台统计** — 总用户数、总收入、总调用次数
- **用户管理** — 启用/禁用、余额调整、配额设置
- **模型管理** — 配置模型价格倍率、启用/禁用
- **订阅套餐管理** — CRUD 套餐方案
- **后端账户管理** — Copilot / OpenRouter 账户池 CRUD、健康检查
- **工单处理** — 回复、优先级调整
- **订单管理** — 订单查看、退款

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | JDK 21 |
| Spring Boot | 3.2.5 | Web + Security + AOP + Validation |
| Spring Data Redis | — | Lettuce 客户端 |
| MyBatis-Plus | 3.5.7 | ORM |
| MySQL | 9.0 | 主数据库 |
| JWT | jjwt 0.12.3 | 认证 |
| Apache HttpClient | 5 | HTTP 连接池 |
| Hutool | 5.8.24 | 工具类 |
| Spring Boot Mail + Thymeleaf | — | 邮件发送 |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3.5 + TypeScript 5.9 | Composition API |
| Vite 7.1 | 构建工具 |
| Element Plus 2.11 | UI 组件库 |
| Pinia 3.0 | 状态管理 |
| ECharts 5.5 / Chart.js 4.5 | 图表 |
| Stripe.js | 支付集成 |
| pnpm Workspace | Monorepo（user-portal + admin-portal） |

### 基础设施

- Docker + Docker Compose（6 个服务）
- Nginx 反向代理（API 网关 + 前端静态资源）
- Redis 7（缓存、限流、验证码、会话粘性、使用量聚合）

## 项目统计

| 指标 | 数量 |
|------|------|
| 数据库表 | 16 个 + 1 视图 |
| API 接口 | 85+ |
| Controller | 16 个 |
| Service | 28 个 |
| Java 文件 | 159 个 |
| 预置模型 | 15+ |
| 调度策略 | 5 种 |

## 项目结构

```
ai-service/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/nonfou/github/
│   │   ├── config/                   # 配置类（JWT、CORS、Security、Properties）
│   │   ├── controller/               # 控制器（16 个）
│   │   ├── service/                  # 业务逻辑（28 个）
│   │   ├── entity/                   # 实体类（14 个）
│   │   ├── mapper/                   # MyBatis Mapper（15 个）
│   │   ├── dto/                      # 请求/响应/SSE 事件 DTO
│   │   ├── component/                # 熔断器组件
│   │   ├── filter/                   # 限流过滤器
│   │   ├── interceptor/              # 管理端安全拦截器
│   │   ├── security/                 # Spring Security 处理器
│   │   ├── enums/                    # 枚举类型
│   │   ├── exception/                # 异常体系
│   │   ├── annotation/               # 自定义注解（@RequireAdmin）
│   │   ├── aspect/                   # AOP 切面
│   │   └── util/                     # 工具类（加密、JWT、日志脱敏等）
│   ├── src/main/resources/
│   │   ├── db/init_database.sql      # 数据库初始化脚本
│   │   ├── templates/email/          # 邮件模板
│   │   ├── application.yml           # 主配置
│   │   ├── application-dev.yml       # 开发环境
│   │   ├── application-prod.yml      # 生产环境
│   │   └── application-example.yml   # 配置示例
│   └── pom.xml
├── frontend/                         # pnpm Monorepo
│   ├── user-portal/                  # 用户端（端口 5173）
│   │   └── src/
│   │       ├── api/                  # API 封装
│   │       ├── views/                # 页面组件
│   │       ├── components/           # 通用组件
│   │       ├── stores/               # Pinia Store
│   │       ├── router/               # 路由配置
│   │       └── utils/                # 工具函数
│   ├── admin-portal/                 # 管理端（端口 5174）
│   │   └── src/
│   │       ├── api/                  # 管理 API 封装
│   │       ├── views/admin/          # 管理页面
│   │       ├── components/           # 通用组件
│   │       └── stores/               # Pinia Store
│   └── package.json                  # Workspace 配置
└── docker/                           # Docker 部署
    ├── docker-compose.yml            # 6 个服务编排
    ├── .env                          # 环境变量
    ├── nginx/                        # Nginx 配置
    ├── init/                         # 数据库初始化 SQL
    └── conf/                         # MySQL 配置
```

## 快速开始

### 前置要求

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+
- pnpm 9+
- Redis 6.0+

### 数据库初始化

```bash
mysql -u root -p
source backend/src/main/resources/db/init_database.sql
```

### 后端配置

复制配置模板并填写必要参数：

```bash
cp backend/src/main/resources/application-example.yml backend/src/main/resources/application-local.yml
```

编辑 `application-local.yml`，至少配置以下项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_api_platform
    username: root
    password: your_password

jwt:
  secret: your-secret-key
  issuer: xcoder-local

verify-code:
  secret: your-verify-code-secret

encryption:
  key: your-encryption-key
```

### 启动服务

**后端**
```bash
cd backend
mvn clean spring-boot:run
# 访问: http://localhost:8080
```

**前端**
```bash
cd frontend
pnpm install
# 同时启动用户端和管理端
pnpm dev:all
# 用户端: http://localhost:5173
# 管理端: http://localhost:5174
```

### 默认账号

**管理员**
- 用户名: `admin` / 密码: `admin123`
- 登录地址: `http://localhost:5174`

**用户验证码**
- 未配置 Redis 或邮件服务时，使用固定验证码: `123456`

## Docker 部署

```bash
cd docker

# 编辑 .env 文件，填写必要的环境变量
# 必填: JWT_SECRET, ENCRYPTION_KEY, VERIFY_CODE_SECRET

docker-compose up -d --build
```

### 服务列表

| 服务 | 端口 | 说明 |
|------|------|------|
| backend | 8080 | Spring Boot 后端 |
| mysql | 3306 | MySQL 9.0 数据库 |
| redis | 6379 | Redis 7 缓存 |
| nginx | 9000 | API 反向代理网关 |
| user-portal | 80 | 用户端前端 |
| admin-portal | 9001 | 管理端前端 |

## API 使用示例

### OpenAI 兼容接口

```bash
# 聊天补全（非流式）
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "你好"}],
    "stream": false
  }'

# 聊天补全（流式）
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "你好"}],
    "stream": true
  }'
```

### Anthropic Claude 兼容接口

```bash
curl -X POST http://localhost:8080/v1/messages \
  -H "x-api-key: YOUR_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 1024,
    "messages": [{"role": "user", "content": "你好"}]
  }'
```

### Codex Responses 接口

```bash
curl -X POST http://localhost:8080/v1/responses \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-5.1-codex",
    "input": "写一个 Hello World"
  }'
```

### 模型列表

```bash
curl http://localhost:8080/v1/models \
  -H "Authorization: Bearer YOUR_API_KEY"
```

## 数据库设计

| 表名 | 说明 |
|------|------|
| users | 用户账户 |
| api_keys | API 密钥（多密钥） |
| recharge_orders | 充值订单 |
| balance_log | 余额变动日志 |
| api_calls | API 调用日志（含 Token、费用、会话） |
| models | 模型配置（含分项 Token 价格） |
| subscription_plans | 订阅套餐 |
| subscriptions | 用户订阅记录 |
| tickets | 工单 |
| ticket_messages | 工单消息 |
| system_config | 系统配置 |
| admins | 管理员账户 |
| backend_accounts | 后端账户池（Copilot / OpenRouter） |
| user_account_bindings | 用户-账户绑定 |
| session_mappings | 会话粘性映射 |
| user_quotas | 用户配额（每日/每月） |
| v_user_stats | 用户统计视图 |

## 安全机制

- **用户认证**: 邮箱验证码登录，HttpOnly Cookie 存储 JWT（SameSite=Strict）
- **管理端认证**: 用户名密码登录，localStorage 存储 Token
- **API 认证**: Bearer Token / x-api-key 自定义密钥
- **数据加密**: AES-256/GCM 加密后端账户 Access Token
- **密码安全**: BCrypt 哈希
- **限流**: Redis 滑动窗口限流（普通 60 RPM，API 30 RPM）
- **管理端安全**: IP 白名单、登录失败锁定、请求频率限制
- **日志脱敏**: 邮箱等敏感信息自动遮蔽

## 健康检查

Kubernetes 风格探针：

| 端点 | 说明 |
|------|------|
| `GET /health` / `/api/health` | 存活探针 |
| `GET /live` / `/api/live` | 存活探针（别名） |
| `GET /ready` / `/api/ready` | 就绪探针（检查 MySQL + Redis 连接） |

## 许可证

MIT License
