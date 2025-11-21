# 🧪 第三阶段测试计划

## 📋 测试概览

**测试目标**: 验证 Token 迁移到 HttpOnly Cookie 和 CSRF 保护功能

**测试环境**:
- 开发环境: http://localhost:5173
- 后端API: http://localhost:8080/api
- 浏览器: Chrome/Edge (推荐), Firefox, Safari

**测试周期**: 2-3 小时

---

## ✅ 前端功能测试

### 测试1: 登录流程

**目的**: 验证登录后 Cookie 设置正确

**步骤**:
1. 打开浏览器 DevTools (F12)
2. 清除所有 Cookie 和 localStorage
   ```javascript
   // 在 Console 执行
   document.cookie.split(";").forEach(c => {
     document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
   });
   localStorage.clear();
   ```
3. 访问 http://localhost:5173/login
4. 输入邮箱和验证码登录
5. 打开 DevTools → Application → Cookies

**预期结果**:
- ✅ Cookies 中存在 `auth_token`
- ✅ `auth_token` 的 HttpOnly 属性为 `true`
- ✅ `auth_token` 的 Secure 属性为 `true` (生产环境) 或 `false` (开发环境)
- ✅ `auth_token` 的 SameSite 属性为 `Strict`
- ✅ localStorage 不包含 `token` 字段
- ✅ localStorage 包含 `userInfo` (但不包含 token 和 apiKey)
- ✅ 成功跳转到 `/dashboard`

**验证命令**:
```javascript
// 在 Console 执行
console.log('localStorage token:', localStorage.getItem('token'));  // 应该是 null
console.log('localStorage userInfo:', localStorage.getItem('userInfo'));
console.log('document.cookie:', document.cookie);  // 看不到 auth_token (因为是 HttpOnly)
```

---

### 测试2: 刷新页面保持登录

**目的**: 验证刷新后通过 Cookie 维持登录状态

**步骤**:
1. 登录成功后,停留在 `/dashboard` 页面
2. 按 F5 刷新页面
3. 观察页面状态

**预期结果**:
- ✅ 刷新后仍然停留在 `/dashboard`
- ✅ 用户信息正常显示
- ✅ 不会跳转到登录页

**验证逻辑**:
- 刷新时,路由守卫调用 `checkLoginStatus()`
- 后端通过 Cookie 中的 token 验证身份
- 返回 `isLoggedIn: true`

---

### 测试3: CSRF Token 获取

**目的**: 验证应用启动时获取 CSRF Token

**步骤**:
1. 清除浏览器缓存
2. 访问 http://localhost:5173
3. 打开 DevTools → Network
4. 查找 `/api/auth/csrf-token` 请求
5. 打开 DevTools → Elements
6. 查看 `<head>` 中的 `<meta name="csrf-token">` 标签

**预期结果**:
- ✅ Network 中有 `/api/auth/csrf-token` 请求
- ✅ 响应状态码 200
- ✅ 响应数据包含 `csrfToken`
- ✅ `<head>` 中存在 `<meta name="csrf-token" content="...">`
- ✅ Console 中输出 "CSRF Token loaded successfully"

---

### 测试4: CSRF Token 在请求中携带

**目的**: 验证 POST 请求自动携带 CSRF Token

**步骤**:
1. 登录后,在任意页面执行一个 POST 请求 (如创建 API 密钥)
2. 打开 DevTools → Network
3. 找到这个 POST 请求
4. 查看 Request Headers

**预期结果**:
- ✅ Request Headers 中包含 `X-CSRF-Token: <token>`
- ✅ 请求成功,状态码 200

**验证的请求类型**:
- 创建 API 密钥: `POST /api/user/api-keys`
- 创建工单: `POST /api/tickets`
- 充值: `POST /api/recharge/create`

---

### 测试5: 登出流程

**目的**: 验证登出清除 Cookie

**步骤**:
1. 登录后,点击"退出登录"按钮
2. 打开 DevTools → Network
3. 查找 `/api/auth/logout` 请求
4. 查看 Response Headers
5. 打开 DevTools → Application → Cookies

**预期结果**:
- ✅ `/api/auth/logout` 请求成功
- ✅ Response Headers 包含 `Set-Cookie: auth_token=; Max-Age=0`
- ✅ Cookies 中 `auth_token` 被删除
- ✅ localStorage 中 `userInfo` 被删除
- ✅ 跳转到 `/login` 页面

---

### 测试6: 未登录访问受保护页面

**目的**: 验证路由守卫正常工作

**步骤**:
1. 清除所有 Cookie 和 localStorage
2. 直接访问 http://localhost:5173/dashboard
3. 观察页面行为

**预期结果**:
- ✅ 自动跳转到 `/login`
- ✅ URL 变为 http://localhost:5173/login

**测试的路由**:
- `/dashboard`
- `/api-keys`
- `/wallet`
- `/getting-started`

---

### 测试7: 已登录访问登录页

**目的**: 验证登录后无法再访问登录页

**步骤**:
1. 正常登录
2. 在地址栏输入 http://localhost:5173/login
3. 按 Enter

**预期结果**:
- ✅ 自动重定向到 `/dashboard`
- ✅ URL 变为 http://localhost:5173/dashboard

---

## 🔒 安全测试

### 安全测试1: XSS 防护 - Token 不可访问

**目的**: 验证 JavaScript 无法读取 HttpOnly Cookie

**步骤**:
1. 登录成功
2. 打开 DevTools → Console
3. 执行以下命令:

```javascript
// 尝试读取 Cookie
console.log(document.cookie);

// 尝试读取 localStorage 中的 token
console.log(localStorage.getItem('token'));

// 尝试通过其他方式访问 Cookie
const cookies = document.cookie.split(';');
const authToken = cookies.find(c => c.includes('auth_token'));
console.log('auth_token:', authToken);
```

**预期结果**:
- ✅ `document.cookie` 中**看不到** `auth_token`
- ✅ `localStorage.getItem('token')` 返回 `null`
- ✅ 无法通过任何前端方式读取 `auth_token`

**结论**: 即使攻击者注入恶意 JavaScript (XSS),也无法窃取 token

---

### 安全测试2: CSRF 防护验证

**目的**: 验证缺少 CSRF Token 的请求被拒绝

**步骤**:
1. 登录成功
2. 打开 DevTools → Console
3. 执行以下代码,模拟没有 CSRF Token 的恶意请求:

```javascript
// 删除 CSRF Token meta 标签
document.querySelector('meta[name="csrf-token"]')?.remove();

// 尝试发送 POST 请求
fetch('/api/user/api-keys', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include',
  body: JSON.stringify({ keyName: 'test' })
})
.then(res => res.json())
.then(data => console.log('Response:', data))
.catch(err => console.error('Error:', err));
```

**预期结果**:
- ✅ 请求被拒绝
- ✅ 响应状态码 `403 Forbidden`
- ✅ 响应数据: `{ "code": 403, "message": "CSRF Token验证失败" }`

---

### 安全测试3: CSRF 攻击模拟

**目的**: 模拟跨站请求伪造攻击

**步骤**:
1. 创建一个恶意 HTML 文件 `attack.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>恶意网站</title>
</head>
<body>
    <h1>点击这里获取奖励!</h1>
    <form id="maliciousForm" action="http://localhost:8080/api/user/api-keys" method="POST">
        <input type="hidden" name="keyName" value="hacked-key">
    </form>
    <script>
        // 自动提交表单
        document.getElementById('maliciousForm').submit();
    </script>
</body>
</html>
```

2. 在浏览器中打开 `attack.html`
3. 观察 Network 请求

**预期结果**:
- ✅ 请求被拒绝 (因为缺少 CSRF Token)
- ✅ 响应状态码 `403 Forbidden`
- ✅ API 密钥未被创建

**注意**: 即使 Cookie 自动携带,没有正确的 CSRF Token,请求也会被拒绝

---

### 安全测试4: SameSite Cookie 验证

**目的**: 验证 SameSite 属性防止跨站请求

**步骤**:
1. 登录成功
2. 打开 DevTools → Application → Cookies
3. 查看 `auth_token` 的属性

**预期结果**:
- ✅ SameSite 属性为 `Strict`

**说明**:
- `SameSite=Strict`: Cookie 不会在跨站请求中发送
- 提供双重保护: SameSite + CSRF Token

---

## 🔄 兼容性测试

### 测试8: 多浏览器测试

**目的**: 验证在不同浏览器中的兼容性

**测试浏览器**:
1. Chrome/Edge (Chromium 内核)
2. Firefox
3. Safari (如果可用)

**测试内容**:
- 登录流程
- 刷新保持登录
- 登出流程
- CSRF Token 获取

**预期结果**:
- ✅ 所有浏览器功能一致
- ✅ Cookie 属性正确设置

---

### 测试9: 隐身模式测试

**目的**: 验证在隐身模式下的行为

**步骤**:
1. 打开浏览器隐身窗口 (Ctrl+Shift+N)
2. 访问 http://localhost:5173
3. 执行完整登录流程
4. 关闭隐身窗口
5. 重新打开隐身窗口
6. 访问 http://localhost:5173/dashboard

**预期结果**:
- ✅ 隐身模式下可以正常登录
- ✅ 关闭窗口后 Cookie 被清除 (Session Cookie)
- ✅ 重新打开时需要重新登录

---

## 🚦 后端接口测试

### 接口测试1: 登录接口

**使用 Postman 或 curl 测试**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "123456"
  }' \
  -v
```

**验证响应头**:
```
Set-Cookie: auth_token=...; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=604800
```

**验证响应体**: 不包含 `token` 字段

---

### 接口测试2: 状态检查接口

```bash
curl http://localhost:8080/api/auth/status \
  -H "Cookie: auth_token=<从登录响应获取>" \
  -v
```

**预期响应** (已登录):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isLoggedIn": true,
    "userInfo": {
      "userId": 1,
      "email": "test@example.com",
      ...
    }
  }
}
```

---

### 接口测试3: CSRF Token 接口

```bash
curl http://localhost:8080/api/auth/csrf-token -v
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "csrfToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

### 接口测试4: 登出接口

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Cookie: auth_token=<token>" \
  -v
```

**验证响应头**:
```
Set-Cookie: auth_token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0
```

---

### 接口测试5: CSRF 保护测试

**无 CSRF Token 的请求**:
```bash
curl -X POST http://localhost:8080/api/user/api-keys \
  -H "Content-Type: application/json" \
  -H "Cookie: auth_token=<token>" \
  -d '{"keyName":"test"}' \
  -v
```

**预期结果**: 403 Forbidden

**有 CSRF Token 的请求**:
```bash
curl -X POST http://localhost:8080/api/user/api-keys \
  -H "Content-Type: application/json" \
  -H "Cookie: auth_token=<token>" \
  -H "X-CSRF-Token: <csrf-token>" \
  -d '{"keyName":"test"}' \
  -v
```

**预期结果**: 200 OK

---

## 📊 性能测试

### 性能测试1: checkLoginStatus 响应时间

**目的**: 验证首次加载时的性能

**步骤**:
1. 清除 Cookie
2. 登录
3. 刷新页面多次
4. 查看 Network 中 `/api/auth/status` 请求的响应时间

**预期结果**:
- ✅ 响应时间 < 200ms
- ✅ 不影响页面加载速度

---

### 性能测试2: 路由守卫延迟

**步骤**:
1. 登录后
2. 在不同页面间快速切换
3. 观察是否有明显延迟

**预期结果**:
- ✅ 路由切换流畅
- ✅ 守卫检查 < 100ms

---

## ✅ 验收标准

### 功能验收

- [ ] 登录成功后 Cookie 中存在 `auth_token`
- [ ] `auth_token` 的 HttpOnly 属性为 true
- [ ] localStorage 不存储 token
- [ ] 刷新页面保持登录状态
- [ ] API 请求自动携带 Cookie
- [ ] POST 请求包含 CSRF Token
- [ ] 缺少 Token 或 CSRF Token 的请求被拒绝
- [ ] 登出成功清除 Cookie
- [ ] 路由守卫正常工作

### 安全验收

- [ ] JavaScript 无法读取 HttpOnly Cookie
- [ ] CSRF 攻击被成功防御
- [ ] SameSite 属性设置正确
- [ ] Cookie 的 Secure 属性在生产环境为 true

### 性能验收

- [ ] checkLoginStatus 响应时间 < 200ms
- [ ] 路由守卫延迟 < 100ms
- [ ] 页面加载无明显延迟

### 兼容性验收

- [ ] Chrome/Edge 测试通过
- [ ] Firefox 测试通过
- [ ] Safari 测试通过 (如果可用)
- [ ] 隐身模式测试通过

---

## 🐛 常见问题排查

### 问题1: Cookie 未设置

**现象**: 登录后 Cookie 中没有 `auth_token`

**排查步骤**:
1. 检查后端是否正确设置 `Set-Cookie` 响应头
2. 检查 CORS 配置是否包含 `allowCredentials(true)`
3. 检查前端是否设置 `withCredentials: true`

---

### 问题2: 刷新后跳转到登录页

**现象**: 刷新页面后失去登录状态

**排查步骤**:
1. 检查 Cookie 是否存在
2. 检查 `/api/auth/status` 接口是否返回正确
3. 检查路由守卫中的 `checkLoginStatus` 逻辑

---

### 问题3: CSRF Token 验证失败

**现象**: 所有 POST 请求返回 403

**排查步骤**:
1. 检查 `<meta name="csrf-token">` 是否存在
2. 检查请求头是否包含 `X-CSRF-Token`
3. 检查后端 Session 中是否存储了 CSRF Token

---

### 问题4: 跨域问题

**现象**: 请求被 CORS 策略阻止

**排查步骤**:
1. 检查后端 CORS 配置
2. 确保 `allowedOrigins` 包含前端域名
3. 确保 `allowCredentials` 为 true

---

## 📝 测试报告模板

```markdown
# 第三阶段测试报告

**测试日期**: YYYY-MM-DD
**测试人员**: XXX
**测试环境**: 开发/测试/生产

## 测试结果汇总

| 测试类别 | 通过 | 失败 | 跳过 |
|---------|------|------|------|
| 功能测试 | 7/7 | 0 | 0 |
| 安全测试 | 4/4 | 0 | 0 |
| 性能测试 | 2/2 | 0 | 0 |
| 兼容性测试 | 2/2 | 0 | 0 |
| **总计** | **15/15** | **0** | **0** |

## 发现的问题

1. [问题描述]
   - 严重程度: 高/中/低
   - 复现步骤: ...
   - 预期行为: ...
   - 实际行为: ...

## 建议

- 建议1: ...
- 建议2: ...

## 结论

[ ] 通过,可以部署
[ ] 不通过,需要修复
```

---

**文档版本**: v1.0
**创建时间**: 2025-11-21
**维护者**: QA 团队
