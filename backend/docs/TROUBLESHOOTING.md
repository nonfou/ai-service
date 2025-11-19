# 故障排查指南

## 🔍 常见问题

### 1. 应用启动失败

#### 问题：端口被占用

**错误信息**:
```
Web server failed to start. Port 8080 was already in use.
```

**解决方案**:
```bash
# 查找占用端口的进程
lsof -i :8080

# 或使用 netstat
netstat -tuln | grep 8080

# 杀死进程
kill -9 <PID>

# 或更换端口
export SERVER_PORT=8081
java -jar app.jar
```

---

#### 问题：数据库连接失败

**错误信息**:
```
Could not open JDBC Connection for transaction
Communications link failure
```

**排查步骤**:
1. 检查数据库是否启动
   ```bash
   sudo systemctl status mysql
   ```

2. 检查数据库连接配置
   ```bash
   # 测试连接
   mysql -h localhost -u root -p ai_api_platform
   ```

3. 检查防火墙
   ```bash
   sudo ufw status
   sudo ufw allow 3306/tcp
   ```

4. 检查 MySQL 配置
   ```bash
   # /etc/mysql/mysql.conf.d/mysqld.cnf
   bind-address = 0.0.0.0  # 允许远程连接
   ```

---

#### 问题：Redis 连接失败

**错误信息**:
```
Unable to connect to Redis
Connection refused
```

**解决方案**:
```bash
# 检查 Redis 是否启动
sudo systemctl status redis

# 启动 Redis
sudo systemctl start redis

# 测试连接
redis-cli ping

# 检查 Redis 配置
redis-cli
> CONFIG GET requirepass
```

---

### 2. 登录问题

#### 问题：验证码收不到

**可能原因**:
1. 邮件服务器配置错误
2. 邮箱授权码错误
3. 邮件被拦截（垃圾邮箱）
4. 发送频率限制

**排查步骤**:
```bash
# 1. 检查日志
tail -f logs/app.log | grep "email"

# 2. 测试 SMTP 连接
telnet smtp.qq.com 587

# 3. 检查 Redis 中的验证码
redis-cli
> GET verify_code:user@example.com

# 4. 检查邮件配置
curl http://localhost:8080/actuator/health
```

**解决方案**:
```yaml
# 确保邮件配置正确
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your-email@qq.com
    password: authorization-code  # 不是密码
```

---

#### 问题：Token 过期

**错误信息**:
```json
{
  "code": 401,
  "message": "Token 无效或已过期"
}
```

**解决方案**:
- 重新登录获取新 Token
- 检查系统时间是否正确
- 确认 JWT_EXPIRATION 配置

---

### 3. API 调用问题

#### 问题：余额不足

**错误信息**:
```json
{
  "code": 402,
  "message": "余额不足"
}
```

**解决方案**:
1. 充值余额
2. 检查账户余额
   ```bash
   curl http://localhost:8080/api/user/balance \
     -H "Authorization: Bearer <token>"
   ```

---

#### 问题：API Key 无效

**错误信息**:
```json
{
  "code": 401,
  "message": "API Key 无效"
}
```

**排查步骤**:
```sql
-- 检查 API Key 状态
SELECT * FROM api_keys WHERE api_key = 'sk-xxx';

-- 检查用户状态
SELECT * FROM users WHERE id = (
  SELECT user_id FROM api_keys WHERE api_key = 'sk-xxx'
);
```

---

#### 问题：模型不可用

**错误信息**:
```json
{
  "code": 400,
  "message": "没有可用的后端账户"
}
```

**排查步骤**:
```sql
-- 检查后端账户状态
SELECT * FROM backend_accounts WHERE status = 'active';

-- 检查账户健康状态
SELECT * FROM backend_accounts
WHERE provider = 'copilot' AND status = 'active';
```

**解决方案**:
1. 在管理后台添加后端账户
2. 检查账户 Token 是否有效
3. 执行健康检查
   ```bash
   curl http://localhost:8080/api/admin/backend-accounts/1/health-check \
     -H "Authorization: Bearer <admin-token>"
   ```

---

### 4. 支付问题

#### 问题：支付宝回调失败

**错误日志**:
```
Alipay notify signature verify failed
```

**排查步骤**:
1. 检查支付宝公钥配置
2. 检查回调 URL 是否可访问
3. 检查签名验证逻辑

```java
// 验证签名
boolean signVerified = AlipaySignature.rsaCheckV1(
    params,
    alipayPublicKey,
    charset,
    signType
);
```

---

#### 问题：微信支付回调失败

**排查步骤**:
1. 检查回调 URL 配置
2. 验证签名
3. 检查商户证书

```bash
# 测试回调 URL
curl -X POST https://yourdomain.com/api/recharge/wechat/notify \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

### 5. 性能问题

#### 问题：响应慢

**排查步骤**:

1. **检查数据库慢查询**
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';

-- 分析慢查询
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;
```

2. **检查 Redis 性能**
```bash
redis-cli --latency
redis-cli --stat
redis-cli info stats
```

3. **检查 JVM 内存**
```bash
jmap -heap <PID>
jstat -gcutil <PID> 1000
```

4. **检查线程状态**
```bash
jstack <PID> > thread_dump.txt
```

---

#### 问题：内存溢出

**错误信息**:
```
java.lang.OutOfMemoryError: Java heap space
```

**解决方案**:
```bash
# 增加堆内存
java -jar app.jar -Xms2g -Xmx4g

# 生成堆转储
java -jar app.jar \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heapdump.hprof
```

**分析堆转储**:
```bash
# 使用 jhat
jhat heapdump.hprof

# 或使用 Eclipse MAT
```

---

### 6. 数据问题

#### 问题：数据不一致

**场景**: 余额扣费但 API 调用失败

**排查**:
```sql
-- 检查余额日志
SELECT * FROM balance_log
WHERE user_id = 123
ORDER BY created_at DESC
LIMIT 10;

-- 检查 API 调用记录
SELECT * FROM api_calls
WHERE user_id = 123
ORDER BY created_at DESC
LIMIT 10;

-- 对比余额
SELECT balance FROM users WHERE id = 123;

SELECT SUM(amount) FROM balance_log WHERE user_id = 123;
```

**修复**:
```sql
-- 手动调整余额
UPDATE users SET balance = balance + 10.00 WHERE id = 123;

-- 添加调整记录
INSERT INTO balance_log (user_id, amount, balance_after, type, remark)
VALUES (123, 10.00, (SELECT balance FROM users WHERE id = 123), 'adjust', '补偿扣费');
```

---

## 🔧 诊断工具

### 1. 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# 详细健康信息
curl http://localhost:8080/actuator/health -H "Authorization: Bearer <admin-token>"
```

**输出示例**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    }
  }
}
```

---

### 2. 指标监控

```bash
# 所有指标
curl http://localhost:8080/actuator/metrics

# JVM 内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP 请求统计
curl http://localhost:8080/actuator/metrics/http.server.requests

# 数据库连接池
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

---

### 3. 日志分析

```bash
# 实时查看日志
tail -f logs/app.log

# 过滤错误日志
grep "ERROR" logs/app.log

# 统计错误类型
grep "ERROR" logs/app.log | awk '{print $NF}' | sort | uniq -c | sort -rn

# 查找特定用户的日志
grep "userId=123" logs/app.log
```

---

### 4. 数据库诊断

```sql
-- 查看连接数
SHOW PROCESSLIST;

-- 查看表状态
SHOW TABLE STATUS;

-- 查看索引使用情况
SELECT * FROM sys.schema_unused_indexes;

-- 分析表
ANALYZE TABLE api_calls;

-- 优化表
OPTIMIZE TABLE api_calls;
```

---

## 📊 监控告警

### 关键指标

| 指标 | 阈值 | 告警级别 |
|------|------|---------|
| API 错误率 | > 5% | 警告 |
| API 错误率 | > 10% | 严重 |
| 响应时间 P99 | > 2s | 警告 |
| 响应时间 P99 | > 5s | 严重 |
| 数据库连接池 | > 90% | 警告 |
| JVM 堆内存 | > 85% | 警告 |
| Redis 内存 | > 80% | 警告 |
| 磁盘使用率 | > 85% | 警告 |

---

## 🆘 紧急处理流程

### 1. 服务不可用

```bash
# 1. 快速检查
curl http://localhost:8080/actuator/health

# 2. 查看日志
tail -100 logs/app.log

# 3. 检查进程
ps aux | grep java

# 4. 重启服务
sudo systemctl restart ai-api-platform

# 5. 验证恢复
curl http://localhost:8080/actuator/health
```

---

### 2. 数据库连接耗尽

```sql
-- 1. 查看当前连接
SHOW PROCESSLIST;

-- 2. 杀死长时间运行的查询
KILL <process_id>;

-- 3. 重启连接池（应用层）
-- 或重启应用
```

---

### 3. 内存泄漏

```bash
# 1. 生成堆转储
jmap -dump:live,format=b,file=heap.hprof <PID>

# 2. 重启应用
sudo systemctl restart ai-api-platform

# 3. 分析堆转储
# 使用 MAT 或 VisualVM
```

---

## 📞 获取帮助

### 联系方式

- **技术支持**: support@example.com
- **问题反馈**: https://github.com/your-org/ai-service/issues
- **紧急热线**: +86 xxx-xxxx-xxxx

### 提供信息

报告问题时请提供：
1. 错误信息和堆栈跟踪
2. 相关日志片段
3. 系统环境信息
4. 复现步骤
5. 发生时间

---

**上次更新**: 2025-01-19
**文档版本**: v1.0.0
