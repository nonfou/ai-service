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

默认开发配置会使用内存 H2。如需通过环境变量指定上游 Copilot 地址，可设置：

```bash
# PowerShell
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
