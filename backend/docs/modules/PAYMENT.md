# 支付集成模块

## 📖 概述

支付模块集成了支付宝和微信支付两大主流支付渠道,提供安全、便捷的在线充值功能。系统支持多种支付方式,并通过严格的签名验证机制确保交易安全。

## 🎯 核心功能

- **多支付渠道**: 支付宝 PC 网站支付、微信 Native 扫码支付
- **订单管理**: 创建订单、查询状态、自动过期
- **异步通知**: 处理支付平台的异步回调通知
- **签名验证**: RSA2/APIv3 签名验证确保安全
- **防重复处理**: Redis 锁防止重复通知
- **余额自动发放**: 支付成功后自动充值余额
- **订单查询**: 主动查询订单状态
- **退款支持**: 支持订单退款(可选)

## 💳 支付方式

### 1. 支付宝 PC 网站支付

**特点**:
- 适用于电脑端网页支付
- 跳转到支付宝收银台
- 支持同步跳转 (return_url) 和异步通知 (notify_url)
- 使用 RSA2 签名算法

**支付流程**:
```
用户发起充值
  ↓
创建充值订单 (待支付)
  ↓
调用支付宝 API 生成支付表单
  ↓
用户跳转到支付宝页面
  ↓
用户完成支付
  ↓
支付宝发送异步通知 → 验证签名
  ↓                    ↓
同步跳转回商户页面    更新订单状态
                      ↓
                      发放余额
                      ↓
                      记录余额变动日志
```

**支付宝 SDK 配置**:
- 网关地址: https://openapi.alipay.com/gateway.do
- 应用ID (appId)
- 应用私钥 (privateKey)
- 支付宝公钥 (alipayPublicKey)
- 数据格式: json
- 字符编码: UTF-8
- 签名算法: RSA2

### 2. 微信 Native 扫码支付

**特点**:
- 生成二维码供用户扫码
- 适用于 PC 端和移动端
- 仅支持异步通知 (notify_url)
- 使用微信支付 APIv3 签名验证

**支付流程**:
```
用户发起充值
  ↓
创建充值订单 (待支付)
  ↓
调用微信统一下单 API
  ↓
获取 code_url (二维码链接)
  ↓
前端生成二维码展示给用户
  ↓
用户使用微信扫码支付
  ↓
微信发送异步通知
  ↓
验证签名
  ↓
更新订单状态
  ↓
发放余额
  ↓
记录余额变动日志
```

**微信支付配置**:
- 公众号/小程序 AppId
- 商户号 (mchId)
- 商户密钥 (mchKey)
- APIv3 密钥 (apiV3Key)
- 证书序列号 (certSerialNo)
- 商户私钥 (privateKey)

## 📊 数据模型

### RechargeOrder 实体

**充值订单表**,记录所有充值订单信息:
- **id**: 主键ID
- **orderNo**: 订单号 (唯一)
- **userId**: 用户ID
- **amount**: 充值金额
- **paymentMethod**: 支付方式 (alipay/wechat)
- **tradeNo**: 第三方交易号
- **status**: 状态 (0-待支付, 1-已支付, 2-已取消)
- **notifyData**: 回调原始数据
- **paidAt**: 支付时间
- **expireAt**: 过期时间
- **createdAt**: 创建时间
- **updatedAt**: 更新时间

### 订单状态

| 状态码 | 状态名称 | 说明 |
|--------|---------|------|
| 0 | 待支付 | 订单已创建,等待支付 |
| 1 | 已支付 | 支付成功,余额已发放 |
| 2 | 已取消 | 订单已取消或过期 |

## 🔧 核心组件

### 1. PaymentController

**职责**: 处理支付相关的 HTTP 请求

**位置**: `src/main/java/com/aiservice/controller/PaymentController.java`

**核心接口**:

#### POST /api/payment/recharge
创建充值订单:
- 接收参数: amount (充值金额), paymentMethod (支付方式)
- 参数验证:
  - 金额必须大于0
  - 单笔充值不超过10000元
  - 支付方式为 alipay 或 wechat
- 调用 PaymentService.createRecharge()
- 返回: 订单号、金额、过期时间、支付表单或二维码链接

#### GET /api/payment/order/{orderNo}
查询订单状态:
- 接收参数: orderNo (订单号)
- 验证用户权限 (只能查询自己的订单)
- 返回: 订单状态、金额、支付方式、支付时间等

#### POST /api/payment/order/{orderNo}/cancel
取消订单:
- 接收参数: orderNo (订单号)
- 验证订单状态 (只能取消待支付订单)
- 更新订单状态为已取消

#### POST /api/payment/alipay/notify
支付宝异步通知:
- 接收支付宝回调参数
- 调用 PaymentService.handleAlipayNotify()
- 成功返回 "success"
- 失败返回 "failure"

#### POST /api/payment/wechat/notify
微信异步通知:
- 接收微信回调数据
- 调用 PaymentService.handleWechatNotify()
- 成功返回 XML 格式: `<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>`
- 失败返回 XML 格式: `<xml><return_code><![CDATA[FAIL]]></return_code></xml>`

#### GET /api/payment/query/{orderNo}
查询支付结果:
- 主动查询支付平台订单状态
- 用于前端轮询确认支付结果
- 返回: 订单号、状态、是否已支付

### 2. PaymentService

**职责**: 支付业务逻辑处理

**位置**: `src/main/java/com/aiservice/service/PaymentService.java`

**核心方法**:

#### createRecharge()
创建充值订单:
1. 生成唯一订单号 (格式: RCH + yyyyMMddHHmmss + 6位随机数)
2. 创建订单记录,状态为待支付
3. 设置过期时间为30分钟
4. 根据支付方式调用对应支付渠道
5. 返回订单信息和支付凭证

**支付宝流程**:
- 调用 createAlipayOrder()
- 使用 AlipayTradePagePayRequest 创建支付
- 设置 notify_url 和 return_url
- 返回 HTML 支付表单

**微信流程**:
- 调用 createWechatOrder()
- 使用 WxPayUnifiedOrderRequest 统一下单
- 金额单位转换为分
- 返回二维码链接 (code_url)

#### handleAlipayNotify()
处理支付宝异步通知:
1. 验证 RSA2 签名
2. 获取订单号、交易号、交易状态、金额
3. 使用 Redis 分布式锁防止重复处理
4. 查询订单并验证:
   - 订单存在性
   - 订单状态 (已处理则忽略)
   - 金额匹配性
5. 交易状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时处理支付成功
6. 释放分布式锁

#### handleWechatNotify()
处理微信异步通知:
1. 解析通知数据 (SDK 自动验证签名)
2. 获取订单号、交易号、金额 (分转元)
3. 使用 Redis 分布式锁防止重复处理
4. 查询订单并验证:
   - 订单存在性
   - 订单状态 (已处理则忽略)
   - 金额匹配性
5. 处理支付成功
6. 释放分布式锁

#### processPaymentSuccess()
处理支付成功:
1. 更新订单状态为已支付
2. 记录第三方交易号
3. 保存回调原始数据
4. 记录支付时间
5. 调用 BalanceService.recharge() 发放余额
6. 记录日志

#### queryPaymentResult()
主动查询支付结果:
1. 查询本地订单状态
2. 如果已支付,直接返回
3. 否则调用支付平台查询:
   - 支付宝: queryAlipayOrder()
   - 微信: queryWechatOrder()
4. 返回支付状态

#### getOrderStatus()
查询订单状态:
- 根据订单号和用户ID查询
- 返回订单详情 VO

#### cancelOrder()
取消订单:
- 验证订单状态 (只能取消待支付订单)
- 更新状态为已取消
- 记录日志

**内部方法**:
- **createAlipayOrder()**: 调用支付宝 SDK 创建支付
- **createWechatOrder()**: 调用微信支付 SDK 统一下单
- **queryAlipayOrder()**: 查询支付宝订单状态
- **queryWechatOrder()**: 查询微信订单状态
- **generateOrderNo()**: 生成唯一订单号
- **getStatusName()**: 获取状态中文名称

### 3. 定时任务: 订单自动过期

**位置**: `src/main/java/com/aiservice/schedule/OrderExpireScheduler.java`

**执行周期**: 每5分钟执行一次 (cron: 0 */5 * * * ?)

**执行逻辑**:
1. 查询已过期但状态为待支付的订单
2. 筛选条件: status=0 AND expireAt < now
3. 批量更新订单状态为已取消 (status=2)
4. 记录每个过期订单的日志
5. 输出过期订单总数统计

## 🔐 安全机制

### 1. 签名验证

**支付宝 RSA2 签名验证**:
- 使用 AlipaySignature.rsaCheckV1() 验证
- 参数: 通知参数、支付宝公钥、字符编码、签名算法
- 验证失败直接返回 false,不处理通知

**微信 APIv3 签名验证**:
- WxJava SDK 内部自动验证签名
- parseOrderNotifyResult() 方法会验证签名有效性
- 签名失败会抛出异常

### 2. 防重复通知

使用 Redis 分布式锁机制:
- 锁键格式: "pay_notify:" + orderNo
- 锁过期时间: 5分钟
- 使用 setIfAbsent 原子操作获取锁
- 获取失败时忽略重复通知,返回 success
- finally 块确保锁释放

**防重复效果**:
- 同一订单的通知同时只能有一个在处理
- 已处理订单再次收到通知时直接忽略
- 避免重复发放余额

### 3. 金额验证

**验证逻辑**:
- 通知金额与订单金额必须完全一致
- 使用 BigDecimal.compareTo() 精确比较
- 不匹配时记录错误日志并返回 false

**防护目的**:
- 防止金额篡改攻击
- 确保充值金额准确

### 4. 订单状态验证

**验证逻辑**:
- 检查订单当前状态
- 已支付订单 (status=1) 不再处理
- 返回 true 让支付平台停止通知

**防护目的**:
- 避免重复发放余额
- 保护财务数据准确性

## 🔧 配置说明

### application.yml 配置

**支付配置项**:

**支付宝配置**:
- **app-id**: 应用ID
- **private-key**: 应用私钥
- **public-key**: 支付宝公钥
- **gateway-url**: 网关地址 (https://openapi.alipay.com/gateway.do)
- **notify-url**: 异步通知地址
- **return-url**: 同步跳转地址
- **sign-type**: 签名算法 (RSA2)

**微信支付配置**:
- **app-id**: 公众号/小程序 AppId
- **mch-id**: 商户号
- **mch-key**: 商户密钥
- **api-v3-key**: APIv3 密钥
- **cert-serial-no**: 证书序列号
- **private-key-path**: 商户私钥文件路径
- **notify-url**: 异步通知地址

**订单配置**:
- **expire-minutes**: 订单过期时间 (默认30分钟)
- **min-amount**: 最小充值金额 (默认1.00元)
- **max-amount**: 最大充值金额 (默认10000元)

## 💻 前端集成

### 1. 创建充值订单

**请求方式**: POST /api/payment/recharge

**请求参数**:
- amount: 充值金额
- paymentMethod: 支付方式 ('alipay' 或 'wechat')

**请求头**:
- Authorization: Bearer {token}
- Content-Type: application/json

**响应处理**:

**支付宝**:
- 响应包含 payForm 字段 (HTML 表单)
- 在页面中创建 div 容器
- 插入 payForm 内容
- 自动提交表单跳转到支付宝

**微信**:
- 响应包含 codeUrl 字段 (二维码链接)
- 使用 qrcode.js 生成二维码
- 展示给用户扫码
- 启动轮询查询支付结果

### 2. 微信二维码展示

**生成二维码**:
- 使用 QRCode 库
- 尺寸: 256x256
- 显示在支付页面中心

**轮询查询**:
- 每3秒调用一次 /api/payment/query/{orderNo}
- 检查 paid 字段
- 支付成功后停止轮询,提示用户并刷新页面
- 5分钟后自动停止轮询

### 3. 查询订单状态

**请求方式**: GET /api/payment/order/{orderNo}

**请求头**:
- Authorization: Bearer {token}

**响应信息**:
- orderNo: 订单号
- amount: 充值金额
- status: 订单状态
- statusName: 状态中文名称
- paymentMethod: 支付方式
- createdAt: 创建时间
- paidAt: 支付时间

## 📈 统计查询

### 充值统计

**每日充值统计**:
- 按日期和支付方式分组
- 统计订单数量和总金额
- 筛选已支付订单 (status=1)
- 最近30天数据
- 按日期降序排列

**查询维度**:
- 日期 (DATE(paid_at))
- 支付方式 (payment_method)
- 订单数量 (COUNT(*))
- 总金额 (SUM(amount))

**用户充值明细**:
- 按用户ID分组
- 统计充值次数
- 累计充值总额
- 最后充值时间
- 按总额降序,取前20名

**应用场景**:
- 识别高价值用户
- 分析充值趋势
- 评估支付渠道效果

## 🔍 常见问题

### Q: 回调地址无法访问?

A: 确保回调地址满足以下条件:
- **可公网访问**: 支付平台无法访问内网地址
- **HTTPS 协议**: 生产环境必须使用 HTTPS
- **正确配置**: 在支付平台后台正确配置回调地址

**本地开发解决方案**:
- 使用内网穿透工具 (如 ngrok, frp)
- 或使用支付宝/微信的沙箱环境测试

### Q: 签名验证失败?

A: 检查以下配置:

**支付宝**:
- 应用私钥是否正确
- 支付宝公钥是否正确 (注意不是应用公钥)
- 签名算法是否为 RSA2

**微信**:
- 商户号是否正确
- APIv3 密钥是否正确
- 证书序列号是否正确

### Q: 收到重复通知怎么办?

A: 系统已通过 Redis 锁防止重复处理:

**防重复机制**:
- 锁键: "pay_notify:" + orderNo
- 锁过期时间: 5分钟
- 使用 setIfAbsent 原子操作

即使收到重复通知也不会重复发放余额。

### Q: 如何处理支付失败?

A: 支付失败有几种情况:

**用户取消支付**:
- 订单保持待支付状态
- 30分钟后自动过期

**支付超时**:
- 定时任务自动将过期订单设为已取消

**支付异常**:
- 用户可以重新发起充值

### Q: 是否支持退款?

A: 可以实现退款功能:

**支付宝退款**:
- 使用 AlipayTradeRefundRequest
- 参数: 订单号、退款金额、退款原因
- 调用 alipayClient.execute() 执行退款

**微信退款**:
- 使用 WxPayRefundRequest
- 参数: 订单号、退款单号、总金额、退款金额、退款原因
- 调用 wxPayService.refund() 执行退款

### Q: 如何测试支付功能?

A: 使用沙箱环境测试:

**支付宝沙箱**:
- 登录支付宝开放平台
- 进入沙箱应用
- 使用沙箱账号测试

**微信支付沙箱**:
- 使用微信支付提供的测试商户号
- 测试环境使用固定金额 (如 0.01 元)

## 🚀 性能优化

### 1. 异步处理通知

**策略**:
- 使用 @Async 注解标记方法
- 异步执行耗时操作
- 如发送通知邮件、记录详细日志等

**优势**:
- 快速响应支付平台
- 避免超时导致重复通知
- 提高系统吞吐量

### 2. 缓存订单信息

**策略**:
- 使用 @Cacheable 缓存订单查询
- 缓存键: order:{orderNo}
- 缓存时间: 5-10分钟

**优势**:
- 减少数据库查询
- 提高查询速度
- 降低数据库负载

### 3. 批量查询订单状态

**策略**:
- 实现批量查询方法
- 一次性查询多个订单
- 减少 API 调用次数

**应用场景**:
- 管理后台批量查询
- 用户订单列表展示
- 定时对账任务

### 4. 数据库索引优化

**关键索引**:

**订单号唯一索引**:
- 字段: order_no
- 类型: UNIQUE INDEX
- 用途: 快速查询订单,防止重复

**用户ID + 状态索引**:
- 字段: (user_id, status)
- 类型: INDEX
- 用途: 查询用户订单列表

**过期时间索引**:
- 字段: (expire_at, status)
- 类型: INDEX
- 用途: 定时任务查询过期订单

---

**上次更新**: 2025-01-19
**文档版本**: v2.0.0
