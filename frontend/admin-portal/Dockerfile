# ====================================================
# 多阶段构建 - Admin Portal
# ====================================================

# 第一阶段：构建环境
FROM node:20-alpine AS builder
WORKDIR /app

# 安装 pnpm
RUN corepack enable && corepack prepare pnpm@9 --activate

# 复制 pnpm 工作空间配置（利用 Docker 缓存）
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY user-portal/package.json ./user-portal/
COPY admin-portal/package.json ./admin-portal/

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源码
COPY . .

# 构建 admin-portal
RUN pnpm build:admin

# ====================================================
# 第二阶段：生产环境
# ====================================================
FROM nginx:alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 复制构建产物
COPY --from=builder /app/admin-portal/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
