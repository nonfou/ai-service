# 统计分析模块

## 📖 概述

统计分析模块提供全面的数据统计和分析功能,帮助管理员和用户了解系统使用情况、消费趋势和业务指标。

## 🎯 核心功能

- 用户消费统计
- API调用统计
- 模型使用分析
- 收入统计和趋势
- Token使用统计
- 实时数据监控
- 报表导出(CSV/Excel)
- 数据可视化支持
- 定时聚合任务
- 缓存优化

## 📦 统计维度

### 1. 用户维度

**消费统计:**
- 总消费金额
- 日均消费
- 消费趋势图

**使用频率:**
- API调用次数
- 活跃天数
- 最后使用时间

**模型偏好:**
- 最常用模型
- 模型使用分布
- 各模型消费占比

**Token使用:**
- 输入Token统计
- 输出Token统计
- Token消费趋势

### 2. 系统维度

**总体指标:**
- 总用户数
- 活跃用户数(日活/月活)
- 总收入
- 总调用量

**增长趋势:**
- 日增长率
- 周增长率
- 月增长率
- 同比环比分析

**模型分布:**
- 各模型使用占比
- 热门模型排行
- 模型使用趋势

**时段分析:**
- 高峰时段识别
- 低谷时段识别
- 24小时调用分布
- 工作日/周末对比

### 3. 财务维度

**收入统计:**
- 充值收入
- 套餐收入
- 日收入趋势
- 月收入统计

**成本分析:**
- API调用成本
- 各模型成本
- 利润率计算
- 成本趋势

**订单统计:**
- 订单总量
- 订单成功率
- 平均订单金额
- 退款统计

### 4. 性能维度

**响应时间:**
- 平均响应时间
- P50响应时间
- P95响应时间
- P99响应时间

**成功率:**
- API调用成功率
- 各模型成功率
- 错误类型分布
- 失败原因分析

**并发量:**
- 峰值并发数
- 平均并发数
- 并发趋势图

## 📊 数据模型

### UserStatistics 实体

用户统计表,存储每个用户的日度统计数据:

**关联字段:**
- id: 主键ID
- userId: 用户ID
- statDate: 统计日期

**消费统计字段:**
- totalCost: 总消费金额
- avgCost: 平均消费

**调用统计字段:**
- callCount: 调用次数
- successCount: 成功次数
- failCount: 失败次数

**Token统计字段:**
- inputTokens: 输入Token数
- outputTokens: 输出Token数
- totalTokens: 总Token数

**时间统计字段:**
- totalDuration: 总耗时(毫秒)
- avgDuration: 平均耗时(毫秒)

**时间戳:**
- createdAt: 创建时间
- updatedAt: 更新时间

### ApiCallStatistics 实体

API调用统计表,存储API调用的聚合数据:

**时间维度:**
- id: 主键ID
- statDate: 统计日期
- statHour: 统计小时(0-23)

**分组维度:**
- modelName: 模型名称
- userId: 用户ID(可选)

**调用统计:**
- callCount: 调用次数
- successCount: 成功次数
- failCount: 失败次数
- successRate: 成功率

**Token统计:**
- inputTokens: 输入Token数
- outputTokens: 输出Token数
- totalTokens: 总Token数

**成本统计:**
- totalCost: 总成本
- avgCost: 平均成本

**性能统计:**
- avgDuration: 平均耗时
- p95Duration: P95耗时
- maxDuration: 最大耗时

**时间戳:**
- createdAt: 创建时间
- updatedAt: 更新时间

### RevenueStatistics 实体

收入统计表,存储财务相关的聚合数据:

**时间维度:**
- id: 主键ID
- statDate: 统计日期

**充值统计:**
- rechargeAmount: 充值金额
- rechargeCount: 充值次数
- avgRecharge: 平均充值金额

**套餐统计:**
- subscriptionAmount: 套餐销售金额
- subscriptionCount: 套餐销售数量

**消费统计:**
- consumeAmount: 消费金额
- consumeCount: 消费次数

**用户统计:**
- newUsers: 新增用户数
- activeUsers: 活跃用户数
- payingUsers: 付费用户数

**时间戳:**
- createdAt: 创建时间
- updatedAt: 更新时间

### ModelUsageStatistics 实体

模型使用统计表,按模型维度聚合数据:

**分组维度:**
- id: 主键ID
- statDate: 统计日期
- modelName: 模型名称

**使用统计:**
- callCount: 调用次数
- uniqueUsers: 独立用户数

**Token统计:**
- totalInputTokens: 总输入Token
- totalOutputTokens: 总输出Token
- avgInputTokens: 平均输入Token
- avgOutputTokens: 平均输出Token

**成本统计:**
- totalCost: 总成本
- totalRevenue: 总收入
- profit: 利润
- profitRate: 利润率(百分比)

**性能统计:**
- avgResponseTime: 平均响应时间
- successRate: 成功率

**时间戳:**
- createdAt: 创建时间
- updatedAt: 更新时间

## 🔧 核心组件

### 1. StatisticsController

**用户接口**,提供以下功能:

#### GET /api/statistics/overview - 获取统计概览

返回用户的整体统计数据:
- 总调用次数
- 总消费金额
- 总Token使用量
- 最近30天调用数
- 最近30天消费
- 日均消费
- 最常用模型
- 最后使用时间
- 活跃天数

数据来源:
- 查询api_call_records表聚合数据
- 使用Redis缓存5分钟
- 避免频繁数据库查询

#### GET /api/statistics/daily - 获取每日统计

查询指定日期范围的每日统计数据:
- 支持日期范围筛选
- 返回每日调用数、消费、Token使用
- 用于绘制趋势图表

查询方式:
- 从user_statistics表查询
- 按日期升序排列
- 返回列表数据

#### GET /api/statistics/models - 获取模型使用统计

返回用户各模型的使用情况:
- 各模型调用次数
- 各模型消费金额
- 各模型Token使用
- 使用占比

查询方式:
- 按模型分组聚合
- 计算占比
- 按调用次数倒序

#### GET /api/statistics/trend - 获取消费趋势

查询最近N天的消费趋势:
- 默认30天
- 支持自定义天数(7/30/90天)
- 返回每日消费数据

应用场景:
- 消费趋势图表
- 预测未来消费
- 识别异常消费

### 2. AdminStatisticsController

**管理员接口**,提供以下功能:

#### GET /api/admin/statistics/overview - 获取系统统计概览

返回系统整体运营数据:
- 总用户数
- 今日新增用户
- 今日活跃用户
- 总收入
- 今日收入
- 总调用量
- 今日调用量
- 平均响应时间

数据聚合:
- 从多个统计表查询
- 使用Redis缓存10分钟
- 提供实时监控数据

#### GET /api/admin/statistics/users - 获取用户统计

查询用户相关的统计数据:
- 用户增长趋势
- 活跃用户趋势
- 付费用户统计
- 用户留存率

支持筛选:
- 日期范围筛选
- 按天/周/月聚合

#### GET /api/admin/statistics/revenue - 获取收入统计

查询财务收入数据:
- 收入趋势图
- 充值统计
- 套餐销售统计
- 消费统计
- 利润分析

返回维度:
- 每日收入明细
- 收入类型分布
- 同比环比增长

#### GET /api/admin/statistics/models - 获取模型统计

查询各模型的使用和收入情况:
- 模型调用量排行
- 模型收入排行
- 模型成本分析
- 模型利润率

统计维度:
- 按模型分组
- 计算成本和收入
- 分析利润率

#### GET /api/admin/statistics/api-calls - 获取API调用统计

查询API调用的详细统计:
- 调用量趋势
- 成功率统计
- 响应时间分布
- 错误类型分析
- 时段分布

支持筛选:
- 按模型筛选
- 按用户筛选
- 按日期范围筛选

#### GET /api/admin/statistics/export - 导出统计报表

导出统计数据为Excel文件:
- 支持多种报表类型(用户/收入/模型/API)
- 支持日期范围筛选
- 生成Excel文件下载

导出内容:
- 详细统计数据
- 图表和分析
- 汇总信息

### 3. StatisticsService

**核心服务**,主要方法:

#### getUserOverview() - 获取用户统计概览

处理流程:
1. 尝试从Redis缓存获取
2. 如果缓存未命中,查询数据库
3. 聚合多个维度的数据:
   - 总体统计(总调用/总消费/总Token)
   - 最近30天统计
   - 最常用模型
   - 最后使用时间
   - 活跃天数
4. 将结果缓存到Redis(5分钟)
5. 返回统计数据

优化策略:
- 使用缓存减少数据库查询
- 合理设置缓存过期时间
- 避免缓存击穿

#### getUserDailyStats() - 获取用户每日统计

查询流程:
1. 从user_statistics表查询
2. 按用户ID和日期范围筛选
3. 按日期升序排列
4. 转换为VO对象返回

返回数据:
- 每日调用次数
- 每日消费金额
- 每日Token使用量
- 成功率

#### getSystemOverview() - 获取系统统计概览

聚合系统整体数据:
1. 尝试从缓存获取
2. 查询多个统计表:
   - 用户统计(总数/今日新增/活跃用户)
   - 收入统计(总收入/今日收入)
   - 调用统计(总调用量/今日调用量)
   - 性能统计(平均响应时间)
3. 缓存结果10分钟
4. 返回概览数据

应用场景:
- 管理后台首页展示
- 实时监控大屏
- 运营数据报告

#### getRevenueStats() - 获取收入统计

查询财务数据:
1. 从revenue_statistics表查询
2. 按日期范围筛选
3. 计算汇总数据:
   - 总收入
   - 总充值
   - 总套餐销售
   - 增长率
4. 返回每日明细和汇总

统计指标:
- 充值金额和次数
- 套餐销售金额和数量
- 消费金额和次数
- 新增/活跃/付费用户数

#### getModelUsageStats() - 获取模型使用统计

按模型维度统计:
1. 从model_usage_statistics表查询
2. 按日期范围筛选
3. 按模型分组
4. 计算汇总数据:
   - 调用量
   - 成本
   - 收入
   - 利润和利润率
5. 按调用量倒序排列

应用价值:
- 识别热门模型
- 评估模型盈利能力
- 优化模型配置

#### exportStatistics() - 导出统计报表

导出流程:
1. 根据类型查询数据
2. 使用POI生成Excel文件
3. 设置表头和数据行
4. 添加格式和样式
5. 返回字节数组

支持类型:
- user: 用户统计报表
- revenue: 收入统计报表
- model: 模型统计报表
- api: API调用统计报表

### 4. StatisticsAggregationScheduler

**定时聚合任务**,自动生成统计数据:

#### aggregateUserStatistics() - 聚合用户统计

执行时间:每天凌晨1点

处理流程:
1. 查询昨天的日期
2. 查询所有有调用记录的用户
3. 对每个用户:
   - 统计昨天的调用次数、成功失败数
   - 统计Token使用量
   - 统计消费金额
   - 计算平均耗时
   - 插入或更新user_statistics表
4. 记录处理日志

优化措施:
- 批量处理提高效率
- 异常不中断其他用户处理
- 记录处理结果和耗时

#### aggregateApiCallStatistics() - 聚合API调用统计

执行时间:每小时整点

处理流程:
1. 查询上一小时的时间范围
2. 按模型、小时分组聚合数据
3. 统计调用数、成功率、Token、成本
4. 计算性能指标(平均/P95/最大耗时)
5. 插入或更新api_call_statistics表

聚合维度:
- 按日期+小时聚合
- 按模型分组
- 可选按用户分组

#### aggregateRevenueStatistics() - 聚合收入统计

执行时间:每天凌晨2点

处理流程:
1. 查询昨天的日期
2. 统计充值数据:
   - 从recharge_orders表查询成功订单
   - 汇总金额和次数
   - 计算平均充值
3. 统计套餐数据:
   - 从subscriptions表查询新订阅
   - 汇总金额和数量
4. 统计消费数据:
   - 从api_call_records表查询
   - 汇总消费金额和次数
5. 统计用户数据:
   - 新增用户数
   - 活跃用户数
   - 付费用户数
6. 插入revenue_statistics表

#### aggregateModelUsageStatistics() - 聚合模型使用统计

执行时间:每天凌晨3点

处理流程:
1. 查询昨天的日期
2. 按模型分组查询数据
3. 对每个模型:
   - 统计调用量和独立用户数
   - 统计Token使用量
   - 计算成本和收入
   - 计算利润和利润率
   - 统计性能指标
   - 插入或更新model_usage_statistics表
4. 清理超过90天的历史数据

业务价值:
- 了解各模型表现
- 优化定价策略
- 决策模型上下架

## 🗄️ 数据库配置

### user_statistics 表

用户统计表,存储用户的日度聚合数据。

**主要字段:**
- id: 主键
- user_id: 用户ID
- stat_date: 统计日期
- total_cost: 总消费
- avg_cost: 平均消费
- call_count: 调用次数
- success_count: 成功次数
- fail_count: 失败次数
- input_tokens: 输入Token
- output_tokens: 输出Token
- total_tokens: 总Token
- total_duration: 总耗时
- avg_duration: 平均耗时
- created_at: 创建时间
- updated_at: 更新时间

**索引设计:**
- idx_user_date: (user_id, stat_date) - 联合主键索引
- idx_stat_date: 统计日期索引

**数据保留策略:**
- 保留最近90天数据
- 超过90天的归档到历史表
- 定期清理提高查询性能

### api_call_statistics 表

API调用统计表,按时间和模型维度聚合。

**主要字段:**
- id: 主键
- stat_date: 统计日期
- stat_hour: 统计小时(0-23)
- model_name: 模型名称
- user_id: 用户ID(可选)
- call_count: 调用次数
- success_count: 成功次数
- fail_count: 失败次数
- success_rate: 成功率
- input_tokens: 输入Token
- output_tokens: 输出Token
- total_tokens: 总Token
- total_cost: 总成本
- avg_cost: 平均成本
- avg_duration: 平均耗时
- p95_duration: P95耗时
- max_duration: 最大耗时
- created_at: 创建时间
- updated_at: 更新时间

**索引设计:**
- idx_date_hour_model: (stat_date, stat_hour, model_name) - 复合索引
- idx_stat_date: 统计日期索引
- idx_model_name: 模型名称索引

**分区策略:**
- 按月分区
- 提高查询性能
- 便于历史数据归档

### revenue_statistics 表

收入统计表,存储财务相关的日度数据。

**主要字段:**
- id: 主键
- stat_date: 统计日期
- recharge_amount: 充值金额
- recharge_count: 充值次数
- avg_recharge: 平均充值
- subscription_amount: 套餐金额
- subscription_count: 套餐数量
- consume_amount: 消费金额
- consume_count: 消费次数
- new_users: 新增用户
- active_users: 活跃用户
- paying_users: 付费用户
- created_at: 创建时间
- updated_at: 更新时间

**索引设计:**
- idx_stat_date: 统计日期索引(唯一)

**数据特点:**
- 每日一条记录
- 用于财务报表和趋势分析
- 永久保留不删除

### model_usage_statistics 表

模型使用统计表,按模型维度聚合。

**主要字段:**
- id: 主键
- stat_date: 统计日期
- model_name: 模型名称
- call_count: 调用次数
- unique_users: 独立用户数
- total_input_tokens: 总输入Token
- total_output_tokens: 总输出Token
- avg_input_tokens: 平均输入Token
- avg_output_tokens: 平均输出Token
- total_cost: 总成本
- total_revenue: 总收入
- profit: 利润
- profit_rate: 利润率
- avg_response_time: 平均响应时间
- success_rate: 成功率
- created_at: 创建时间
- updated_at: 更新时间

**索引设计:**
- idx_date_model: (stat_date, model_name) - 联合索引
- idx_stat_date: 统计日期索引

**业务价值:**
- 模型表现评估
- 定价策略优化
- 资源分配决策

## 💻 前端集成

### 统计概览页面

展示用户或系统的统计概览:

**卡片式展示:**
- 总调用次数卡片
- 总消费金额卡片
- 总Token使用卡片
- 活跃天数卡片

**趋势图表:**
- 消费趋势折线图(ECharts)
- 调用量趋势柱状图
- Token使用趋势面积图

**模型分布:**
- 模型使用饼图
- 模型排行榜
- 模型详细数据表格

### 数据图表组件

使用ECharts实现各类图表:

**折线图:**
- 展示时间序列趋势
- 支持多条曲线对比
- 支持缩放和数据点提示

**柱状图:**
- 展示分类数据对比
- 支持堆叠柱状图
- 支持横向和纵向

**饼图:**
- 展示占比分布
- 支持环形图样式
- 支持数据标签

**面积图:**
- 展示累积趋势
- 突出数据总量
- 支持渐变填充

### 报表导出功能

前端实现导出功能:

**导出按钮:**
- 位于数据表格上方
- 点击触发导出请求
- 显示导出进度

**处理流程:**
1. 用户点击导出按钮
2. 前端发送GET请求到导出接口
3. 设置responseType为'blob'
4. 接收二进制数据
5. 创建下载链接
6. 触发浏览器下载
7. 显示导出成功提示

**用户体验:**
- 导出中显示loading
- 支持取消导出
- 文件名自动添加日期

### API调用示例

前端如何调用统计接口:

**获取概览数据:**
- 接口: GET /api/statistics/overview
- 请求头: Authorization + JWT Token
- 返回: JSON格式的统计数据
- 展示在概览卡片中

**获取趋势数据:**
- 接口: GET /api/statistics/trend?days=30
- 参数: days(天数)
- 返回: 数组格式的每日数据
- 用于绘制趋势图表

**导出报表:**
- 接口: GET /api/admin/statistics/export
- 参数: type、startDate、endDate
- 返回: Excel文件二进制流
- 触发浏览器下载

## 🚀 性能优化

### 索引优化

**单列索引:**
- stat_date: 日期查询
- user_id: 用户查询
- model_name: 模型查询

**复合索引:**
- (user_id, stat_date): 用户日期联合查询
- (stat_date, stat_hour, model_name): API调用统计查询
- (stat_date, model_name): 模型统计查询

**索引维护:**
- 定期分析索引使用率
- 删除未使用的索引
- 重建碎片化索引

### 缓存策略

**多级缓存架构:**

**一级缓存(Redis):**
- 概览数据: 5分钟过期
- 趋势数据: 10分钟过期
- 排行榜数据: 15分钟过期

**二级缓存(本地缓存):**
- 常用配置数据
- 模型列表
- 减少Redis访问

**缓存更新策略:**
- 定时任务执行后主动更新缓存
- 用户查询时懒加载
- 设置合理的过期时间

**缓存穿透防护:**
- 布隆过滤器拦截无效查询
- 空值缓存短期存储
- 接口限流保护

### 查询优化

**分页优化:**
- 使用游标分页替代offset
- 限制最大页数
- 深分页提示用户缩小范围

**聚合优化:**
- 使用统计表而非实时聚合
- 预计算常用指标
- 异步处理复杂统计

**慢查询监控:**
- 记录超过1秒的查询
- 分析慢查询原因
- 优化SQL或添加索引

### 定时任务优化

**任务调度:**
- 错峰执行避免资源冲突
- 控制并发数量
- 设置超时保护

**批量处理:**
- 批量插入替代逐条插入
- 使用事务保证一致性
- 合理设置批次大小(1000条)

**异常处理:**
- 单条失败不影响批次
- 记录详细错误日志
- 失败数据重试机制

## 🔍 常见问题

### Q: 统计数据延迟怎么办?

A: 统计数据有一定延迟是正常的:

**实时数据:**
- 概览数据缓存5-10分钟
- 可接受的延迟范围
- 刷新缓存可获取最新数据

**聚合数据:**
- 定时任务每小时或每天执行
- 查看聚合时间了解数据时效性
- 重要决策建议使用最新聚合数据

**优化方案:**
- 提高聚合频率(需权衡性能)
- 关键指标提供实时查询接口
- 在界面上标注数据更新时间

### Q: 如何处理大数据量?

A: 采用多种策略处理大数据量:

**数据分区:**
- 按月或周分区统计表
- 查询时只扫描相关分区
- 提高查询性能3-10倍

**数据归档:**
- 90天以上数据归档到历史表
- 主表只保留热数据
- 归档数据支持离线查询

**数据采样:**
- 超大数据量使用采样统计
- 保证统计准确性
- 大幅降低计算量

**分布式处理:**
- 使用Spark或Flink处理海量数据
- 离线计算复杂指标
- 结果存储到统计表

### Q: 导出报表慢怎么优化?

A: 报表导出优化方案:

**异步导出:**
- 大数据量改为异步导出
- 后台生成文件
- 完成后通知用户下载

**分页导出:**
- 限制单次导出数量
- 支持分批导出
- 最终合并文件

**数据预处理:**
- 提前聚合常用报表数据
- 导出时直接读取
- 避免实时复杂计算

**格式优化:**
- CSV格式比Excel快
- 简化格式和样式
- 减少公式和图表

## 📈 扩展功能

### 自定义报表

支持用户自定义报表:

**报表配置:**
- 选择数据维度
- 选择统计指标
- 选择时间范围
- 选择分组方式

**报表模板:**
- 预定义常用报表模板
- 用户可保存自定义报表
- 支持分享报表配置

**定时报表:**
- 配置定时生成报表
- 自动发送到邮箱
- 支持周报、月报

### 数据对比分析

支持时间段对比:

**同比分析:**
- 今年与去年同期对比
- 识别年度增长趋势
- 评估季节性影响

**环比分析:**
- 本期与上期对比
- 识别短期趋势
- 快速发现异常

**多维度对比:**
- 模型对比
- 用户群体对比
- 时段对比

### 智能预警

基于统计数据的智能预警:

**异常预警:**
- 消费突然激增
- 成功率突然下降
- 响应时间突然增加

**阈值预警:**
- 余额低于设定值
- 调用量超过配额
- 成本超过预算

**趋势预警:**
- 预测余额耗尽时间
- 预测月度消费
- 提前提醒充值

### 数据看板

实时监控大屏:

**关键指标:**
- 实时调用量
- 实时并发数
- 实时成功率
- 实时收入

**趋势图表:**
- 24小时趋势
- 7天趋势
- 30天趋势

**地域分布:**
- 用户地域分布
- 调用量地图
- 热力图展示

**自动刷新:**
- 每分钟自动刷新
- WebSocket实时推送
- 无需手动刷新

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
