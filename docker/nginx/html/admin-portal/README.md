# 前端静态资源占位

此目录用于存放 admin-portal 前端构建产物。

## 部署步骤

```bash
# 1. 在 frontend 目录构建
cd frontend
pnpm install
pnpm build:admin

# 2. 复制构建产物到此目录
cp -r admin-portal/dist/* ../docker/nginx/html/admin-portal/
```

## 目录结构

构建后应包含：
```
admin-portal/
├── index.html
├── assets/
│   ├── *.js
│   ├── *.css
│   └── ...
└── ...
```
