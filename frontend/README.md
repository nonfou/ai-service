# AI API Platform - Frontend

当前前端仅保留 `admin-portal` 管理端应用，用户端页面与相关工作区配置已移除。

## 项目结构

```text
frontend/
├── admin-portal/        # 管理端应用 (端口: 5174)
├── pnpm-workspace.yaml  # Workspace 配置
├── package.json         # 根脚本
└── README.md
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

安装依赖：

```bash
cd frontend
pnpm install
```

启动管理端：

```bash
cd frontend
pnpm dev
```

或：

```bash
cd frontend/admin-portal
pnpm dev
```

默认访问地址：`http://localhost:5174`

## 构建

```bash
cd frontend
pnpm build
```

## 环境变量

开发环境 `frontend/admin-portal/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_PORT=5174
```

生产环境 `frontend/admin-portal/.env.production`：

```env
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_PORT=5174
```

## 功能范围

当前保留的页面能力：

1. 管理员登录
2. Copilot API Key 管理
3. Copilot `/usage` 使用进度查看

## Docker 构建

```bash
cd frontend/admin-portal
docker build -t cc-web-admin-portal .
```

## 相关文档

- [后端 API 文档](../docs/backend/api.md)
- [Vue 3 文档](https://cn.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
