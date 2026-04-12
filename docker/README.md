# Docker 部署

当前仓库保留两套部署入口:

- `docker-compose.copilot.yml`: 开发/最小部署，使用内存 H2
- `docker-compose.yml`: 完整部署，包含后端、API 反向代理和前端管理端

`docker/` 目录当前只保留实际生效的 compose、nginx 配置和环境变量示例文件。

## 1. 开发/最小部署

```bash
cd docker
cp .env.example .env.copilot
```

至少修改以下配置：

- `COPILOT_PROXY_BASE_URL`：按实际运行时动态配置；不再提供默认值
- `COPILOT_PROXY_API_KEY`：如果上游需要额外鉴权则填写，否则留空

启动:

```bash
docker compose --env-file .env.copilot -f docker-compose.copilot.yml up -d --build
```

说明:

- 默认 `dev` profile
- 默认内存 H2，重启后数据会重建
- 适合只验证代理链路

## 2. 完整部署

准备完整部署环境变量:

```bash
cd docker
cp .env.example .env
# 编辑 .env，至少设置 JWT_SECRET、ENCRYPTION_KEY
```

`ENCRYPTION_KEY` 必须是 16/24/32 字节的随机字符串；最简单是直接填 32 个 ASCII 字符。

示例：

```bash
ENCRYPTION_KEY=replace_with_32_chars_secret_key
```

生产启动:

```bash
docker compose --env-file .env -f docker-compose.yml up -d --build
```

说明:

- 默认 `prod` profile
- 数据通过 `./data:/app/data` 挂载到文件型 H2
- `DB_PATH` 默认示例为 `/app/data/ai_api_platform.db`
- 会同时启动后端、API Nginx 和前端管理端

## 3. 验证

```bash
curl http://localhost:8080/health
curl http://localhost:8080/v1/models
curl http://localhost:9001/health
```

`COPILOT_PROXY_BASE_URL` 现在没有默认值；如需通过环境变量提供，请填写完整实际地址。
