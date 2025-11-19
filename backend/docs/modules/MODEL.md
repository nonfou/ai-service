# AI模型管理模块

## 📖 概述

AI模型管理模块负责管理系统支持的AI模型,包括模型配置、定价、分组、排序等功能,为用户提供统一的模型选择和使用体验。

## 🎯 核心功能

- 模型信息管理(CRUD)
- 模型定价配置
- 模型分组和分类
- 模型排序和推荐
- 模型能力标识
- 模型可用性控制
- 模型统计分析
- 模型使用限制

## 🤖 模型设计

### 模型分类

系统支持以下几类AI模型:

1. **文本生成模型**
   - GPT-4 Turbo
   - GPT-4
   - GPT-3.5 Turbo
   - Claude 3.5 Sonnet
   - Claude 3 Opus/Sonnet/Haiku

2. **代码生成模型**
   - Codex
   - Code Llama
   - StarCoder

3. **多模态模型**
   - GPT-4 Vision
   - Claude 3 Opus (支持图片)
   - Gemini Pro Vision

4. **嵌入模型**
   - text-embedding-ada-002
   - text-embedding-3-small/large

### 模型能力标识

**ModelCapability 枚举**,定义模型支持的能力:
- **TEXT_GENERATION**: 文本生成
- **CODE_GENERATION**: 代码生成
- **VISION**: 视觉理解
- **FUNCTION_CALLING**: 函数调用
- **JSON_MODE**: JSON模式
- **STREAMING**: 流式输出
- **PROMPT_CACHING**: 提示词缓存

## 📊 数据模型

### AiModel 实体

**AI模型表**,存储所有模型配置信息:

**基本信息**:
- **id**: 主键ID
- **modelId**: 模型ID (如 gpt-4-turbo)
- **modelName**: 显示名称
- **provider**: 提供商 (openai/anthropic/google)
- **category**: 分类 (text/code/vision/embedding)

**描述信息**:
- **description**: 模型描述
- **capabilities**: 能力标识 (JSON数组)

**上下文和输出限制**:
- **maxContextTokens**: 最大上下文token数
- **maxOutputTokens**: 最大输出token数

**定价信息** (每百万token的美元价格):
- **inputPricePer1M**: 输入价格
- **outputPricePer1M**: 输出价格
- **cachedInputPricePer1M**: 缓存输入价格

**倍率配置**:
- **priceMultiplier**: 价格倍率

**显示和排序**:
- **groupName**: 分组名称
- **sortOrder**: 排序
- **badge**: 徽章 (new/popular/recommended)
- **colorTheme**: 颜色主题

**状态和配置**:
- **status**: 状态 (1-启用 0-禁用)
- **isDefault**: 是否默认模型
- **isVisible**: 是否前端可见
- **createdAt**: 创建时间
- **updatedAt**: 更新时间

### ModelUsageLimit 实体

**模型使用限制表**,配置模型的访问限制:

**基本信息**:
- **id**: 主键ID
- **modelId**: 模型ID
- **limitType**: 限制类型 (global/user/account)
- **targetId**: 目标ID (用户ID或账户ID)

**限制配置**:
- **maxRequestsPerMinute**: 每分钟最大请求数
- **maxRequestsPerHour**: 每小时最大请求数
- **maxRequestsPerDay**: 每天最大请求数
- **maxTokensPerRequest**: 单次请求最大token数
- **maxTokensPerDay**: 每天最大token数

## 🔧 核心组件

### 1. ModelController

**用户接口**,提供模型查询功能:

**位置**: `src/main/java/com/aiservice/controller/ModelController.java`

**核心接口**:

#### GET /api/models
获取可用模型列表:
- 支持按分类筛选 (category)
- 支持按提供商筛选 (provider)
- 只返回启用且可见的模型
- 按排序值和创建时间排序
- 返回: 模型列表 (ModelVO)

#### GET /api/models/{modelId}
获取模型详情:
- 查询指定模型ID的详细信息
- 包含定价、能力、限制等完整信息
- 返回: 模型详情 (ModelDetailVO)

#### GET /api/models/groups
获取模型分组:
- 按 groupName 分组返回模型
- 每个分组包含多个模型
- 返回: 分组列表 (ModelGroupVO)

#### GET /api/models/recommended
获取推荐模型:
- 返回带有 badge 标识的推荐模型
- 用于首页或快速选择
- 返回: 推荐模型列表

#### GET /api/models/{modelId}/availability
检查模型可用性:
- 检查用户是否可以使用该模型
- 验证使用限制
- 返回: 可用性信息 (ModelAvailabilityVO)

#### GET /api/models/{modelId}/stats
获取模型使用统计:
- 查询当前用户对该模型的使用情况
- 包含调用次数、token消耗等
- 返回: 统计信息 (ModelStatsVO)

### 2. AdminModelController

**管理员接口**,提供模型管理功能:

**位置**: `src/main/java/com/aiservice/controller/AdminModelController.java`

**核心接口**:

#### GET /api/admin/models
获取所有模型 (包括禁用):
- 支持分页查询
- 支持多条件筛选 (provider/category/status)
- 返回所有模型 (包括不可见的)
- 需要 `model:view` 权限

#### POST /api/admin/models
创建模型:
- 接收 CreateModelRequest
- 验证 modelId 唯一性
- 设置默认状态和可��性
- 需要 `model:edit` 权限
- 记录操作日志

#### PUT /api/admin/models/{id}
更新模型:
- 接收 UpdateModelRequest
- 更新指定模型的信息
- 需要 `model:edit` 权限
- 记录操作日志

#### DELETE /api/admin/models/{id}
删除模型:
- 物理删除模型记录
- 谨慎操作,建议使用禁用代替
- 需要 `model:edit` 权限
- 记录操作日志

#### PUT /api/admin/models/{id}/status
更新模型状态:
- 切换 status 字段 (1/0)
- 禁用后用户不可见
- 需要 `model:edit` 权限
- 记录操作日志

#### PUT /api/admin/models/{id}/sort
更新模型排序:
- 修改 sortOrder 值
- 控制前端展示顺序
- 数字越小越靠前
- 需要 `model:edit` 权限
- 记录操作日志

#### PUT /api/admin/models/batch-multiplier
批量更新模型倍率:
- 接收模型ID列表和倍率值
- 批量更新多个模型的价格倍率
- 需要 `model:edit` 权限
- 记录操作日志

#### POST /api/admin/models/sync-pricing
同步模型定价:
- 从官方API获取最新定价
- 更新数据库中的价格信息
- 需要 `model:edit` 权限
- 返回同步数量
- 记录操作日志

#### GET /api/admin/models/{modelId}/usage-stats
获取模型使用统计:
- 查询指定模型的使用情况
- 支持日期范围筛选
- 返回详细的统���数据
- 需要 `model:view` 权限

### 3. ModelService

**模型管理服务**,核心业务逻辑:

**位置**: `src/main/java/com/aiservice/service/ModelService.java`

**核心方法**:

#### getAvailableModels()
获取可用模型列表:
1. 构建缓存键 (基于 category 和 provider)
2. 先从 Redis 缓存获取
3. 缓存未命中时查询数据库
4. 筛选条件: status=1 AND is_visible=true
5. 支持按分类和提供商筛选
6. 按 sort_order 和 created_at 排序
7. 转换为 VO 对象
8. 缓存30分钟

#### getModelDetail()
获取模型详情:
1. 构建缓存键 (model:{modelId})
2. 先从 Redis 缓存获取
3. 缓存未命中时查询数据库
4. 验证模型状态 (必须启用)
5. 转换为 ModelDetailVO
6. 计算实际价格 (应用倍率)
7. 缓存1小时

#### getModelGroups()
获取模型分组:
1. 查询所有启用且可见的模型
2. 按 groupName 分组
3. 每组模型按 sortOrder 排序
4. 转换为 ModelGroupVO
5. 返回分组列表

#### createModel()
创建模型:
1. 检查 modelId 是否已存在
2. 复制请求数据到实体
3. 设置默认状态 (status=1, isVisible=true)
4. 插入数据库
5. 清除模型列表缓存

#### updateModel()
更新模型:
1. 查询模型是否存在
2. 复制请求数据到实体
3. 更新数据库
4. 清除模型缓存 (列表和详情)

#### batchUpdateMultiplier()
批量更新倍率:
1. 循环处理每个 modelId
2. 查询模型记录
3. 更新 priceMultiplier 字段
4. 保存到数据库
5. 清除所有模型缓存

**内部方法**:
- **clearModelCache()**: 清除所有模型列表缓存
- **convertToVO()**: 转换实体为 VO,解析 capabilities JSON
- **convertToDetailVO()**: 转换为详情 VO,计算实际价格

## 🗄️ 数据库配置

### ai_models 表

**表结构说明**:

**字段定义**:
- id: 主键ID,自增
- model_id: 模型ID,唯一索引
- model_name: 显示名称
- provider: 提供商
- category: 分类
- description: 模型描述
- capabilities: 能力标识 (JSON格式)
- max_context_tokens: 最大上下文token数
- max_output_tokens: 最大输出token数
- input_price_per_1m: 输入价格 (每百万token/美元)
- output_price_per_1m: 输出价格 (每百万token/美元)
- cached_input_price_per_1m: 缓存输入价格
- price_multiplier: 价格倍率,默认 1.00
- group_name: 分组名称
- sort_order: 排序,默认 0
- badge: 徽章
- color_theme: 颜色主题
- status: 状态 (1-启用 0-禁用),默认 1
- is_default: 是否默认模型,默认 FALSE
- is_visible: 是否前端可见,默认 TRUE
- created_at: 创建时间
- updated_at: 更新时间

**索引设计**:
- PRIMARY KEY (id)
- UNIQUE INDEX idx_model_id (model_id)
- INDEX idx_provider (provider)
- INDEX idx_category (category)
- INDEX idx_status (status)
- INDEX idx_sort_order (sort_order)
- INDEX idx_provider_category (provider, category)

### 初始化模型数据

**预置模型示例**:

**GPT系列**:
1. **gpt-4-turbo**:
   - 名称: GPT-4 Turbo
   - 提供商: openai
   - 分类: text
   - 描述: 最新的GPT-4模型,性能强大,支持128K上下文
   - 能力: 文本生成、函数调用、JSON模式、流式
   - 上下文: 128000 tokens
   - 输出: 4096 tokens
   - 价格: $10.00/$30.00 (输入/输出)
   - 徽章: recommended
   - 排序: 1

2. **gpt-4**:
   - 名称: GPT-4
   - 提供商: openai
   - 分类: text
   - 上下文: 8192 tokens
   - 价格: $30.00/$60.00
   - 排序: 2

3. **gpt-3.5-turbo**:
   - 名称: GPT-3.5 Turbo
   - 提供商: openai
   - 分类: text
   - 上下文: 16385 tokens
   - 价格: $0.50/$1.50
   - 徽章: popular
   - 排序: 3

**Claude系列**:
1. **claude-3-5-sonnet-20241022**:
   - 名称: Claude 3.5 Sonnet
   - 提供商: anthropic
   - 分类: text
   - 描述: Anthropic最新模型,性能卓越,支持200K上下文
   - 能力: 文本生成、视觉、提示词缓存、流式
   - 上下文: 200000 tokens
   - 输出: 8192 tokens
   - 价格: $3.00/$15.00 (输入/输出)
   - 缓存价格: $0.30
   - 徽章: new
   - 排序: 4

2. **claude-3-opus-20240229**:
   - 名称: Claude 3 Opus
   - 提供商: anthropic
   - 分类: text
   - 上下文: 200000 tokens
   - 价格: $15.00/$75.00
   - 排序: 5

## 💻 前端集成

### Vue 3 模型选择组件

**组件功能**:
- 模型分类标签页切换
- 网格布局展示模型卡片
- 徽章显示 (新/热门/推荐)
- 能力标签展示
- 定价信息显示
- 模型选择和高亮
- 响应式设计

**组件结构**:

**顶部标签页**:
- 全部
- 文本生成
- 代码生成
- 多模态

**模型卡片**:
- 徽章 (右上角)
- 模型名称和提供商
- 模型描述
- 能力标签列表
- 上下文长度信息
- 定价信息 (输入/输出)
- 选中标识 (右下角勾选)

**交互逻辑**:
- 加载模型列表 (GET /api/models)
- 默认选择推荐模型
- 分类切换过滤模型
- 点击卡片选择模型
- 触发 model-selected 事件

**辅助方法**:
- formatTokens(): 格式化token数 (K/M单位)
- formatPrice(): 格式化价格 (保留2位小数)
- getBadgeText(): 获取徽章中文文本
- getCapabilityText(): 获取能力中文文本

**样式设计**:
- 网格布局,自适应列数
- 卡片悬停效果
- 选中状态高亮
- 徽章颜色区分 (新-绿/热门-橙/推荐-蓝)
- 分隔线区分价格区域

## 🚀 性能优化

### 1. 模型列表缓存

**策略**:
- 使用 Redis 缓存模型列表
- 缓存键包含筛选条件 (category + provider)
- 缓存时间: 30分钟
- 模型更新时清除缓存

**配置**:
- RedisCacheConfiguration 设置
- 序列化: GenericJackson2JsonRedisSerializer
- 过期时间: 30分钟

**优势**:
- 减少数据库查询
- 提高响应速度
- 降低数据库负载

### 2. 定价计算优化

**策略**:
- 使用 @Cacheable 缓存价格计算结果
- 缓存键: model:price:{modelId}
- 从缓存的模型信息中获取定价
- 避免重复计算

**优势**:
- 快速返回价格
- 减少计算开销
- 提高API响应速度

### 3. 数据库索引优化

**复合索引**:
- idx_provider_category_status (provider, category, status)
- 优化多条件查询

**覆盖索引**:
- idx_list_query (status, is_visible, sort_order, model_id, model_name, provider)
- 查询无需回表,直接从索引获取数据

**优势**:
- 大幅提升查询速度
- 减少磁盘I/O
- 提高并发能力

## 🔍 常见问题

### Q: 如何添加新模型?

A: 通过管理后台添加:

**步骤**:
1. 登录管理后台
2. 进入"模型管理"页面
3. 点击"添加模型"
4. 填写模型信息:
   - 模型ID: 如 `gpt-4-turbo`
   - 显示名称
   - 提供商
   - 分类
   - 上下文长度
   - 定价信息
5. 保存

### Q: 如何调整模型定价?

A: 有两种方式:

**方式1: 调整倍率**
- 更新 price_multiplier 字段
- 影响所有该提供商的模型
- 例如: 将 openai 的模型统一调整为 1.5 倍

**方式2: 更新基础定价**
- 使用管理后台的"同步定价"功能
- 从官方API获取最新价格
- 更新 input_price_per_1m 和 output_price_per_1m

### Q: 如何隐藏某个模型?

A: 更新模型状态:

**方式1: 禁用模型**
- 设置 status = 0
- 模型完全不可用

**方式2: 隐藏显示**
- 设置 is_visible = FALSE
- 模型可用但不在列表中显示

### Q: 如何设置默认模型?

A: 更新默认模型标识:

**步骤**:
1. 先取消其他模型的默认标识 (is_default = FALSE)
2. 设置新的默认模型 (is_default = TRUE)
3. 前端会自动选择默认模型

### Q: 如何监控模型使用情况?

A: 使用统计查询:

**查询维度**:
- 按模型分组
- 统计调用次数
- 累计token消耗
- 平均成本
- 总成本

**时间范围**:
- 最近7天
- 最近30天
- 自定义日期范围

**数据来源**:
- api_call_records 表
- 关联 ai_models 表

## 📊 统计分析

### 模型使用排行

**模型使用次数排行**:
- 按模型分组统计
- 关联模型名称和提供商
- 统计使用次数
- 累计token消耗
- 累计收入
- 最近30天数据
- 按使用次数降序
- 取前10名

**应用场景**:
- 识别热门模型
- 优化模型配置
- 制定定价策略

**模型收入贡献**:
- 按模型分组统计
- 计算总收入
- 计算收入占比
- 最近30天数据
- 按收入降序

**应用场景**:
- 评估模型价值
- 优化模型组合
- 制定推广策略

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
