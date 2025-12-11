-- ============================================
-- AI API Platform - 完整数据库初始化脚本
-- 版本: 2.0.0
-- 创建时间: 2025-11-09
-- 说明: 包含所有表结构、索引、默认数据及迁移内容
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) COMMENT '密码（加密后）',
    api_key VARCHAR(64) COMMENT '用户API密钥（已废弃，使用api_keys表）',
    balance DECIMAL(10, 4) DEFAULT 0.0000 COMMENT '账户余额（元）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. API密钥表 (支持多密钥管理)
-- ============================================
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '密钥ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    key_name VARCHAR(100) NOT NULL COMMENT '密钥名称',
    api_key VARCHAR(64) NOT NULL UNIQUE COMMENT 'API密钥',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_api_key (api_key),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API密钥表';

-- ============================================
-- 3. 充值订单表
-- ============================================
CREATE TABLE IF NOT EXISTS recharge_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    amount DECIMAL(10, 2) NOT NULL COMMENT '充值金额（元）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消',
    pay_method VARCHAR(20) COMMENT '支付方式：alipay, wechat',
    trade_no VARCHAR(100) COMMENT '第三方交易号',
    pay_time TIMESTAMP NULL COMMENT '支付时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_user_status (user_id, status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';

-- ============================================
-- 4. 余额变动日志表
-- ============================================
CREATE TABLE IF NOT EXISTS balance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10, 4) NOT NULL COMMENT '变动金额（正数为增加，负数为减少）',
    balance_after DECIMAL(10, 4) NOT NULL COMMENT '变动后余额',
    type VARCHAR(20) NOT NULL COMMENT '类型：recharge-充值，consume-消费',
    related_id BIGINT COMMENT '关联ID（充值订单ID或API调用ID）',
    remark VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_user_type (user_id, type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='余额变动日志表';

-- ============================================
-- 5. API调用日志表
-- ============================================
CREATE TABLE IF NOT EXISTS api_calls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '调用ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    api_key VARCHAR(64) NOT NULL COMMENT 'API Key',
    model VARCHAR(50) NOT NULL COMMENT '模型名称',
    backend_account_id BIGINT COMMENT '使用的后端账户ID',
    provider VARCHAR(50) COMMENT '提供商 (copilot/openrouter)',
    input_tokens INT DEFAULT 0 COMMENT '输入token数',
    output_tokens INT DEFAULT 0 COMMENT '输出token数',
    cache_read_tokens INT DEFAULT 0 COMMENT '缓存读取tokens',
    cache_write_tokens INT DEFAULT 0 COMMENT '缓存写入tokens',
    session_hash VARCHAR(64) COMMENT '会话哈希',
    cost DECIMAL(10, 6) DEFAULT 0.000000 COMMENT '费用（元）',
    raw_cost DECIMAL(20,10) DEFAULT 0 COMMENT '原始成本（元）',
    markup_rate DECIMAL(10,4) DEFAULT 1.0000 COMMENT '加成倍率',
    markup_cost DECIMAL(20,10) DEFAULT 0 COMMENT '加成金额（元）',
    request_time TIMESTAMP NOT NULL COMMENT '请求时间',
    response_time TIMESTAMP NULL COMMENT '响应时间',
    duration INT COMMENT '耗时（毫秒）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-成功，0-失败',
    error_msg TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_api_key (api_key),
    INDEX idx_model (model),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_backend_account (backend_account_id),
    INDEX idx_provider (provider),
    INDEX idx_session (session_hash),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_user_model (user_id, model),
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_date (user_id, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用日志表';

-- ============================================
-- 6. 模型配置表
-- ============================================
CREATE TABLE IF NOT EXISTS models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型ID',
    model_name VARCHAR(50) NOT NULL UNIQUE COMMENT '模型名称（如 gpt-4o）',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    provider VARCHAR(50) COMMENT '提供商 (copilot/openrouter/custom)',
    price_multiplier DECIMAL(5, 2) DEFAULT 1.00 COMMENT '价格倍率（1.0表示正常价格）',
    input_token_price DECIMAL(20, 10) NULL COMMENT '输入 token 价格（每百万 token 的美元价格）',
    output_token_price DECIMAL(20, 10) NULL COMMENT '输出 token 价格（每百万 token 的美元价格）',
    cache_read_token_price DECIMAL(20, 10) NULL COMMENT '缓存读取 token 价格（每百万 token 的美元价格）',
    cache_write_token_price DECIMAL(20, 10) NULL COMMENT '缓存写入 token 价格（每百万 token 的美元价格）',
    supports_streaming BOOLEAN DEFAULT TRUE COMMENT '是否支持流式输出',
    max_tokens INT DEFAULT 4096 COMMENT '最大token数',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    description TEXT COMMENT '模型说明',
    tags JSON COMMENT '模型标签列表,格式: ["推荐", "低价", "新品"]',
    sort_order INT DEFAULT 0 COMMENT '排序值,数值越小越靠前',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_model_name (model_name),
    INDEX idx_provider (provider),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

-- ============================================
-- 7. 订阅套餐表
-- ============================================
CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '套餐ID',
    plan_name VARCHAR(50) NOT NULL UNIQUE COMMENT '套餐标识名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description TEXT COMMENT '套餐说明',
    original_price DECIMAL(10, 2) NOT NULL COMMENT '原价（元）',
    price DECIMAL(10, 2) NOT NULL COMMENT '现价（元）',
    quota_amount DECIMAL(10, 2) NOT NULL COMMENT '额度金额（元）',
    features JSON COMMENT '功能特性列表',
    color_theme VARCHAR(20) DEFAULT 'blue' COMMENT '颜色主题: green-绿色, blue-蓝紫色, pink-粉红色',
    badge_text VARCHAR(50) COMMENT '徽章显示文字,如: 推荐套餐, 新用户专享',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_plan_name (plan_name),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅套餐表';

-- ============================================
-- 8. 订阅记录表
-- ============================================
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订阅ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_id BIGINT NOT NULL COMMENT '套餐ID',
    plan_name VARCHAR(100) NOT NULL COMMENT '套餐名称（冗余）',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额（元）',
    quota_amount DECIMAL(10, 2) NOT NULL COMMENT '获得额度（元）',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-生效中，expired-已过期，cancelled-已取消',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_status (status),
    INDEX idx_end_date (end_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅记录表';

-- ============================================
-- 9. 工单表
-- ============================================
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subject VARCHAR(200) NOT NULL COMMENT '主题',
    content TEXT NOT NULL COMMENT '内容',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待处理，processing-处理中，resolved-已解决，closed-已关闭',
    priority VARCHAR(20) DEFAULT 'normal' COMMENT '优先级：low-低，normal-正常，high-高，urgent-紧急',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- ============================================
-- 10. 工单消息表
-- ============================================
CREATE TABLE IF NOT EXISTS ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    user_id BIGINT COMMENT '用户ID（用户消息时非空）',
    admin_id BIGINT COMMENT '管理员ID（管理员回复时非空）',
    message TEXT NOT NULL COMMENT '消息内容',
    is_staff TINYINT DEFAULT 0 COMMENT '是否管理员消息：1-是，0-否',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单消息表';

-- ============================================
-- 11. 系统配置表
-- ============================================
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 12. 管理员表
-- ============================================
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    role VARCHAR(20) DEFAULT 'admin' COMMENT '角色',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ============================================
-- 13. 后端账户配置表（多账户中继）
-- ============================================
CREATE TABLE IF NOT EXISTS backend_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    provider ENUM('copilot', 'openrouter') NOT NULL COMMENT '提供商类型',
    access_token TEXT NOT NULL COMMENT '加密后的访问令牌(AES-256)',
    priority INT DEFAULT 1 COMMENT '优先级 1-100，数字越小优先级越高',
    status ENUM('active', 'disabled', 'error') DEFAULT 'active' COMMENT '账户状态',
    max_concurrent INT DEFAULT 10 COMMENT '最大并发请求数',
    rate_limit_per_minute INT DEFAULT 60 COMMENT '每分钟请求限制',
    cost_multiplier DECIMAL(10,4) DEFAULT 1.0000 COMMENT '成本倍率',
    error_count INT DEFAULT 0 COMMENT '连续错误次数',
    last_used_at DATETIME COMMENT '最后使用时间',
    last_error_at DATETIME COMMENT '最后错误时间',
    last_error_message TEXT COMMENT '最后错误信息',
    metadata JSON COMMENT '扩展信息 {"region": "us", "supported_models": []}',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_provider_status (provider, status),
    INDEX idx_priority (priority),
    INDEX idx_last_used (last_used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后端账户配置表';

-- ============================================
-- 14. 用户账户绑定表
-- ============================================
CREATE TABLE IF NOT EXISTS user_account_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    api_key_id BIGINT COMMENT 'API密钥ID（可选，为NULL时表示用户级绑定）',
    backend_account_id BIGINT NOT NULL COMMENT '后端账户ID',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认账户',
    binding_type ENUM('user', 'api_key') DEFAULT 'user' COMMENT '绑定类型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE,
    FOREIGN KEY (backend_account_id) REFERENCES backend_accounts(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_account (user_id, backend_account_id),
    INDEX idx_user_id (user_id),
    INDEX idx_api_key_id (api_key_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户绑定表';

-- ============================================
-- 15. 会话粘性映射表
-- ============================================
CREATE TABLE IF NOT EXISTS session_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_hash VARCHAR(64) NOT NULL COMMENT '会话哈希（SHA-256）',
    backend_account_id BIGINT NOT NULL COMMENT '绑定的后端账户ID',
    api_key_id BIGINT NOT NULL COMMENT 'API密钥ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    request_count INT DEFAULT 1 COMMENT '该会话请求次数',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_session (session_hash),
    INDEX idx_expires (expires_at),
    INDEX idx_account (backend_account_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (backend_account_id) REFERENCES backend_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话粘性映射表';

-- ============================================
-- 16. 用户配额管理表
-- ============================================
CREATE TABLE IF NOT EXISTS user_quotas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    quota_type ENUM('daily', 'monthly', 'custom') NOT NULL COMMENT '配额类型',
    quota_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '配额金额（元）',
    used_amount DECIMAL(20,2) DEFAULT 0.00 COMMENT '已使用金额（元）',
    reset_at DATETIME NOT NULL COMMENT '下次重置时间',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用配额限制',
    alert_threshold DECIMAL(5,2) DEFAULT 80.00 COMMENT '告警阈值（百分比）',
    last_alert_at DATETIME COMMENT '最后告警时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_quota_type (user_id, quota_type),
    INDEX idx_user_id (user_id),
    INDEX idx_reset_at (reset_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户配额管理表';

-- ============================================
-- 17. 视图：用户统计视图
-- ============================================
CREATE OR REPLACE VIEW v_user_stats AS
SELECT
    u.id,
    u.email,
    u.balance,
    u.status,
    u.created_at,
    COUNT(DISTINCT ac.id) AS total_calls,
    COALESCE(SUM(ac.cost), 0) AS total_cost,
    COALESCE(SUM(ac.input_tokens), 0) AS total_input_tokens,
    COALESCE(SUM(ac.output_tokens), 0) AS total_output_tokens,
    MAX(ac.created_at) AS last_call_time
FROM users u
LEFT JOIN api_calls ac ON u.id = ac.user_id
GROUP BY u.id, u.email, u.balance, u.status, u.created_at;

-- ============================================
-- 18. 插入默认数据 - 模型配置
-- ============================================
INSERT INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, status, description, tags, sort_order) VALUES
-- GPT 系列
('claude-sonnet-4.5', 'Claude Sonnet 4.5', 'Anthropic', 1.20, 10.00, 30.00, 1, 'Anthropic Claude Sonnet 4.5 最强编程模型', JSON_ARRAY(), 90),
('claude-haiku-4.5', 'Claude Haiku 4.5', 'Anthropic', 0.30, 3, 10, 1, 'Anthropic Claude Haiku 4.5 - 快速轻量', JSON_ARRAY('低价'), 110),
('gpt-5.1-codex', 'GPT-5.1 Codex', 'Open AI', 0.90, 10, 25.00, 1, 'OpenAI GPT-5.1 Codex 最新编程模型', JSON_ARRAY('推荐'), 30),
('gpt-5.1', 'GPT-5.1', 'Open AI', 1.00, 8.00, 25.00, 1, 'OpenAI GPT-5.1 模型 - 最新旗舰模型', JSON_ARRAY('推荐', '最新'), 10),
('gpt-5.1-codex-mini', 'GPT-5.1 Codex Mini', 'Open AI', 0.30, 5, 20.00, 1, 'OpenAI GPT-5.1 Codex Mini - 经济高效', JSON_ARRAY('推荐', '低价'), 20),

('gemini-2.5-pro', 'Gemini 2.5 Pro', 'Google', 0.3, 7.00, 21.00, 1, 'Google gemini 模型', JSON_ARRAY(), 40),
('gemini-3-pro', 'Gemini 3.0 Pro', 'Google', 0.8, 10, 30.0, 1, 'Google gemini 最强编程模型 - 快速经济', JSON_ARRAY('推荐'), 50)
-- Claude 系列

ON DUPLICATE KEY UPDATE
                     display_name = VALUES(display_name),
                     provider = VALUES(provider),
                     input_token_price = VALUES(input_token_price),
                     output_token_price = VALUES(output_token_price),
                     tags = VALUES(tags);

-- ============================================
-- 19. 插入默认数据 - 订阅套餐
-- ============================================
INSERT INTO subscription_plans (plan_name, display_name, description, original_price, price, quota_amount, features, color_theme, badge_text, sort_order) VALUES
                                                                                                                                                              ('trial_card', '体验卡', '适合新用户体验', 9.90, 4.90, 10.00,
                                                                                                                                                               JSON_ARRAY('1天有效期', '10元额度', '支持所有模型', '基础技术支持'),
                                                                                                                                                               'green', '新用户专享', 1),

                                                                                                                                                              ('max_100', 'Max 100', '适合轻度使用', 188.00, 98.00, 150.00,
                                                                                                                                                               JSON_ARRAY('30天有效期', '150元额度', '支持所有模型', '优先技术支持', 'API使用统计'),
                                                                                                                                                               'blue', '推荐套餐', 2),

                                                                                                                                                              ('max_200', 'Max 200', '适合中度使用', 700.00, 360.00, 600.00,
                                                                                                                                                               JSON_ARRAY('30天有效期', '600元额度', '支持所有模型', '优先技术支持', 'API使用统计', '专属客服'),
                                                                                                                                                               'pink', '高级套餐', 3);

-- ============================================
-- 20. 插入默认数据 - 系统配置
-- ============================================
INSERT INTO system_config (config_key, config_value, description) VALUES
('copilot_api_url', 'http://localhost:4141', 'Copilot API 代理地址'),
('copilot_github_token', '', 'GitHub Token'),
('input_token_price', '4.1', '输入token价格（元/百万）'),
('output_token_price', '16.4', '输出token价格（元/百万）'),
('rate_limit_per_minute', '60', '每分钟请求限制'),
('daily_free_quota', '0', '每日免费额度（元）'),
('system_notice', '欢迎使用 AI API Platform', '系统公告')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description);

-- ============================================
-- 21. 插入默认数据 - 管理员账户
-- ============================================
-- 密码: admin123 (使用 BCrypt 加密)
INSERT INTO admins (username, password, role, status) VALUES
('admin', '$2b$10$89wowyqpCvaZI74KBGHFSOkXAT8BEDyHBMEvUn0BM6hIly7rvzlUq', 'super_admin', 1)
ON DUPLICATE KEY UPDATE
    role = VALUES(role),
    status = VALUES(status);

-- ============================================
-- 22. 插入示例数据 - 后端账户（可选）
-- ============================================
-- 注意：access_token 需要实际加密后再使用
INSERT INTO backend_accounts (account_name, provider, access_token, priority, status, max_concurrent, rate_limit_per_minute, metadata) VALUES
('Copilot-Primary', 'copilot', 'ENCRYPTED_TOKEN_PLACEHOLDER_1', 1, 'active', 20, 100,
 JSON_OBJECT('region', 'global', 'description', '主要 Copilot 账户')),
('OpenRouter-Main', 'openrouter', 'ENCRYPTED_TOKEN_PLACEHOLDER_2', 1, 'active', 15, 60,
 JSON_OBJECT('region', 'us-east', 'description', '主要 OpenRouter 账户'))
ON DUPLICATE KEY UPDATE
    priority = VALUES(priority),
    status = VALUES(status);

-- ============================================
-- 23. 恢复外键检查
-- ============================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 完成提示
-- ============================================
SELECT '✅ 数据库初始化完成！' AS message,
       '包含以下表：' AS note,
       'users, api_keys, recharge_orders, balance_log, api_calls, models, subscription_plans, subscriptions, tickets, ticket_messages, system_config, admins, backend_accounts, user_account_bindings, session_mappings, user_quotas' AS tables;

SELECT
    '📊 统计信息：' AS info,
    (SELECT COUNT(*) FROM models) AS '模型数量',
    (SELECT COUNT(*) FROM subscription_plans) AS '套餐数量',
    (SELECT COUNT(*) FROM system_config) AS '系统配置项',
    (SELECT COUNT(*) FROM admins) AS '管理员数量',
    (SELECT COUNT(*) FROM backend_accounts) AS '后端账户数量';
