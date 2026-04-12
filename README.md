# ai-service

Spring Boot AI 代理服务，包含后端 API 与前端管理端。当前运行主路径只依赖一个可配置的 Copilot Relay 地址，对外保留 OpenAI / Anthropic / Codex 兼容接口。

## 保留能力

- `POST /v1/chat/completions`
- `GET /v1/models`
- `POST /v1/embeddings`
- `POST /v1/responses`
- `POST /v1/messages`
- `POST /v1/messages/count_tokens`
- 模型别名映射

## 当前约束

- `/v1/*` 不做本地 API Key 鉴权
- 运行主路径不依赖用户余额、订阅、计费和多上游路由

## 本地启动

要求：

- 本机可用 JDK 21 和 Maven
- 如需运行前端管理端，需安装 Node.js 18+ 与 pnpm 9+

```bash
cd backend
mvn spring-boot:run
```

默认开发配置会使用仓库根目录下的 SQLite 文件库 `data/ai_api_platform_dev.db`，首次启动会自动初始化表结构和默认管理员。

默认管理员账号：

```text
username: admin
password: admin123
```

如需通过环境变量指定开发库路径或上游 Copilot 地址，可设置：

```bash
# PowerShell
$env:DB_PATH="../data/ai_api_platform_dev.db"
$env:COPILOT_PROXY_BASE_URL="<your-copilot-relay-url>"
$env:COPILOT_PROXY_API_KEY=""
```

如果上游不是 `/v1` 根路径，请直接填完整 Base URL。

前端管理端本地启动：

```bash
cd frontend
pnpm install
pnpm dev
```

默认访问地址：`http://localhost:5174`

## 升级说明

升级到包含 Token 统计功能的版本时，建议按下面流程处理：

1. 备份当前 SQLite 数据库文件。
2. 替换后端程序和前端静态资源。
3. 重启服务。

应用启动时会自动执行两类数据库脚本：

- `schema_sqlite.sql`：用于基础表结构初始化。
- `db/migration/sqlite/V*.sql`：用于按版本顺序执行增量升级脚本。

这次升级包含 `V20260413_001__create_token_usage_records.sql`，会自动创建 `token_usage_records` 表和相关索引，不需要手工执行 SQL。

如果你需要临时关闭自动迁移，可以设置：

```bash
# PowerShell
$env:DB_MIGRATION_ENABLED="false"
```

正常情况下不建议关闭。服务启动日志中会输出“数据库迁移执行成功”或对应失败原因；如果迁移失败，应用会直接中止启动，避免出现半升级状态。

## Docker 启动

开发/最小部署使用内存 H2：

```bash
cd docker
cp .env.example .env.copilot
# 编辑 COPILOT_PROXY_BASE_URL

docker compose --env-file .env.copilot -f docker-compose.copilot.yml up -d --build
```

完整部署使用文件型 H2，并通过 `DB_PATH` 持久化数据，同时启动前端管理端：

```bash
cd docker
cp .env.example .env
# 编辑 .env，至少设置 DB_PATH、JWT_SECRET、ENCRYPTION_KEY
docker compose --env-file .env -f docker-compose.yml up -d --build
```

其中 `ENCRYPTION_KEY` 必须是 16/24/32 字节字符串；建议直接使用 32 个随机 ASCII 字符。

完整部署对外只暴露一个入口端口，默认 `9000`，同时承载前端页面和后端 API 代理。

## 调用示例

```bash
curl http://localhost:8080/v1/models
```

```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-sonnet-4.5",
    "messages": [{"role": "user", "content": "hello"}],
    "stream": false
  }'
```

## 健康检查

- `GET /health`
- `GET /live`
- `GET /ready`
