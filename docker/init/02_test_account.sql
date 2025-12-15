-- ============================================
-- 测试账户初始化脚本
-- 创建时间: 2025-12-12
-- 说明: 创建测试账户并分配 500 元余额
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 创建测试用户
-- ============================================
-- 密码: test123456 (使用 BCrypt 加密)
INSERT INTO users (email, password, balance, status) VALUES
('79667276@qq.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 500.0000, 1)
ON DUPLICATE KEY UPDATE
    balance = 500.0000,
    status = 1;

-- 获取用户ID
SET @test_user_id = (SELECT id FROM users WHERE email = '79667276@qq.com');

-- ============================================
-- 2. 创建默认 API Key
-- ============================================
INSERT INTO api_keys (user_id, key_name, api_key, status) VALUES
(@test_user_id, '默认密钥', 'sk-test-1234567890abcdef1234567890abcdef', 1)
ON DUPLICATE KEY UPDATE
    status = 1;

-- ============================================
-- 3. 记录充值日志
-- ============================================
INSERT INTO balance_log (user_id, amount, balance_after, type, remark) VALUES
(@test_user_id, 500.0000, 500.0000, 'recharge', '系统初始化测试账户额度');

-- ============================================
-- 4. 创建对应的充值订单记录
-- ============================================
INSERT INTO recharge_orders (user_id, order_no, amount, status, pay_method, trade_no, pay_time) VALUES
(@test_user_id, CONCAT('TEST', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '001'), 500.00, 1, 'system', 'SYSTEM_INIT', NOW());

-- ============================================
-- 完成提示
-- ============================================
SELECT '测试账户创建成功！' AS message;
SELECT
    '账户信息：' AS info,
    '79667276@qq.com' AS 邮箱,
    'test123456' AS 密码,
    '500.00 元' AS 余额,
    'sk-test-1234567890abcdef1234567890abcdef' AS API密钥;
