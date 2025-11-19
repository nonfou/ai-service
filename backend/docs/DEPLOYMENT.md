# 部署指南

## 📋 环境要求

### 基础环境

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| Java | 21 | 21 | 必须使用 Java 21+ |
| MySQL | 8.0 | 8.0.33+ | 数据库 |
| Redis | 6.0 | 7.0+ | 缓存服务 |
| Maven | 3.8 | 3.9+ | 构建工具 |

### 硬件要求

**开发环境**:
- CPU: 2 核+
- 内存: 4GB+
- 磁盘: 10GB+

**生产环境**:
- CPU: 4 核+
- 内存: 8GB+
- 磁盘: 50GB+（含日志）
- 网络: 100Mbps+

## 🚀 快速部署

### 方式一：本地部署

#### 1. 安装依赖

```bash
# 安装 Java 21
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# macOS
brew install openjdk@21

# 安装 MySQL
sudo apt install mysql-server-8.0

# 安装 Redis
sudo apt install redis-server
```

#### 2. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/backend/src/main/resources/db/init_database.sql
```

#### 3. 配置环境变量

```bash
# 复制配置文件
cp .env.example .env

# 编辑配置
vim .env
```

**必填配置**:
```properties
# 数据库
DB_URL=jdbc:mysql://localhost:3306/ai_api_platform?useUnicode=true&characterEncoding=utf8mb4
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# JWT
JWT_SECRET=your-secret-key-at-least-256-bits-long
JWT_EXPIRATION=604800000

# 邮件服务
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=your-authorization-code
```

#### 4. 构建项目

```bash
cd backend
mvn clean package -DskipTests
```

#### 5. 启动应用

```bash
java -jar target/ai-api-platform-1.0.0.jar
```

或使用 Maven:
```bash
mvn spring-boot:run
```

#### 6. 验证部署

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 测试登录
curl -X POST http://localhost:8080/api/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

---

### 方式二：Docker 部署

#### 1. 构建镜像

```bash
cd backend

# 构建镜像
docker build -t ai-api-platform:latest .
```

#### 2. 使用 Docker Compose

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: ai-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: ai_api_platform
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./src/main/resources/db/init_database.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - ai-network
    command: --default-authentication-plugin=mysql_native_password

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: ai-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - ai-network
    command: redis-server --appendonly yes

  # 应用服务
  app:
    image: ai-api-platform:latest
    container_name: ai-app
    depends_on:
      - mysql
      - redis
    environment:
      DB_URL: jdbc:mysql://mysql:3306/ai_api_platform?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
      DB_USERNAME: root
      DB_PASSWORD: root123
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ""
      REDIS_DATABASE: 0
      JWT_SECRET: your-jwt-secret-key-at-least-256-bits-long-replace-this
      JWT_EXPIRATION: 604800000
      MAIL_HOST: smtp.qq.com
      MAIL_PORT: 587
      MAIL_USERNAME: your-email@qq.com
      MAIL_PASSWORD: your-authorization-code
    ports:
      - "8080:8080"
    networks:
      - ai-network
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:

networks:
  ai-network:
    driver: bridge
```

#### 3. 启动服务

```bash
docker-compose up -d
```

#### 4. 查看日志

```bash
# 查看应用日志
docker-compose logs -f app

# 查看所有服务
docker-compose ps
```

#### 5. 停止服务

```bash
docker-compose down
```

---

## 🌐 生产环境部署

### 使用 Nginx 反向代理

#### 1. 安装 Nginx

```bash
sudo apt install nginx
```

#### 2. 配置 Nginx

创建 `/etc/nginx/sites-available/ai-api-platform`:

```nginx
upstream backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name api.yourdomain.com;

    # 日志
    access_log /var/log/nginx/ai-api-access.log;
    error_log /var/log/nginx/ai-api-error.log;

    # 客户端最大请求体大小
    client_max_body_size 10M;

    # 代理配置
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # SSE 流式支持
        proxy_buffering off;
        proxy_cache off;
    }

    # 健康检查
    location /actuator/health {
        proxy_pass http://backend/actuator/health;
        access_log off;
    }
}
```

#### 3. 启用配置

```bash
sudo ln -s /etc/nginx/sites-available/ai-api-platform /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 4. 配置 HTTPS

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 申请证书
sudo certbot --nginx -d api.yourdomain.com

# 自动续期
sudo certbot renew --dry-run
```

---

### 使用 Systemd 管理

#### 1. 创建服务文件

创建 `/etc/systemd/system/ai-api-platform.service`:

```ini
[Unit]
Description=AI API Platform
After=network.target mysql.service redis.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/ai-api-platform
ExecStart=/usr/bin/java -jar /opt/ai-api-platform/ai-api-platform-1.0.0.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

# 环境变量
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC"

# 日志
StandardOutput=append:/var/log/ai-api-platform/app.log
StandardError=append:/var/log/ai-api-platform/error.log

[Install]
WantedBy=multi-user.target
```

#### 2. 启动服务

```bash
# 重载配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start ai-api-platform

# 开机自启
sudo systemctl enable ai-api-platform

# 查看状态
sudo systemctl status ai-api-platform

# 查看日志
sudo journalctl -u ai-api-platform -f
```

---

## 📊 性能优化

### JVM 参数优化

```bash
java -jar app.jar \
  -Xms4g \                          # 初始堆大小
  -Xmx8g \                          # 最大堆大小
  -XX:+UseG1GC \                    # 使用 G1 垃圾回收器
  -XX:MaxGCPauseMillis=200 \        # 最大 GC 暂停时间
  -XX:+HeapDumpOnOutOfMemoryError \ # OOM 时生成堆转储
  -XX:HeapDumpPath=/logs/heapdump.hprof
```

### 数据库优化

```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';

-- 启用慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- 查看表索引使用情况
SELECT * FROM sys.schema_unused_indexes;

-- 分析表
ANALYZE TABLE api_calls;
```

### Redis 优化

```bash
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
save ""  # 关闭 RDB 持久化（仅缓存场景）
appendonly yes  # 开启 AOF
```

---

## 🔍 监控和日志

### 应用日志

**日志级别配置** (`application-prod.yml`):

```yaml
logging:
  level:
    root: INFO
    com.nonfou.github: INFO
    org.springframework.web: WARN
  file:
    name: /var/log/ai-api-platform/app.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 监控端点

启用 Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

**访问监控端点**:
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info

# 指标数据
curl http://localhost:8080/actuator/metrics
```

---

## 🔐 安全加固

### 1. 数据库安全

```bash
# 创建专用数据库用户
CREATE USER 'ai_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_api_platform.* TO 'ai_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Redis 安全

```bash
# 设置密码
requirepass your_strong_password

# 禁用危险命令
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command CONFIG ""
```

### 3. 防火墙配置

```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# 限制 MySQL 和 Redis 只允许本地访问
sudo ufw deny 3306/tcp
sudo ufw deny 6379/tcp
```

---

## 🔄 更新和回滚

### 零停机更新

```bash
# 1. 构建新版本
mvn clean package

# 2. 备份当前版本
cp app.jar app.jar.backup

# 3. 部署新版本
cp target/ai-api-platform-1.0.1.jar /opt/ai-api-platform/app.jar

# 4. 优雅重启
sudo systemctl restart ai-api-platform

# 5. 验证
curl http://localhost:8080/actuator/health
```

### 回滚

```bash
# 恢复备份
cp app.jar.backup app.jar

# 重启服务
sudo systemctl restart ai-api-platform
```

---

## 📝 检查清单

部署前检查：

- [ ] Java 21 已安装
- [ ] MySQL 8.0 已安装并初始化
- [ ] Redis 已安装并运行
- [ ] 环境变量已配置
- [ ] 数据库脚本已执行
- [ ] 邮件服务已配置并测试
- [ ] 后端 AI 账户已添加
- [ ] 防火墙规则已配置
- [ ] SSL 证书已配置（生产环境）
- [ ] 监控和日志已配置

部署后验证：

- [ ] 应用启动成功
- [ ] 健康检查通过
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] 邮件发送正常
- [ ] API 调用正常
- [ ] 日志输出正常

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
