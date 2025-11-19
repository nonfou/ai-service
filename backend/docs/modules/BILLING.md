# 计费系统模块

## 📖 概述

计费系统是平台的核心财务模块,负责 API 调用的成本计算、余额管理和费用结算。系统支持精确到 Token 级别的计费,通过三层倍率机制实现灵活的定价策略。

## 🎯 核心功能

- **精确计费**: Token 级别的精确成本计算
- **多层倍率**: 模型倍率 × 账户倍率 × 平台加成
- **实时扣费**: API 调用完成后立即扣除余额
- **余额管理**: 充值、消费、调整、退款
- **变动记录**: 完整的余额变动日志
- **成本统计**: 多维度的成本分析
- **价格配置**: 灵活的价格管理
- **汇率支持**: 支持多币种计费

## 💰 计费模型

### Token 类型

**OpenAI/Copilot 模型**:

1. **Prompt tokens** (输入 Token)
   - 用户消息
   - 系统提示词
   - 对话历史

2. **Completion tokens** (输出 Token)
   - AI 生成的回复内容

3. **Cached tokens** (缓存读取 Token)
   - 从缓存读取的输入内容
   - 价格通常为普通输入的 50%

4. **Cache write tokens** (缓存写入 Token)
   - 写入缓存的内容
   - 价格通常为普通输入的 125%

### 价格配置

#### 模型价格表

**ModelPrice 实体**,存储模型价格信息:
- **modelName**: 模型名称
- **inputPrice**: 输入价格 (USD/1M tokens)
- **outputPrice**: 输出价格 (USD/1M tokens)
- **cacheReadPrice**: 缓存读取价格
- **cacheWritePrice**: 缓存写入价格
- **currency**: 币种 (USD)
- **exchangeRate**: 汇率 (CNY/USD)
- **effectiveDate**: 生效日期

#### 价格示例

**GPT-4o** (2024-05-13):
- 模型名称: gpt-4o
- 输入价格: $5.00/1M tokens
- 输出价格: $15.00/1M tokens
- 缓存读取: $2.50/1M tokens
- 缓存写入: $6.25/1M tokens
- 汇率: 7.2 CNY/USD
- 生效日期: 2024-05-13

**GPT-4o-mini**:
- 模型名称: gpt-4o-mini
- 输入价格: $0.15/1M tokens
- 输出价格: $0.60/1M tokens
- 缓存读取: $0.075/1M tokens
- 缓存写入: $0.1875/1M tokens
- 汇率: 7.2 CNY/USD
- 生效日期: 2024-07-18

**Claude 3.5 Sonnet**:
- 模型名称: claude-3-5-sonnet-20241022
- 输入价格: $3.00/1M tokens
- 输出价格: $15.00/1M tokens
- 缓存读取: $0.30/1M tokens
- 缓存写入: $3.75/1M tokens
- 汇率: 7.2 CNY/USD
- 生效日期: 2024-10-22

### 三层倍率机制

#### 1. 模型倍率

每个模型配置独立的倍率,用于调整该模型的定价:

**Model 实体字段**:
- **modelName**: 模型名称
- **displayName**: 显示名称
- **priceMultiplier**: 模型倍率,默认 1.0
- **status**: 状态 (0-禁用, 1-启用)

**使用场景**:
- 热门模型可以设置更高倍率 (如 1.5)
- 促销模型可以设置更低倍率 (如 0.8)

#### 2. 账户倍率

每个后端账户有独立的成本倍率:

**BackendAccount 实体字段**:
- **accountName**: 账户名称
- **platform**: 平台
- **costMultiplier**: 账户成本倍率

**使用场景**:
- 不同渠道账户的成本不同
- 企业账户可能有优惠价格
- 个人账户成本可能更高

#### 3. 平台加成

系统级别的统一加成,用于平台利润:

**配置项**:
- **platformMarkup**: 平台加成,默认 1.2 (20% 利润)
- **minCharge**: 最小扣费,默认 0.001 元

### 计费公式

#### 基础成本计算

```
基础成本 (USD) = (
  输入Token × 输入单价 +
  输出Token × 输出单价 +
  缓存读Token × 缓存读单价 +
  缓存写Token × 缓存写单价
) / 1,000,000
```

#### 最终费用计算

```
最终费用 (CNY) = 基础成本 × 模型倍率 × 账户倍率 × 平台加成 × 汇率

取 max(最终费用, 最小扣费)
```

#### 计算示例

**场景**: 使用 GPT-4o 模型
- 输入: 1,000 tokens
- 输出: 500 tokens
- 缓存读取: 200 tokens
- 缓存写入: 0 tokens
- 模型倍率: 1.0
- 账户倍率: 1.0
- 平台加成: 1.2
- 汇率: 7.2 CNY/USD

**计算过程**:
```
基础成本 = (1000 × 5.00 + 500 × 15.00 + 200 × 2.50 + 0 × 6.25) / 1,000,000
         = (5,000 + 7,500 + 500 + 0) / 1,000,000
         = 13,000 / 1,000,000
         = 0.013 USD

最终费用 = 0.013 × 1.0 × 1.0 × 1.2 × 7.2
         = 0.11232 CNY
         ≈ 0.11 元
```

## 🔧 核心组件

### 1. CostCalculatorService

**职责**: 成本计算,价格查询

**位置**: `src/main/java/com/aiservice/service/CostCalculatorService.java`

**核心方法**:

#### calculateCost()
计算 API 调用成本:
1. 获取模型价格信息
2. 计算基础成本 (USD)
3. 获取模型倍率
4. 应用倍率和汇率转换
5. 确保不低于最小扣费
6. 保留4位小数

参数:
- modelName: 模型名称
- inputTokens: 输入 Token 数
- outputTokens: 输出 Token 数
- cacheReadTokens: 缓存读取 Token 数
- cacheWriteTokens: 缓存写入 Token 数

返回: 最终费用 (CNY)

#### calculateCostWithAccount()
计算带账户倍率的成本:
1. 调用 calculateCost() 计算基础费用
2. 查询后端账户信息
3. 应用账户成本倍率
4. 返回最终费用

参数:
- accountId: 后端账户ID
- modelName: 模型名称
- inputTokens: 输入 Token 数
- outputTokens: 输出 Token 数
- cacheReadTokens: 缓存读取 Token 数
- cacheWriteTokens: 缓存写入 Token 数

返回: 最终费用 (CNY)

#### estimateCost()
估算成本 (用于预检查):
- 计算基础费用
- 预留 20% 缓冲
- 用于API调用前的余额检查

#### getCostBreakdown()
获取价格明细:
- 返回详细的成本分解信息
- 包含各类型Token的独立成本
- 展示各层倍率的应用
- 用于成本透明化展示

**内部方法**:

- **calculateBaseCost()**: 计算基础成本 (USD),分别计算输入、输出、缓存读、缓存写的成本并求和
- **getModelPrice()**: 获取模型价格,查询生效中的最新价格

### 2. BalanceService

**职责**: 余额查询、扣除、充值

**位置**: `src/main/java/com/aiservice/service/BalanceService.java`

**核心方法**:

#### getBalance()
获取用户余额:
- 查询用户信息
- 返回可用余额
- 余额为空时返回0

#### deduct()
扣除余额:
1. 使用 Redis 分布式锁避免并发
2. 查询当前余额
3. 检查余额是否充足
4. 扣除余额并更新
5. 记录余额变动日志
6. 清除余额缓存
7. 释放分布式锁

异常处理:
- 金额必须大于0
- 余额不足抛出 InsufficientBalanceException
- 操作频繁提示稍后重试

#### recharge()
充值余额:
1. 使用分布式锁
2. 查询当前余额
3. 增加余额并更新
4. 记录充值日志
5. 清除余额缓存
6. 释放锁

参数:
- userId: 用户ID
- amount: 充值金额
- relatedId: 关联订单ID
- remark: 备注说明

#### adjust()
调整余额 (管理员操作):
- 支持正负金额调整
- 调整后余额不能为负数
- 记录操作者信息
- 日志备注标记为管理员调整

#### refund()
退款:
- 调用 recharge() 增加余额
- 更新日志类型为 refund
- 备注标记为退款

**内部方法**:
- **clearBalanceCache()**: 清除用户余额缓存,确保数据一致性

### 3. BalanceLogService

**职责**: 余额变动日志查询和统计

**位置**: `src/main/java/com/aiservice/service/BalanceLogService.java`

**核心方法**:

#### getUserBalanceLogs()
查询用户余额变动记录:
- 支持按操作类型筛选 (recharge/consume/adjust/refund)
- 支持按日期范围筛选
- 分页查询
- 按创建时间倒序
- 转换为 VO 返回

参数:
- userId: 用户ID
- type: 操作类型 (可选)
- startDate: 开始日期 (可选)
- endDate: 结束日期 (可选)
- page: 页码
- pageSize: 每页数量

返回: 分页的余额变动记录列表

#### getBalanceStatistics()
统计用户余额变动:
- 总充值金额
- 总消费金额
- 总退款金额
- 总调整金额
- 按指定时间范围统计

**内部方法**:
- **convertToVO()**: 转换实体为 VO,添加类型中文名称
- **getTypeName()**: 获取操作类型的中文名称

## 📊 数据模型

### BalanceLog 实体

**余额变动记录**,记录所有余额操作:
- **id**: 主键ID
- **userId**: 用户ID
- **amount**: 变动金额 (正数=增加,负数=减少)
- **balanceAfter**: 变动后余额
- **type**: 类型 (recharge/consume/adjust/refund)
- **relatedId**: 关联ID (订单ID/API调用ID等)
- **remark**: 备注
- **createdAt**: 创建时间

### 余额变动类型

| 类型 | 说明 | 金额符号 | 示例场景 |
|------|------|----------|----------|
| recharge | 充值 | 正数 | 在线支付充值 |
| consume | 消费 | 负数 | API 调用扣费 |
| adjust | 调整 | 正/负 | 管理员手动调整 |
| refund | 退款 | 正数 | 订单退款 |

## 🔍 使用示例

### 1. 计算 API 调用成本

通过 CostCalculatorService 计算成本:
- 注入 CostCalculatorService
- 调用 calculateCost() 方法
- 传入模型名称和各类型 Token 数量
- 返回最终费用 (人民币)

示例场景:
- 模型: gpt-4o
- 输入: 1000 tokens
- 输出: 500 tokens
- 缓存读取: 200 tokens
- 缓存写入: 0 tokens

### 2. 扣除用户余额

通过 BalanceService 扣除余额:
- 注入 BalanceService
- 调用 deduct() 方法
- 传入用户ID、金额、关联ID、备注
- 捕获 InsufficientBalanceException 处理余额不足

### 3. 用户充值

通过 BalanceService 充值:
- 调用 recharge() 方法
- 传入用户ID、金额、订单ID、备注
- 自动记录充值日志

### 4. 查询余额变动记录

通过 BalanceLogService 查询日志:
- 注入 BalanceLogService
- 调用 getUserBalanceLogs() 方法
- 支持类型筛选和日期范围
- 分页返回结果

示例: 查询最近30天的消费记录
- 类型: consume
- 日期: 最近30天
- 分页: 每页20条

### 5. 获取成本明细

通过 CostCalculatorService 获取详细分解:
- 调用 getCostBreakdown() 方法
- 返回 CostBreakdown 对象
- 包含各部分成本明细
- 展示倍率和汇率应用

明细包含:
- 输入Token成本
- 输出Token成本
- 缓存读取成本
- 缓存写入成本
- 基础成本
- 模型倍率
- 平台加成
- 汇率
- 最终费用

## 🔧 配置说明

### application.yml 配置

**计费配置项**:
- **platform-markup**: 平台加成 (1.2 = 20% 利润)
- **min-charge**: 最小扣费 (元)
- **default-exchange-rate**: 默认汇率 (CNY/USD)
- **balance-warning-threshold**: 余额预警阈值 (元)
- **auto-recharge**: 自动充值配置
  - enabled: 是否启用
  - threshold: 触发阈值
  - amount: 充值金额

## 📈 统计分析

### 用户消费统计

**统计维度**:
- 按用户ID分组
- 统计最近30天消费记录
- 计算总消费金额
- 计算消费次数
- 计算平均消费金额
- 按总消费降序排列

**应用场景**:
- 识别高价值用户
- 分析用户消费行为
- 制定营销策略

### 每日收入统计

**统计维度**:
- 按日期分组
- 统计每日充值记录
- 计算每日充值总金额
- 计算每日充值次数
- 最近30天趋势分析

**应用场景**:
- 了解平台收入趋势
- 评估运营效果
- 预测未来收入

### 模型成本统计

**统计维度**:
- 按模型分组
- 统计最近7天调用记录
- 计算调用次数
- 统计总输入输出Token数
- 计算总成本
- 按总成本降序排列

**应用场景**:
- 分析各模型使用情况
- 优化模型定价策略
- 控制成本支出

## 🔍 常见问题

### Q: 为什么要使用三层倍率?

A: 三层倍率提供了灵活的定价策略:
- **模型倍率**: 针对不同模型差异化定价
- **账户倍率**: 反映不同渠道的成本差异
- **平台加成**: 统一的平台利润率

这种设计便于:
- 动态调整价格策略
- 针对不同用户群体定价
- 快速响应市场变化

### Q: 如何防止余额并发扣除问题?

A: 系统使用 Redis 分布式锁:

**分布式锁机制**:
- 锁键格式: "balance:lock:" + userId
- 锁过期时间: 5秒
- 使用 setIfAbsent 原子操作
- finally 块确保锁释放

确保同一用户的余额操作串行执行,避免余额被多次扣除。

### Q: Token 估算不准确怎么办?

A: 系统在预检查时预留 20% 缓冲:

**处理流程**:
- 估算成本 = 计算成本 × 1.2
- 先扣除预估费用
- API 调用完成后计算实际费用
- 多退少补

### Q: 如何处理余额不足?

A: 系统在多个环节检查余额:

**检查点**:
1. **API 调用前**: 估算费用并检查
2. **余额扣除时**: 再次精确检查
3. **余额不足时**: 抛出 InsufficientBalanceException

**建议措施**:
- 设置余额预警通知
- 启用自动充值功能
- 在客户端显示余额信息

### Q: 价格变动如何处理?

A: 价格表使用生效日期机制:

**查询逻辑**:
- 根据模型名称查询价格
- 筛选生效日期 <= 当前日期
- 按生效日期降序
- 取最新一条记录

**优势**:
- 新价格设置未来的生效日期
- 旧价格继续对历史数据生效
- 支持价格历史追溯

### Q: 如何实现不同用户不同价格?

A: 可以通过用户分组和价格策略:

**方案1: 用户分组**
- User 表添加 price_group 字段
- 价格表关联用户组
- 查询时根据用户组查询价格
- 支持: 普通用户、VIP用户等分组

**方案2: 用户倍率**
- User 表添加 price_multiplier 字段
- VIP 用户设置 0.8 (8折)
- 计算公式: 最终费用 = 基础成本 × 模型倍率 × 用户倍率 × 平台加成

## 🚀 性能优化

### 1. 缓存价格配置

**策略**:
- 使用 Spring Cache 缓存模型价格
- 缓存键: model_price:{modelName}
- 自动过期和更新
- 减少数据库查询

**实现**:
- @Cacheable 注解标记方法
- 缓存未命中时查询数据库
- 价格更新后清除缓存

### 2. 批量计费

**策略**:
- 支持批量计算多个API调用成本
- 减少数据库查询次数
- 批量获取价格配置
- 批量更新余额

**优势**:
- 提高计算效率
- 减少网络开销
- 降低数据库负载

### 3. 异步记录日志

**策略**:
- 使用 @Async 注解异步记录
- 不阻塞主业务流程
- 提高响应速度
- 日志失败不影响扣费

**注意事项**:
- 确保日志最终一致性
- 监控异步任务状态
- 异常时重试机制

### 4. 定期归档历史数据

**策略**:
- 归档90天前的余额日志
- 移动到历史表
- 从主表删除
- 减轻查询压力

**执行方式**:
- 定时任务自动执行
- 凌晨低峰期运行
- 批量操作提高效率
- 归档数据可用于审计

---

**上次更新**: 2025-01-19
**文档版本**: v2.0.0
