# ai-service

最小化的 Spring Boot AI 代理服务。当前运行主路径只依赖一个可配置的 Copilot Relay 地址，对外保留 OpenAI / Anthropic / Codex 兼容接口。

## 保留能力

- `POST /v1/chat/completions`
- `GET /v1/models`
- `POST /v1/embeddings`
- `POST /v1/responses`
- `POST /v1/messages`
- `POST /v1/messages/count_tokens`
- Claude Code 热身请求模型降级
- 模型别名映射

## 当前约束

- `/v1/*` 不做本地 API Key 鉴权
- 运行主路径不依赖用户余额、订阅、计费和多上游路由
- 现有前端、后台和业务模块仍保留在仓库中，但不再是最小部署方案的一部分

## 本地启动

要求：本机可用 JDK 21 和 Maven。

```bash
cd backend
mvn spring-boot:run
```

默认开发配置会使用内存 H2；真正需要配置的只有上游 Copilot 地址：

```bash
# PowerShell
$env:COPILOT_PROXY_BASE_URL="http://127.0.0.1:4141/v1"
$env:COPILOT_PROXY_API_KEY=""
```

如果上游不是 `/v1` 根路径，请直接填完整 Base URL。

## Docker 启动

开发/最小部署使用内存 H2：

```bash
cd docker
cp .env.copilot.example .env.copilot
# 编辑 COPILOT_PROXY_BASE_URL

docker compose --env-file .env.copilot -f docker-compose.copilot.yml up -d --build
```

生产部署使用文件型 H2，并通过 `DB_PATH` 持久化数据：

```bash
cd docker
# 编辑 .env，至少设置 DB_PATH、JWT_SECRET、VERIFY_CODE_SECRET、ENCRYPTION_KEY
docker compose --env-file .env -f docker-compose.yml up -d --build
```

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
