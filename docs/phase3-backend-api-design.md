# 🔧 第三阶段后端API设计文档

## 📋 概览

**目标**: 将认证令牌从 localStorage 迁移到 HttpOnly Cookie,并添加 CSRF 保护

**涉及接口**:
1. `POST /api/auth/login` - 登录接口改造
2. `POST /api/auth/logout` - 新增登出接口
3. `GET /api/auth/status` - 新增登录状态检查接口
4. `GET /api/auth/csrf-token` - 新增 CSRF Token 获取接口
5. AuthInterceptor - 认证中间件改造
6. CsrfInterceptor - 新增 CSRF 验证中间件

---

## 🔐 1. 登录接口改造

### 接口信息

```
POST /api/auth/login
Content-Type: application/json
```

### 请求参数

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

### 响应���数

**改动**: 响应中**不再返回** `token` 字段

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "username": "用户名",
    "balance": 100.00
  }
}
```

### Cookie 设置

**关键改动**: 在响应头中设置 HttpOnly Cookie

```http
Set-Cookie: auth_token=<JWT_TOKEN>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=604800
```

**Cookie 属性说明**:
- `auth_token`: Cookie 名称
- `HttpOnly`: 防止 JavaScript 访问,防御 XSS 攻击
- `Secure`: 仅在 HTTPS 下传输 (生产环境必须)
- `SameSite=Strict`: 防止 CSRF 攻击
- `Path=/`: 整个站点可用
- `Max-Age=604800`: 7天过期 (秒)

### Java 实现示例

```java
@PostMapping("/login")
public Result<LoginResponse> login(
    @RequestBody LoginRequest request,
    HttpServletResponse response
) {
    // 1. 验证邮箱验证码
    User user = authService.validateAndLogin(request.getEmail(), request.getCode());

    if (user == null) {
        return Result.error("验证码错误或已过期");
    }

    // 2. 生成 JWT Token
    String token = jwtService.generateToken(user.getId());

    // 3. 设置 HttpOnly Cookie
    Cookie cookie = new Cookie("auth_token", token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);  // 生产环境必须为 true
    cookie.setPath("/");
    cookie.setMaxAge(7 * 24 * 60 * 60);  // 7天

    // 注意: Servlet API 不直接支持 SameSite,需要手动设置响应头
    response.addHeader("Set-Cookie",
        String.format("auth_token=%s; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=%d",
            token, 7 * 24 * 60 * 60)
    );

    // 4. 返回用户信息 (不包含 token)
    return Result.success(LoginResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .balance(user.getBalance())
        .build());
}
```

### Spring Boot 配置 (CORS)

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173", "https://yourdomain.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)  // ✅ 允许携带 Cookie
            .maxAge(3600);
    }
}
```

---

## 🚪 2. 登出接口

### 接口信息

```
POST /api/auth/logout
```

### 响应参数

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

### Cookie 清除

```http
Set-Cookie: auth_token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0
```

### Java 实现示例

```java
@PostMapping("/logout")
public Result<Void> logout(HttpServletResponse response) {
    // 清除 Cookie (设置 MaxAge 为 0)
    response.addHeader("Set-Cookie",
        "auth_token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0"
    );

    return Result.success("登出成功");
}
```

---

## ✅ 3. 登录状态检查接口

### 接口信息

```
GET /api/auth/status
```

### 响应参数

**场景1: 已登录**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isLoggedIn": true,
    "userInfo": {
      "userId": 1,
      "email": "user@example.com",
      "username": "用户名",
      "balance": 100.00
    }
  }
}
```

**场景2: 未登录**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isLoggedIn": false
  }
}
```

### Java 实现示例

```java
@GetMapping("/status")
public Result<LoginStatusResponse> checkStatus(HttpServletRequest request) {
    // 1. 从 Cookie 中获取 token
    String token = extractTokenFromCookie(request);

    if (token == null || !jwtService.validateToken(token)) {
        return Result.success(LoginStatusResponse.builder()
            .isLoggedIn(false)
            .build());
    }

    // 2. 获取用户信息
    Long userId = jwtService.getUserId(token);
    User user = userService.getById(userId);

    if (user == null) {
        return Result.success(LoginStatusResponse.builder()
            .isLoggedIn(false)
            .build());
    }

    // 3. 返回登录状态和用户信息
    return Result.success(LoginStatusResponse.builder()
        .isLoggedIn(true)
        .userInfo(UserInfo.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .balance(user.getBalance())
            .build())
        .build());
}

private String extractTokenFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("auth_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
    }
    return null;
}
```

---

## 🛡️ 4. CSRF Token 获取接口

### 接口信息

```
GET /api/auth/csrf-token
```

### 响应参数

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "csrfToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### Java 实现示例

```java
@GetMapping("/csrf-token")
public Result<CsrfTokenResponse> getCsrfToken(HttpSession session) {
    // 1. 生成 CSRF Token (UUID)
    String csrfToken = UUID.randomUUID().toString();

    // 2. 存储到 Session
    session.setAttribute("CSRF_TOKEN", csrfToken);

    // 3. 返回给前端
    return Result.success(CsrfTokenResponse.builder()
        .csrfToken(csrfToken)
        .build());
}
```

### 响应类定义

```java
@Data
@Builder
public class CsrfTokenResponse {
    private String csrfToken;
}
```

---

## 🔒 5. 认证中间件改造

### AuthInterceptor 改造

**改动**: 从 Cookie 读取 token,而非 Authorization header

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {

        // 1. 从 Cookie 中提取 token
        String token = extractTokenFromCookie(request);

        // 2. 验证 token
        if (token == null || !jwtService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
            return false;
        }

        // 3. 提取用户ID并设置到上下文
        Long userId = jwtService.getUserId(token);
        UserContext.set(userId);

        return true;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        // 清理上下文
        UserContext.remove();
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

### 拦截器注册

```java
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private CsrfInterceptor csrfInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // CSRF 拦截器 (优先级最高)
        registry.addInterceptor(csrfInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/send-code",
                "/api/auth/csrf-token",
                "/api/auth/status"
            );

        // 认证拦截器
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/send-code",
                "/api/auth/csrf-token",
                "/api/auth/status"
            );
    }
}
```

---

## 🛡️ 6. CSRF 验证中间件

### CsrfInterceptor 实现

```java
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {

        String method = request.getMethod();

        // 1. 只对修改数据的请求验证 CSRF Token
        if (!method.matches("POST|PUT|DELETE|PATCH")) {
            return true;
        }

        // 2. 从请求头获取 CSRF Token
        String headerToken = request.getHeader("X-CSRF-Token");

        // 3. 从 Session 获取存储的 CSRF Token
        HttpSession session = request.getSession(false);
        String sessionToken = session != null
            ? (String) session.getAttribute("CSRF_TOKEN")
            : null;

        // 4. 验证 Token
        if (headerToken == null || !headerToken.equals(sessionToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF Token验证失败\"}");
            return false;
        }

        return true;
    }
}
```

---

## 🔧 7. 辅助类和工具

### UserContext (ThreadLocal)

```java
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static void remove() {
        USER_ID.remove();
    }
}
```

### JwtService 接口

```java
public interface JwtService {
    /**
     * 生成 JWT Token
     */
    String generateToken(Long userId);

    /**
     * 验证 Token 是否有效
     */
    boolean validateToken(String token);

    /**
     * 从 Token 中提取用户ID
     */
    Long getUserId(String token);
}
```

---

## 📦 响应类定义

### LoginResponse

```java
@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String email;
    private String username;
    private BigDecimal balance;
    // ❌ 不再包含 token 字段
}
```

### LoginStatusResponse

```java
@Data
@Builder
public class LoginStatusResponse {
    private Boolean isLoggedIn;
    private UserInfo userInfo;
}

@Data
@Builder
public class UserInfo {
    private Long userId;
    private String email;
    private String username;
    private BigDecimal balance;
}
```

---

## ⚙️ 配置说明

### application.yml

```yaml
server:
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: strict
      timeout: 7d

spring:
  session:
    store-type: redis  # 推荐使用 Redis 存储 Session
    redis:
      namespace: spring:session
```

### Redis 配置 (推荐)

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: your-password
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## 🧪 测试接口

### 使用 Postman 测试

#### 1. 测试登录

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "code": "123456"
}
```

**检查响应头**: 应包含 `Set-Cookie: auth_token=...`

#### 2. 测试 CSRF Token

```
GET http://localhost:8080/api/auth/csrf-token
```

#### 3. 测试状态检查

```
GET http://localhost:8080/api/auth/status
Cookie: auth_token=<从登录响应获取>
```

#### 4. 测试登出

```
POST http://localhost:8080/api/auth/logout
Cookie: auth_token=<从登录响应获取>
```

---

## ⚠️ 注意事项

### 1. HTTPS 要求

生产环境**必须**使用 HTTPS,否则 `Secure` 标记会导致 Cookie 无法传输

### 2. 跨域配置

确保 CORS 配置中设置了 `allowCredentials(true)`

### 3. Session 存储

推荐使用 Redis 存储 Session,避免服务器重启或多实例部署时 Session 丢失

### 4. Cookie 域名

如果前后端域名不同,需要配置 Cookie 的 `Domain` 属性:

```java
cookie.setDomain(".yourdomain.com");
```

### 5. 开发环境

开发环境使用 HTTP 时,需要将 `Secure` 设置为 `false`:

```java
cookie.setSecure(false);  // 仅开发环境
```

---

## 🔗 相关资源

- [MDN - HTTP Cookies](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Cookies)
- [OWASP - CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

**文档版本**: v1.0
**创建时间**: 2025-11-21
**维护者**: 后端开发团队
