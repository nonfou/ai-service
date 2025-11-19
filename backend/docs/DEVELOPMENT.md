# 开发指南

## 🛠️ 开发环境搭建

### 1. 安装必要工具

#### Java 21
```bash
# macOS
brew install openjdk@21

# Linux
sudo apt install openjdk-21-jdk

# 验证安装
java -version
```

#### Maven
```bash
# macOS
brew install maven

# Linux
sudo apt install maven

# 验证安装
mvn -version
```

#### MySQL 8.0
```bash
# macOS
brew install mysql@8.0

# Linux
sudo apt install mysql-server-8.0

# 启动服务
sudo systemctl start mysql
```

#### Redis
```bash
# macOS
brew install redis
brew services start redis

# Linux
sudo apt install redis-server
sudo systemctl start redis

# 验证
redis-cli ping
```

### 2. 克隆项目

```bash
git clone https://github.com/your-org/ai-service.git
cd ai-service/backend
```

### 3. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/db/init_database.sql
```

### 4. 配置环境

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置
vim .env
```

### 5. 启动项目

```bash
# 方式一：使用 Maven
mvn clean spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target/ai-api-platform-1.0.0.jar
```

### 6. 访问应用

- 应用地址: http://localhost:8080
- 健康检查: http://localhost:8080/actuator/health

---

## 📁 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/nonfou/github/
│   │   │   ├── annotation/      # 自定义注解
│   │   │   ├── aspect/          # AOP 切面
│   │   │   ├── common/          # 通用类
│   │   │   ├── config/          # 配置类
│   │   │   ├── controller/      # 控制器
│   │   │   ├── dto/             # DTO
│   │   │   │   ├── request/     # 请求 DTO
│   │   │   │   └── response/    # 响应 DTO
│   │   │   ├── entity/          # 实体类
│   │   │   ├── exception/       # 异常处理
│   │   │   ├── interceptor/     # 拦截器
│   │   │   ├── mapper/          # MyBatis Mapper
│   │   │   ├── service/         # 业务逻辑
│   │   │   │   ├── impl/        # 服务实现
│   │   │   │   └── proxy/       # 代理服务
│   │   │   └── util/            # 工具类
│   │   └── resources/
│   │       ├── application.yml          # 配置文件
│   │       ├── application-dev.yml      # 开发环境
│   │       ├── application-prod.yml     # 生产环境
│   │       ├── db/init_database.sql     # 数据库脚本
│   │       └── mapper/                  # Mapper XML
│   └── test/
│       └── java/                        # 单元测试
├── docs/                # 项目文档
├── .env.example         # 环境变量模板
├── Dockerfile           # Docker 构建文件
├── pom.xml              # Maven 配置
└── README.md            # 项目说明
```

---

## 📝 代码规范

### 命名规范

#### 类名
- 使用大驼峰命名（PascalCase）
- Controller: `XxxController`
- Service: `XxxService`
- Mapper: `XxxMapper`
- Entity: 直接用名词

```java
// 好的命名
public class UserController {}
public class ApiKeyService {}
public class UserMapper {}
public class User {}

// 不好的命名
public class usercontroller {}
public class Api_Key_Service {}
```

#### 方法名
- 使用小驼峰命名（camelCase）
- 查询：`get`, `find`, `query`, `list`
- 保存：`save`, `create`, `insert`
- 更新：`update`, `modify`
- 删除：`delete`, `remove`
- 判断：`is`, `has`, `can`

```java
// 好的命名
public User getUserById(Long id) {}
public List<User> listUsers() {}
public void saveUser(User user) {}
public boolean isAdmin(User user) {}

// 不好的命名
public User get_user_by_id(Long id) {}
public List<User> GetUsers() {}
```

#### 变量名
- 使用小驼峰命名
- 常量使用全大写 + 下划线

```java
// 好的命名
private String apiKey;
private LocalDateTime createdAt;
private static final int MAX_RETRY_COUNT = 3;

// 不好的命名
private String ApiKey;
private LocalDateTime created_at;
private static final int maxRetryCount = 3;
```

### 注释规范

#### 类注释
```java
/**
 * 用户服务类
 *
 * 提供用户相关的业务逻辑，包括：
 * - 用户查询
 * - 用户创建
 * - 用户更新
 *
 * @author Your Name
 * @since 1.0.0
 */
@Service
public class UserService {
}
```

#### 方法注释
```java
/**
 * 根据 ID 获取用户
 *
 * @param userId 用户 ID
 * @return 用户信息，不存在则返回 null
 * @throws BusinessException 如果用户被禁用
 */
public User getUserById(Long userId) {
}
```

### 代码风格

#### 使用 Lombok
```java
// 推荐使用 Lombok
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
}

// 避免手写 getter/setter
```

#### 异常处理
```java
// 好的做法：明确的异常处理
public User getUserById(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    if (user.getStatus() == 0) {
        throw new BusinessException("用户已被禁用");
    }
    return user;
}

// 不好的做法：吞掉异常
public User getUserById(Long userId) {
    try {
        return userMapper.selectById(userId);
    } catch (Exception e) {
        return null;  // 不要这样做
    }
}
```

#### Stream API
```java
// 推荐使用 Stream API
List<String> emails = users.stream()
    .filter(user -> user.getStatus() == 1)
    .map(User::getEmail)
    .collect(Collectors.toList());

// 避免传统循环（除非性能敏感）
```

---

## 🧪 测试

### 单元测试

```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testGetUserById() {
        // Given
        Long userId = 1L;

        // When
        User user = userService.getUserById(userId);

        // Then
        assertNotNull(user);
        assertEquals(userId, user.getId());
    }

    @Test(expected = BusinessException.class)
    public void testGetUserById_NotFound() {
        // Given
        Long userId = 999L;

        // When
        userService.getUserById(userId);

        // Then - 应该抛出异常
    }
}
```

### 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSendCode() throws Exception {
        mockMvc.perform(post("/api/auth/send-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

### Mock 测试

```java
@RunWith(MockitoJUnitRunner.class)
public class AccountSchedulerServiceTest {

    @Mock
    private BackendAccountService backendAccountService;

    @InjectMocks
    private AccountSchedulerService schedulerService;

    @Test
    public void testSelectAccount() {
        // Given
        List<BackendAccount> accounts = Arrays.asList(
            createTestAccount(1L),
            createTestAccount(2L)
        );
        when(backendAccountService.getActiveAccounts())
            .thenReturn(accounts);

        // When
        BackendAccount selected = schedulerService.selectAccount("gpt-4o", null, null);

        // Then
        assertNotNull(selected);
        verify(backendAccountService, times(1)).getActiveAccounts();
    }
}
```

---

## 🔨 常用开发任务

### 添加新的 API 接口

1. **创建 DTO**

```java
// 请求 DTO
@Data
public class CreateOrderRequest {
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    private String payMethod;
}

// 响应 DTO
@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private Integer status;
}
```

2. **创建 Controller**

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return Result.success(response);
    }
}
```

3. **创建 Service**

```java
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 业务逻辑
        Order order = new Order();
        order.setAmount(request.getAmount());
        order.setPayMethod(request.getPayMethod());
        orderMapper.insert(order);

        return OrderResponse.builder()
            .orderId(order.getId())
            .orderNo(order.getOrderNo())
            .amount(order.getAmount())
            .status(order.getStatus())
            .build();
    }
}
```

### 添加新的数据表

1. **编写 SQL**

```sql
CREATE TABLE IF NOT EXISTS new_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

2. **创建 Entity**

```java
@Data
@TableName("new_table")
public class NewEntity {
    private Long id;
    private String name;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

3. **创建 Mapper**

```java
@Mapper
public interface NewEntityMapper extends BaseMapper<NewEntity> {
    // 自定义查询方法
}
```

---

## 🐛 调试技巧

### 1. 日志调试

```java
@Slf4j
@Service
public class UserService {

    public User getUserById(Long userId) {
        log.debug("查询用户, userId={}", userId);

        User user = userMapper.selectById(userId);

        if (user == null) {
            log.warn("用户不存在, userId={}", userId);
            throw new BusinessException("用户不存在");
        }

        log.info("查询用户成功, userId={}, email={}", userId, user.getEmail());
        return user;
    }
}
```

### 2. 断点调试

在 IntelliJ IDEA 中：
- 设置断点：点击行号左侧
- 调试运行：点击 Debug 按钮
- 单步执行：F8
- 进入方法：F7
- 查看变量：Variables 面板

### 3. SQL 调试

```yaml
# application-dev.yml
logging:
  level:
    com.nonfou.github.mapper: DEBUG

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

## 📚 推荐资源

### 官方文档
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Redis 文档](https://redis.io/documentation)

### 学习资源
- 《Effective Java》
- 《Clean Code》
- 《重构：改善既有代码的设计》

### 开发工具
- IntelliJ IDEA（推荐）
- VS Code
- Postman（API 测试）
- Navicat/DBeaver（数据库管理）
- Redis Desktop Manager（Redis 管理）

---

## 🤝 贡献指南

### 提交代码

1. Fork 项目
2. 创建特性分支：`git checkout -b feature/xxx`
3. 提交代码：`git commit -am 'Add xxx feature'`
4. 推送分支：`git push origin feature/xxx`
5. 提交 Pull Request

### Commit 规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 类型**:
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat(user): 添加用户导出功能

实现用户数据导出为 Excel 文件的功能，支持按条件筛选导出。

Closes #123
```

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
