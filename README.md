# AI API Platform

基于 GitHub Copilot 的 AI API 聚合服务平台

## 项目简介

本项目是一个完整的 AI API 中转服务平台，对外提供统一的 OpenAI 兼容 API 接口，支持对接多种 AI API（如 GitHub Copilot API 等），采用 Token 计费模式。

### 核心功能

#### 用户端功能
- **用户认证系统** - 邮箱验证码登录/注册，JWT Token 认证
- **多 API 密钥管理** - 支持创建多个 API 密钥，独立启用/禁用，查看使用情况
- **余额充值系统** - 支持支付宝/微信支付（当前为模拟支付）
- **订阅套餐系统** - 购买套餐获取额度，支持多种套餐方案
- **API 调用统计** - 实时查看调用次数、Token 消耗、费用统计
- **余额日志** - 完整的余额变动记录，支持筛选查询
- **工单系统** - 创建工单、查看工单状态、与管理员对话
- **AI 聊天接口** - OpenAI 兼容的聊天接口，支持多种 AI 模型

#### 管理端功能
- **用户管理** - 查看用户列表、启用/禁用用户、查看用户详情
- **平台统计** - 总用户数、总收入、总调用次数、今日数据
- **模型管理** - 配置 AI 模型、设置价格倍率、启用/禁用模型
- **订单管理** - 查看充值订单、订阅记录
- **工单处理** - 处理用户工单、回复消息

### 技术栈

**前端**
- Vue 3 + TypeScript + Vite
- TailwindCSS + HeadlessUI
- Vue Router + Pinia
- Axios + ECharts

**后端**
- Spring Boot 3.x
- JWT 认证
- MyBatis-Plus + MySQL 8.0
- Redis (可选)

**基础设施**
- Docker + Docker Compose (可选)
- 外部 AI API 服务（可配置）

### 项目统计

- **数据库表**: 12 个表
- **API 接口**: 42 个端点
- **代码量**: 3000+ 行
- **功能模块**: 8 个主要模块

## 快速开始

详细的部署指南请参考 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)

### 前置要求

- Java 17 或更高版本
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+
- Redis 6.0+ (可选)

### 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 全新安装
source backend/src/main/resources/db/schema.sql

# 从旧版本升级
source backend/src/main/resources/db/migration_v1_to_v2.sql
```

### 后端配置

编辑 `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_api_platform
    username: root
    password: your_password

jwt:
  secret: your-secret-key-change-this-in-production
  expiration: 86400000
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
npm install
npm run dev
# 访问: http://localhost:5173
```

### 默认账号

**管理员账号**
- 用户名: `admin`
- 密码: `admin123`
- 登录地址: `http://localhost:5173/admin`

**测试验证码**
- 如未配置 Redis，系统使用固定验证码: `123456`

## 数据库设计

### 数据库表清单

| 表名 | 说明 | 主要字段 |
|-----|------|---------|
| users | 用户表 | id, email, balance, status |
| api_keys | API密钥表 | id, user_id, key_name, api_key, status |
| recharge_orders | 充值订单表 | id, user_id, amount, pay_method, status |
| balance_log | 余额变动日志 | id, user_id, amount, type, remark |
| api_calls | API调用日志 | id, user_id, model, tokens, cost |
| models | 模型配置表 | id, model_name, provider, price_multiplier |
| system_config | 系统配置表 | id, config_key, config_value |
| admins | 管理员表 | id, username, password |
| subscription_plans | 订阅套餐表 | id, plan_name, price, quota_amount |
| subscriptions | 订阅记录表 | id, user_id, plan_id, start_date, end_date |
| tickets | 工单表 | id, user_id, subject, status, priority |
| ticket_messages | 工单消息表 | id, ticket_id, sender_type, message |

## API 接口

完整的 API 文档请参考 [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

### API 接口统计

- **用户认证**: 2 个接口 (发送验证码、登录)
- **用户信息**: 3 个接口 (获取信息、余额、统计)
- **API 密钥管理**: 5 个接口 (CRUD + 重新生成)
- **充值订单**: 3 个接口 (创建、列表、详情)
- **余额管理**: 3 个接口 (余额、日志、统计)
- **API 调用统计**: 4 个接口 (日志、今日统计、趋势、模型使用)
- **订阅套餐**: 4 个接口 (套餐列表、订阅、历史、取消)
- **工单系统**: 5 个接口 (创建、列表、详情、回复、关闭)
- **AI 聊天**: 3 个接口 (聊天、OpenAI 兼容、模型列表)
- **管理后台**: 7 个接口 (登录、用户管理、统计、模型管理)

### 使用示例

```bash
# 1. 登录获取 Token (测试环境验证码: 123456)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","code":"123456"}'

# 2. 创建 API 密钥
curl -X POST http://localhost:8080/api/user/api-keys \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"keyName":"生产环境密钥"}'

# 3. 调用 AI 接口
curl -X POST http://localhost:8080/api/chat \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "你好"}]
  }'
```

## 项目结构

```
cc-web/
├── frontend/                    # 前端项目
│   ├── src/
│   │   ├── api/                # API 接口封装
│   │   ├── components/         # 可复用组件
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── types/              # TypeScript 类型定义
│   │   └── views/              # 页面组件
│   │       ├── auth/           # 登录/注册页面
│   │       ├── dashboard/      # 用户控制台
│   │       ├── admin/          # 管理后台
│   │       ├── subscription/   # 订阅套餐
│   │       └── ticket/         # 工单系统
│   ├── Dockerfile
│   └── nginx.conf
├── backend/                     # 后端项目
│   ├── src/main/
│   │   ├── java/com/nonfou/github/
│   │   │   ├── config/         # 配置类 (JWT, CORS, MyBatis)
│   │   │   ├── controller/     # 控制器层 (7个主要 Controller)
│   │   │   ├── dto/            # 数据传输对象
│   │   │   ├── entity/         # 实体类 (12个表对应实体)
│   │   │   ├── mapper/         # MyBatis Mapper
│   │   │   ├── service/        # 业务逻辑层
│   │   │   ├── exception/      # 异常处理
│   │   │   └── util/           # 工具类
│   │   └── resources/
│   │       ├── db/             # 数据库脚本
│   │       │   ├── schema.sql         # 完整数据库脚本
│   │       │   └── migration_v1_to_v2.sql   # 迁移脚本
│   │       └── application.yml # 应用配置
│   ├── pom.xml
│   └── Dockerfile
├── DEPLOYMENT_GUIDE.md          # 部署指南
├── API_DOCUMENTATION.md         # API 接口文档
├── IMPLEMENTATION_SUMMARY.md    # 实现总结
├── docker-compose.yml           # Docker Compose 配置
├── .env.example                 # 环境变量示例
└── README.md                    # 项目文档
```

## 功能特性

### 安全性
- JWT Token 认证机制
- BCrypt 密码加密
- API 密钥安全存储
- 数据脱敏显示（API 密钥列表只显示部分字符）
- CORS 跨域配置
- 全局异常处理

### 性能优化
- Redis 缓存支持（可选）
- 数据库索引优化
- 分页查询支持
- 懒加载和按需加载

### 业务功能
- 多 API 密钥管理（每个用户可创建多个密钥）
- 模拟支付系统（支持支付宝/微信支付流程）
- 订阅套餐系统（购买套餐获取额度）
- 完整的余额变动日志
- 详细的 API 调用统计
- 工单系统（支持用户与管理员对话）
- OpenAI 兼容 API 接口

## 计费规则

默认计费规则（可在管理后台调整）:
- 输入 tokens: ¥4.1 / 1,000,000 tokens
- 输出 tokens: ¥16.4 / 1,000,000 tokens
- 支持模型价格倍率设置

## 文档索引

- [部署指南](DEPLOYMENT_GUIDE.md) - 详细的安装和配置说明
- [API 文档](API_DOCUMENTATION.md) - 完整的 API 接口参考
- [实现总结](IMPLEMENTATION_SUMMARY.md) - 技术实现细节和架构设计

## 常见问题

### 数据库连接失败
检查 MySQL 是否启动，用户名密码是否正确，防火墙是否开放 3306 端口

### 验证码发送失败
如未配置 Redis 或邮件服务，系统会自动使用固定验证码 `123456`

### API 密钥验证失败
确保密钥状态为启用，用户状态为正常，密钥格式正确

### 前端无法连接后端
检查 CORS 配置，确保允许前端域名跨域请求

## 更新日志

### v2.0.0 (2025-11-08)
- ✨ 新增多 API 密钥管理功能
- ✨ 新增订阅套餐系统
- ✨ 新增工单系统
- ✨ 新增详细的 API 调用统计
- ✨ 新增余额变动日志
- ✨ 新增管理后台功能
- 🔧 优化数据库设计
- 📝 完善 API 文档
- 📝 新增部署指南

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License
