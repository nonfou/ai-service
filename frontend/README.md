# AI API Platform - Frontend

基于 Vue 3 + TypeScript + Element Plus 的 AI API 平台前端项目,采用 pnpm workspace 管理的 Monorepo 架构。

## 项目结构

```
frontend/
├── user-portal/          # 用户端应用 (端口: 5173)
│   ├── src/
│   │   ├── api/         # API 接口定义
│   │   ├── components/  # 公共组件
│   │   ├── router/      # 路由配置
│   │   ├── stores/      # Pinia 状态管理
│   │   ├── utils/       # 工具函数
│   │   ├── views/       # 页面组件
│   │   ├── App.vue
│   │   ├── main.ts
│   │   └── style.css
│   ├── .env.development
│   ├── .env.production
│   ├── package.json
│   └── vite.config.ts
│
├── admin-portal/        # 管理端应用 (端口: 5174)
│   ├── src/
│   │   ├── api/        # API 接口定义
│   │   ├── router/     # 路由配置
│   │   ├── stores/     # Pinia 状态管理
│   │   ├── utils/      # 工具函数
│   │   ├── views/      # 页面组件
│   │   ├── App.vue
│   │   ├── main.ts
│   │   └── style.css
│   ├── .env.development
│   ├── .env.production
│   ├── package.json
│   └── vite.config.ts
│
├── pnpm-workspace.yaml  # Workspace 配置
├── package.json         # 根配置
└── README.md           # 本文档
```

## 技术栈

- **包管理器**: pnpm workspace
- **框架**: Vue 3.5 + TypeScript 5.9
- **构建工具**: Vite 7.1
- **UI 库**: Element Plus 2.11
- **状态管理**: Pinia 3.0
- **路由**: Vue Router 4.6
- **HTTP 客户端**: Axios 1.13
- **数据可视化**: ECharts 5.5 (仅 admin-portal)

## 环境要求

- Node.js >= 18.0.0
- pnpm >= 9.0.0

## 快速开始

### 1. 安装依赖

```bash
# 在 frontend 目录下执行
pnpm install
```

### 2. 启动开发服务器

**方式一: 分别启动**

```bash
# 启动用户端 (http://localhost:5173)
cd user-portal
pnpm dev

# 启动管理端 (http://localhost:5174)
cd admin-portal
pnpm dev
```

**方式二: 使用 workspace 命令 (推荐)**

```bash
# 在 frontend 根目录执行

# 启动用户端
pnpm --filter @cc-web/user-portal dev

# 启动管理端
pnpm --filter @cc-web/admin-portal dev
```

### 3. 生产构建

```bash
# 构建用户端
cd user-portal
pnpm build

# 构建管理端
cd admin-portal
pnpm build
```

构建产物会生成在各自的 `dist` 目录。

## 环境配置

### 用户端 (user-portal)

**开发环境** (`.env.development`):
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_PORT=5173
```

**生产环境** (`.env.production`):
```env
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_PORT=5173
```

### 管理端 (admin-portal)

**开发环境** (`.env.development`):
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_PORT=5174
```

**生产环境** (`.env.production`):
```env
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_PORT=5174
```

## 项目特点

### 1. 完全独立
- 用户端和管理端是完全独立的应用
- 删除了 shared 包,各自维护独立代码
- 可以独立打包、部署和扩展

### 2. 路由隔离
- **用户端**: 用户相关路由,守卫检查 `token`
- **管理端**: 管理员路由,守卫检查 `adminToken`

### 3. 依赖优化
- 用户端包含 ECharts (用于使用统计图表)
- 管理端包含 ECharts (用于数据可视化)
- 按需加载,优化包体积

## 功能说明

### 用户端功能 (user-portal)

1. **用户认证** - 邮箱验证码登录/注册
2. **模型浏览** - 查看所有 AI 模型和定价
3. **仪表盘**
   - 数据概览 (余额、调用次数、消费)
   - API 密钥管理 (创建、启用/禁用、重新生成、删除)
   - 使用统计 (图表展示、趋势分析)
   - 调用日志 (筛选、详情查看)
4. **订阅套餐** - 查看和订阅套餐
5. **充值管理** - 余额充值、充值历史
6. **帮助文档** - 快速开始指南、API 文档、代码示例

### 管理端功能 (admin-portal)

1. **管理员认证** - 用户名密码登录
2. **数据概览** - 用户/订单/工单统计
3. **用户管理** - 查看、启用/禁用、调整余额
4. **模型管理** - 启用/禁用、更新配置
5. **套餐管理** - CRUD 操作
6. **订单管理** - 查看、完成订单、退款
7. **工单管理** - 状态/优先级管理、回复

## 后端 API

前端项目需要后端 API 服务支持,默认地址: `http://localhost:8080`

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

API 文档: `docs/backend/api.md`

## 常见问题

### 1. 安装依赖失败

```bash
# 清理缓存
pnpm store prune

# 删除依赖重新安装
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

### 2. 端口被占用

```bash
# Windows
netstat -ano | findstr :5173
taskkill /PID <进程ID> /F

# Linux/Mac
lsof -ti:5173 | xargs kill
```

### 3. API 请求失败

1. 确认后端服务已启动
2. 检查 `.env.development` 中的 API 地址
3. 检查后端 CORS 配置

### 4. TypeScript 编译错误

```bash
# 运行类型检查
cd user-portal  # 或 admin-portal
npx tsc --noEmit
```

## 部署说明

### Nginx 配置示例

**用户端:**
```nginx
server {
    listen 80;
    server_name user.yourdomain.com;
    root /var/www/user-portal/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }
}
```

**管理端:**
```nginx
server {
    listen 80;
    server_name admin.yourdomain.com;
    root /var/www/admin-portal/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }
}
```

### Docker 部署

每个项目都包含 `Dockerfile` 和 `nginx.conf`:

```bash
# 构建镜像
cd user-portal
docker build -t cc-web-user-portal .

cd ../admin-portal
docker build -t cc-web-admin-portal .

# 运行容器
docker run -d -p 80:80 cc-web-user-portal
docker run -d -p 8080:80 cc-web-admin-portal
```

## 开发建议

1. **代码规范** - 遵循 Vue 3 Composition API 风格
2. **类型安全** - 充分利用 TypeScript 类型检查
3. **组件拆分** - 保持组件单一职责
4. **性能优化** - 使用路由懒加载和代码分割
5. **测试** - 编写单元测试和 E2E 测试

## 端口分配

| 项目 | 端口 | 用途 |
|------|------|------|
| user-portal | 5173 | 用户端应用 |
| admin-portal | 5174 | 管理端应用 |
| backend | 8080 | 后端 API 服务 |

## 相关文档

- [后端 API 文档](../docs/backend/api.md)
- [Vue 3 文档](https://cn.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [ECharts 文档](https://echarts.apache.org/zh/index.html)
- [Pinia 文档](https://pinia.vuejs.org/zh/)

---

**版本:** 1.0.0
**最后更新:** 2024-11
