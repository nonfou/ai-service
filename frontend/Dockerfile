# ====================================================
# 多阶段构建 - AI API Platform Frontend
# 支持构建 user-portal 和 admin-portal
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

# 构建参数：指定要构建的项目
ARG BUILD_TARGET=all

# 构建前端应用
RUN if [ "$BUILD_TARGET" = "user" ]; then \
        pnpm build:user; \
    elif [ "$BUILD_TARGET" = "admin" ]; then \
        pnpm build:admin; \
    else \
        pnpm build:all; \
    fi

# ====================================================
# 第二阶段：导出构建产物
# ====================================================
FROM alpine:latest AS exporter

# 复制构建产物
COPY --from=builder /app/user-portal/dist /dist/user-portal
COPY --from=builder /app/admin-portal/dist /dist/admin-portal

# 默认命令：列出构建产物
CMD ["ls", "-la", "/dist"]
