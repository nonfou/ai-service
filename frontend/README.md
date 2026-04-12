# AI Service Frontend

`frontend/` 现在是单应用前端根目录，承载管理端页面，不再使用 workspace 子应用结构。

## 项目结构

```text
frontend/
├── src/                # 页面、路由、状态管理、接口封装
├── public/             # 静态资源
├── .env.development    # 开发环境变量
├── .env.production     # 生产环境变量
├── Dockerfile          # 前端镜像构建
├── index.html          # Vite 入口
├── package.json        # 前端脚本与依赖
└── vite.config.ts      # Vite 配置
```

## 技术栈

- Vue 3.5
- TypeScript 5.9
- Vite 7
- Element Plus 2.11
- Pinia 3
- Vue Router 4.6
- Axios 1.13
- ECharts 6

## 环境要求

- Node.js >= 18
- pnpm >= 9

## 本地开发

```bash
cd frontend
pnpm install
pnpm dev
```

默认访问地址：`http://localhost:5174`

## 构建

```bash
cd frontend
pnpm build
```

如需同时执行类型检查：

```bash
cd frontend
pnpm build:check
```

## 环境变量

开发环境 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_PORT=5174
```

生产环境 `frontend/.env.production`：

```env
VITE_API_BASE_URL=
VITE_PORT=5174
```

## 功能范围

当前前端提供：

1. 管理员登录
2. Copilot API Key 管理
3. Copilot `/usage` 使用进度查看

## Docker 构建

```bash
cd frontend
docker build -t ai-service-admin-portal .
```
