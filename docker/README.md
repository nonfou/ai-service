# Docker 部署

当前仓库保留两套部署入口:

- `docker-compose.copilot.yml`: 开发/最小部署，使用内存 H2
- `docker-compose.yml`: 生产部署，使用文件型 H2 持久化

## 1. 开发/最小部署

```bash
cd docker
cp .env.copilot.example .env.copilot
```

至少修改以下配置：

- `COPILOT_PROXY_BASE_URL`：指向你的 Copilot Relay，例如 `http://127.0.0.1:4141/v1`
- `COPILOT_PROXY_API_KEY`：如果上游需要额外鉴权则填写，否则留空

启动:

```bash
docker compose --env-file .env.copilot -f docker-compose.copilot.yml up -d --build
```

说明:

- 默认 `dev` profile
- 默认内存 H2，重启后数据会重建
- 适合只验证代理链路

## 2. 生产部署

准备生产环境变量:

```bash
cd docker
# 编辑 .env，设置安全密钥与 DB_PATH
```

生产启动:

```bash
docker compose --env-file .env -f docker-compose.yml up -d --build
```

说明:

- 默认 `prod` profile
- 数据通过 `./data:/app/data` 挂载到文件型 H2
- `DB_PATH` 默认为 `/app/data/ai_api_platform`

## 3. 验证

```bash
curl http://localhost:8080/health
curl http://localhost:8080/v1/models
```

若你的 Copilot Relay 不带 `/v1` 前缀，请把 `COPILOT_PROXY_BASE_URL` 配成完整实际地址。
