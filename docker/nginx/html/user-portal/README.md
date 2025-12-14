# 前端静态资源占位

此目录用于存放 user-portal 前端构建产物。

## 部署方式

### 方式一：使用 Docker 构建（推荐）

```bash
cd docker

# 构建并复制前端资源
docker-compose --profile build run --rm frontend-builder

# 启动服务
docker-compose up -d
```

### 方式二：本地构建后复制

```bash
# 1. 在 frontend 目录构建
cd frontend
pnpm install
pnpm build:user

# 2. 复制构建产物到此目录
cp -r user-portal/dist/* ../docker/nginx/html/user-portal/
```

## 目录结构

构建后应包含：
```
user-portal/
├── index.html
├── assets/
│   ├── *.js
│   ├── *.css
│   └── ...
└── ...
```
