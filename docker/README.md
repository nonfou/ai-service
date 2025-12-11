# AI API Platform - Docker 部署

## 目录结构

```
docker/
├── docker-compose.yml   # Docker Compose 配置
├── .env.example         # 环境变量示例
├── .env                 # 实际环境变量 (需创建)
├── conf/
│   └── my.cnf           # MySQL 配置
├── init/                # MySQL 初始化脚本
│   └── *.sql
└── README.md            # 本文档

backend/
└── Dockerfile           # 后端服务镜像构建
```

## 快速开始

### 1. 配置环境变量

```bash
cd docker
cp .env .env

# 编辑配置文件，填写实际值
vim .env
```

**必须配置的变量：**
- `MYSQL_ROOT_PASSWORD` - MySQL root 密码
- `MYSQL_PASSWORD` - 应用数据库密码
- `JWT_SECRET` - JWT 签名密钥 (至少32字符)
- `VERIFY_CODE_SECRET` - 验证码加密密钥
- `ENCRYPTION_KEY` - 数据加密密钥 (16/24/32 字节)

### 2. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看日志
docker-compose logs -f backend

# 查看服务状态
docker-compose ps
```

### 3. 停止服务

```bash
# 停止服务
docker-compose down

# 停止并删除数据卷 (会清除所有数据!)
docker-compose down -v
```

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| backend | 8080 | Spring Boot 后端 API |
| mysql | 3306 | MySQL 9.0 数据库 |
| redis | 6379 | Redis 7 缓存 |

## 健康检查

```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查 MySQL
docker exec ai-service-mysql mysqladmin ping -h localhost -u root -p

# 检查 Redis
docker exec ai-service-redis redis-cli ping
```

## 数据持久化

数据通过 Docker volumes 持久化：
- `mysql_data` - MySQL 数据
- `redis_data` - Redis 数据

## 生产环境建议

1. **安全配置**
   - 修改所有默认密码
   - 使用强密钥 (`openssl rand -base64 32`)
   - 设置 `COOKIE_SECURE=true`
   - 配置 HTTPS 反向代理

2. **性能调优**
   - 根据服务器配置调整 `JAVA_OPTS`
   - 调整 MySQL 和 Redis 内存限制
   - 配置连接池参数

3. **备份策略**
   - 定期备份 MySQL 数据
   - 备份 Redis 数据 (可选)
