# 管理后台模块

## 📖 概述

管理后台模块提供完整的系统管理功能,包括用户管理、账户管理、订单管理、套餐管理、系统配置等核心管理功能。

## 🎯 核心功能

- 用户管理(CRUD)
- 用户余额调整
- 用户状态控制
- 后台账户管理
- API调用记录查询
- 充值订单管理
- 套餐订阅管理
- 工单管理
- 系统配置管理
- 操作日志审计
- 数据统计分析
- 批量操作支持

## 🔐 权限设计

### 角色定义

系统预定义四种管理员角色:

- **超级管理员(super_admin)**: 拥有所有权限,可以管理其他管理员
- **管理员(admin)**: 基础管理权限,可以管理用户和订单
- **运营(operator)**: 运营相关权限,侧重数据分析和内容管理
- **客服(customer_service)**: 工单处理权限,专注用户服务

### 权限列表

系统权限按模块划分:

**用户管理权限**:
- user:view - 查看用户信息
- user:edit - 编辑用户信息
- user:delete - 删除用户
- user:balance - 调整用户余额

**账户管理权限**:
- account:view - 查看API账户
- account:edit - 编辑API账户
- account:delete - 删除API账户

**订单管理权限**:
- order:view - 查看充值订单
- order:refund - 处理订单退款

**套餐管理权限**:
- plan:manage - 管理订阅套餐

**工单管理权限**:
- ticket:view - 查看工单
- ticket:handle - 处理工单

**系统配置权限**:
- system:config - 修改系统配置

**数据统计权限**:
- stats:view - 查看数据统计

权限检查通过自定义注解@RequirePermission实现。

## 📊 数据模型

### AdminUser 实体

管理员用户实体,存储后台管理员信息:
- **id**: 主键ID
- **username**: 登录用户名
- **password**: 密码(BCrypt加密)
- **realName**: 真实姓名
- **email**: 邮箱地址
- **phone**: 手机号码
- **role**: 角色(super_admin/admin/operator/customer_service)
- **permissions**: 权限列表(JSON数组格式)
- **status**: 状态(1-正常 0-禁用)
- **lastLoginAt**: 最后登录时间
- **lastLoginIp**: 最后登录IP地址
- **createdAt**: 创建时间
- **updatedAt**: 更新时间

### AdminLog 实体

管理员操作日志实体,记录所有管理操作:
- **id**: 主键ID
- **adminId**: 管理员ID
- **adminUsername**: 管理员用户名
- **action**: 操作类型(如CREATE_USER, UPDATE_BALANCE)
- **module**: 所属模块(如用户管理、订单管理)
- **targetType**: 目标类型(user/order/account等)
- **targetId**: 目标ID
- **content**: 操作内容描述
- **requestMethod**: HTTP请求方法(GET/POST/PUT/DELETE)
- **requestUrl**: 请求URL
- **requestParams**: 请求参数(JSON格式)
- **ip**: 操作IP地址
- **userAgent**: 用户代理信息
- **createdAt**: 操作时间

日志用于审计和问题追溯。

## 🔧 核心组件

### 1. AdminUserController

**管理员用户管理接口**:

- **GET /api/admin/users**: 获取管理员列表
  - 支持分页查询
  - 支持按用户名、角色筛选
  - 返回管理员基本信息(密码字段已过滤)

- **POST /api/admin/users**: 创建管理员
  - 设置用户名、密码、角色等
  - 密码使用BCrypt加密
  - 需要super_admin权限

- **PUT /api/admin/users/{id}**: 更新管理员信息
  - 可更新真实姓名、邮箱、手机、角色、权限
  - 不能修改用户名
  - 记录操作日志

- **DELETE /api/admin/users/{id}**: 删除管理员
  - 物理删除管理员记录
  - 不能删除super_admin角色
  - 需要super_admin权限

- **PUT /api/admin/users/{id}/status**: 启用/禁用管理员
  - 切换status字段
  - 禁用后无法登录

- **PUT /api/admin/users/{id}/password**: 重置管理员密码
  - 生成新密码或使用指定密码
  - 密码BCrypt加密后保存

### 2. AdminUserManagementController

**用户管理接口**,管理普通用户:

- **GET /api/admin/user-management/users**: 获取用户列表
  - 支持分页、搜索、筛选
  - 可按用户名、邮箱、注册时间筛选
  - 显示余额、状态等信息

- **GET /api/admin/user-management/users/{id}**: 获取用户详情
  - 显示完整用户信息
  - 包含余额、消费、账户等详情

- **PUT /api/admin/user-management/users/{id}**: 更新用户信息
  - 可修改邮箱、手机、昵称等
  - 记录修改日志

- **PUT /api/admin/user-management/users/{id}/status**: 启用/禁用用户
  - 禁用后用户无法登录和调用API
  - 需要user:edit权限

- **DELETE /api/admin/user-management/users/{id}**: 删除用户
  - 软删除或硬删除(可配置)
  - 需要user:delete权限
  - 谨慎操作

- **POST /api/admin/user-management/users/{id}/balance**: 调整用户余额
  - 支持充值、扣减、赠送
  - 需要user:balance权限
  - 记录余额变动日志和操作日志

- **GET /api/admin/user-management/users/{id}/accounts**: 获取用户API账户
  - 显示用户的所有API账户
  - 包含账户状态和配置

- **GET /api/admin/user-management/users/{id}/orders**: 获取用户订单
  - 显示充值订单历史
  - 支持分页查询

- **GET /api/admin/user-management/users/{id}/subscriptions**: 获取用户订阅
  - 显示套餐订阅历史
  - 包含订阅状态和有效期

### 3. AdminAccountController

**API账户管理接口**:

- **GET /api/admin/accounts**: 获取API账户列表
  - 支持分页和搜索
  - 可按用户、平台筛选
  - 显示账户状态和余额

- **GET /api/admin/accounts/{id}**: 获取账户详情
  - 显示完整账户配置
  - 包含Cookie、Token等信息

- **PUT /api/admin/accounts/{id}**: 更新账户信息
  - 可修改账户配置
  - 更新Cookie、Token等
  - 需要account:edit权限

- **PUT /api/admin/accounts/{id}/status**: 启用/禁用账户
  - 禁用后该账户不参与调度
  - 不影响其他账户

- **DELETE /api/admin/accounts/{id}**: 删除账户
  - 物理删除账户记录
  - 需要account:delete权限

- **POST /api/admin/accounts/{id}/refresh**: 刷新账户状态
  - 手动触发账户健康检查
  - 更新账户余额和状态

### 4. AdminCallRecordController

**API调用记录管理接口**:

- **GET /api/admin/call-records**: 获取调用记录列表
  - 支持分页查询
  - 可按用户、账户、平台、时间筛选
  - 显示调用详情和费用

- **GET /api/admin/call-records/{id}**: 获取调用记录详情
  - 显示完整的请求和响应
  - 包含Token使用量和费用明细

- **GET /api/admin/call-records/statistics**: 获取调用统计
  - 统计总调用次数、成功率
  - 统计Token使用量和费用
  - 按时段/用户/平台分组统计

### 5. AdminOrderController

**充值订单管理接口**:

- **GET /api/admin/orders**: 获取订单列表
  - 支持分页查询
  - 可按用户、状态、支付方式、时间筛选
  - 显示订单金额和状态

- **GET /api/admin/orders/{id}**: 获取订单详情
  - 显示完整订单信息
  - 包含支付信息和回调记录

- **PUT /api/admin/orders/{id}/status**: 更新订单状态
  - 手动标记订单状态
  - 用于异常订单处理

- **POST /api/admin/orders/{id}/refund**: 订单退款
  - 处理用户退款请求
  - 需要order:refund权限
  - 记录退款原因和操作者

### 6. AdminLogController

**操作日志查询接口**:

- **GET /api/admin/logs**: 获取操作日志
  - 支持分页查询
  - 可按管理员、操作类型、模块、时间筛选
  - 显示操作详情和IP地址

- **GET /api/admin/logs/{id}**: 获取日志详情
  - 显示完整的请求参数
  - 用于审计和问题追溯

- **GET /api/admin/logs/statistics**: 操作统计
  - 统计各管理员操作次数
  - 统计各模块操作频率
  - 识别异常操作行为

## 🎯 AOP日志记录

### @AdminLog 注解

系统使用AOP方式自动记录管理员操作日志。

**注解定义**:
- action: 操作类型(如CREATE_USER, UPDATE_BALANCE)
- module: 所属模块(如用户管理、订单管理)

**使用方式**:
在Controller方法上添加@AdminLog注解,系统会自动记录操作详情。

**记录内容**:
- 自动获取当前管理员信息
- 记录请求方法和URL
- 记录请求参数(JSON格式)
- 记录操作IP和User-Agent
- 记录操作时间

**切面实现逻辑**:
1. 通过@Around环绕通知拦截请求
2. 从注解中获取action和module
3. 从SecurityContext获取当前管理员
4. 从HttpServletRequest获取请求信息
5. 执行目标方法
6. 方法执行完成后保存日志
7. 异常情况也会记录日志

## 🔒 权限控制

### @RequirePermission 注解

系统使用自定义注解实现权限控制。

**注解定义**:
- value: 所需权限code(如"user:edit", "order:refund")

**使用方式**:
在Controller方法上添加@RequirePermission注解,系统会自动检查权限。

**权限检查流程**:
1. 拦截器获取当前管理员
2. 从注解中获取所需权限
3. 查询管理员的权限列表
4. 检查是否包含所需权限
5. super_admin角色拥有所有权限
6. 权限不足返回403错误

**拦截器实现**:
- 使用HandlerInterceptor拦截请求
- 在preHandle方法中进行权限检查
- 权限验证失败抛出异常
- 全局异常处理器返回统一错误响应

## 📊 数据统计

### 概览统计

提供平台整体数据概览:
- 用户总数和活跃用户数
- 今日新增用户数
- API调用总次数和今日调用次数
- 平台总收入和今日收入
- 账户总数和可用账户数

### 用户统计

用户相关数据统计:
- 用户增长趋势(按日/周/月)
- 用户余额分布
- 用户活跃度分析
- 用户地域分布

### 调用统计

API调用数据统计:
- 调用量趋势分析
- 各平台调用分布
- 各模型使用情况
- Token消耗统计
- 成功率和错误率

### 收入统计

财务数据统计:
- 收入趋势分析
- 充值金额分布
- 套餐销售统计
- 支付方式分析

### 账户统计

API账户数据统计:
- 账户平台分布
- 账户状态统计
- 账户余额分布
- 账户调用频率

## 🗄️ 数据库配置

### admin_users 表

管理员用户表,存储后台管理员信息。

**主要字段**:
- id: 主键ID
- username: 登录用户名(唯一)
- password: 密码(BCrypt加密)
- real_name: 真实姓名
- email: 邮箱
- phone: 手机号
- role: 角色
- permissions: 权限列表(JSON)
- status: 状态(1-正常 0-禁用)
- last_login_at: 最后登录时间
- last_login_ip: 最后登录IP
- created_at: 创建时间
- updated_at: 更新时间

**索引设计**:
- idx_username: 用户名唯一索引
- idx_email: 邮箱索引
- idx_role: 角色索引
- idx_status: 状态索引

### admin_logs 表

管理员操作日志表,记录所有管理操作。

**主要字段**:
- id: 主键ID
- admin_id: 管理员ID
- admin_username: 管理员用户名
- action: 操作类型
- module: 模块
- target_type: 目标类型
- target_id: 目标ID
- content: 操作内容
- request_method: 请求方法
- request_url: 请求URL
- request_params: 请求参数(JSON)
- ip: 操作IP
- user_agent: 用户代理
- created_at: 操作时间

**索引设计**:
- idx_admin_id: 管理员ID索引
- idx_action: 操作类型索引
- idx_module: 模块索引
- idx_created_at: 创建时间索引
- idx_admin_created: 管理员ID+创建时间联合索引
- idx_target: 目标类型+目标ID联合索引

**分区策略**:
由于日志数据量大,建议按月分区,方便归档和查询。

## 💻 前端集成

### 管理后台前端架构

管理后台前端使用Vue 3 + Element Plus构建。

**主要页面**:
- 登录页面: 管理员登录认证
- 仪表盘: 数据概览和趋势图表
- 用户管理: 用户列表、详情、编辑
- 账户管理: API账户列表和管理
- 调用记录: API调用历史查询
- 订单管理: 充值订单和退款处理
- 套餐管理: 订阅套餐配置
- 工单管理: 用户工单处理
- 系统管理: 管理员和权限配置
- 操作日志: 管理员操作审计

**路由守卫**:
- 未登录自动跳转登录页
- 检查Token有效性
- 验证页面访问权限
- 无权限显示403页面

**API调用**:
- 统一封装axios请求
- 自动添加Token到请求头
- 统一处理响应和错误
- Token过期自动刷新或跳转登录

**权限控制**:
- 根据管理员权限动态显示菜单
- 按钮级权限控制(v-permission指令)
- 无权限的功能自动隐藏或禁用

### 数据可视化

使用ECharts展示统计数据:
- 折线图: 用户增长、调用趋势、收入趋势
- 柱状图: 各平台调用分布、套餐销售统计
- 饼图: 用户余额分布、支付方式分析
- 仪表盘: 今日数据概览

## 🔍 常见问题

### Q: 如何创建第一个超级管理员?

A: 通过数据库脚本或初始化接口创建。

创建方式:
1. 执行初始化SQL脚本
2. 插入admin_users表
3. role设置为super_admin
4. password使用BCrypt加密
5. status设置为1(启用)

建议在系统部署时通过脚本自动创建,避免手动操作。

### Q: 忘记管理员密码怎么办?

A: 通过数据库或其他超级管理员重置。

重置方式:
1. 超级管理员通过后台重置密码接口
2. 直接修改数据库password字段(需BCrypt加密)
3. 联系系统管理员重置

密码重置后建议立即修改为安全密码。

### Q: 如何自定义权限?

A: 在AdminPermission枚举中添加新权限。

添加步骤:
1. 在枚举中定义新权限(code和描述)
2. 在对应Controller方法添加@RequirePermission注解
3. 给管理员分配新权限
4. 前端根据权限控制显示

权限code建议使用模块:操作的格式,如user:view。

### Q: 操作日志会一直累积吗?

A: 建议定期归档或清理历史日志。

日志管理策略:
1. 保留最近3-6个月的日志
2. 更早的日志归档到专用表或文件
3. 使用分区表提高查询性能
4. 定期备份重要日志

归档日志仍可用于审计,但不在线查询。

### Q: 如何防止管理员误操作?

A: 通过权限控制和二次确认。

防护措施:
1. 严格的权限分配,最小权限原则
2. 危险操作(如删除)需二次确认
3. 重要操作记录详细日志
4. 定期审计管理员操作
5. 禁用功能优于删除功能

对于批量操作,建议增加操作确认和预览功能。

## 🚀 性能优化

### 1. 日志表优化

操作日志表数据量大,需要优化:
- 按月分区,提高查询性能
- 索引优化,覆盖常用查询条件
- 定期归档历史数据
- 异步写入日志,不阻塞主流程

### 2. 权限缓存

管理员权限信息缓存:
- Redis缓存管理员权限列表
- 登录时加载权限到缓存
- 权限变更时清除缓存
- 缓存过期时间30分钟

### 3. 统计数据缓存

数据统计结果缓存:
- 仪表盘数据缓存5分钟
- 趋势图数据缓存15分钟
- 复杂统计结果缓存更长时间
- 支持手动刷新缓存

### 4. 批量操作优化

批量管理操作优化:
- 批量查询减少数据库访问
- 批量更新使用批处理
- 异步处理大批量操作
- 提供操作进度反馈

## 📈 监控告警

### 操作监控

监控异常管理操作:
- 批量删除用户
- 大额余额调整
- 频繁密码重置
- 异常时段登录

### 性能监控

监控后台性能指标:
- API响应时间
- 数据库查询耗时
- 缓存命中率
- 错误率和异常

### 告警通知

异常情况告警:
- 权限异常访问
- 系统错误超过阈值
- 关键操作告警
- 性能异常告警

告警方式:
- 邮件通知
- 短信通知
- 钉钉/企业微信通知

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
