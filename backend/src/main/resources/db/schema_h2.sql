-- ============================================
-- AI API Platform - H2 数据库初始化脚本
-- 从 MySQL schema 转换，用于嵌入式部署
-- ============================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) COMMENT '密码（加密后）',
    api_key VARCHAR(64) COMMENT '用户API密钥（已废弃，使用api_keys表）',
    balance DECIMAL(10, 4) DEFAULT 0.0000 COMMENT '账户余额（元）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 2. API密钥表
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '密钥ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    key_name VARCHAR(100) NOT NULL COMMENT '密钥名称',
    api_key VARCHAR(64) NOT NULL UNIQUE COMMENT 'API密钥',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_api_key ON api_keys(api_key);
CREATE INDEX IF NOT EXISTS idx_api_keys_status ON api_keys(status);

-- 3. 充值订单表
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_recharge_orders_user_id ON recharge_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_recharge_orders_order_no ON recharge_orders(order_no);
CREATE INDEX IF NOT EXISTS idx_recharge_orders_status ON recharge_orders(status);
CREATE INDEX IF NOT EXISTS idx_recharge_orders_created_at ON recharge_orders(created_at);

-- 4. 余额变动日志表
CREATE TABLE IF NOT EXISTS balance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10, 4) NOT NULL COMMENT '变动金额',
    balance_after DECIMAL(10, 4) NOT NULL COMMENT '变动后余额',
    type VARCHAR(20) NOT NULL COMMENT '类型：recharge-充值，consume-消费',
    related_id BIGINT COMMENT '关联ID',
    remark VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_balance_log_user_id ON balance_log(user_id);
CREATE INDEX IF NOT EXISTS idx_balance_log_type ON balance_log(type);
CREATE INDEX IF NOT EXISTS idx_balance_log_created_at ON balance_log(created_at);

-- 5. API调用日志表
CREATE TABLE IF NOT EXISTS api_calls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '调用ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    api_key VARCHAR(64) NOT NULL COMMENT 'API Key',
    model VARCHAR(50) NOT NULL COMMENT '模型名称',
    backend_account_id BIGINT COMMENT '使用的后端账户ID',
    provider VARCHAR(50) COMMENT '提供商',
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
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_api_calls_user_id ON api_calls(user_id);
CREATE INDEX IF NOT EXISTS idx_api_calls_api_key ON api_calls(api_key);
CREATE INDEX IF NOT EXISTS idx_api_calls_model ON api_calls(model);
CREATE INDEX IF NOT EXISTS idx_api_calls_status ON api_calls(status);
CREATE INDEX IF NOT EXISTS idx_api_calls_created_at ON api_calls(created_at);
CREATE INDEX IF NOT EXISTS idx_api_calls_provider ON api_calls(provider);
CREATE INDEX IF NOT EXISTS idx_api_calls_user_created ON api_calls(user_id, created_at);

-- 6. 模型配置表
CREATE TABLE IF NOT EXISTS models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型ID',
    model_name VARCHAR(50) NOT NULL UNIQUE COMMENT '模型名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    provider VARCHAR(50) COMMENT '提供商',
    price_multiplier DECIMAL(5, 2) DEFAULT 1.00 COMMENT '价格倍率',
    input_token_price DECIMAL(20, 10) NULL COMMENT '输入token价格',
    output_token_price DECIMAL(20, 10) NULL COMMENT '输出token价格',
    cache_read_token_price DECIMAL(20, 10) NULL COMMENT '缓存读取token价格',
    cache_write_token_price DECIMAL(20, 10) NULL COMMENT '缓存写入token价格',
    supports_streaming BOOLEAN DEFAULT TRUE COMMENT '是否支持流式输出',
    max_tokens INT DEFAULT 4096 COMMENT '最大token数',
    max_context_tokens INT DEFAULT 128000 COMMENT '最大上下文Token数',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    description TEXT COMMENT '模型说明',
    tags TEXT COMMENT '模型标签列表(JSON)',
    sort_order INT DEFAULT 0 COMMENT '排序值',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);
CREATE INDEX IF NOT EXISTS idx_models_provider ON models(provider);
CREATE INDEX IF NOT EXISTS idx_models_status ON models(status);

-- 7. 订阅套餐表
CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '套餐ID',
    plan_name VARCHAR(50) NOT NULL UNIQUE COMMENT '套餐标识名称',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    description TEXT COMMENT '套餐说明',
    original_price DECIMAL(10, 2) NOT NULL COMMENT '原价（元）',
    price DECIMAL(10, 2) NOT NULL COMMENT '现价（元）',
    quota_amount DECIMAL(10, 2) NOT NULL COMMENT '额度金额（元）',
    features TEXT COMMENT '功能特性列表(JSON)',
    color_theme VARCHAR(20) DEFAULT 'blue' COMMENT '颜色主题',
    badge_text VARCHAR(50) COMMENT '徽章文字',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);
CREATE INDEX IF NOT EXISTS idx_subscription_plans_status ON subscription_plans(status);

-- 8. 订阅记录表
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订阅ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_id BIGINT NOT NULL COMMENT '套餐ID',
    plan_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额（元）',
    quota_amount DECIMAL(10, 2) NOT NULL COMMENT '获得额度（元）',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);

-- 9. 工单表
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subject VARCHAR(200) NOT NULL COMMENT '主题',
    content TEXT NOT NULL COMMENT '内容',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    priority VARCHAR(20) DEFAULT 'normal' COMMENT '优先级',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_tickets_user_id ON tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);

-- 10. 工单消息表
CREATE TABLE IF NOT EXISTS ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    user_id BIGINT COMMENT '用户ID',
    admin_id BIGINT COMMENT '管理员ID',
    message TEXT NOT NULL COMMENT '消息内容',
    is_staff TINYINT DEFAULT 0 COMMENT '是否管理员消息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);

-- 11. 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 12. 管理员表
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    role VARCHAR(20) DEFAULT 'admin' COMMENT '角色',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 13. 后端账户配置表
CREATE TABLE IF NOT EXISTS backend_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    provider VARCHAR(20) NOT NULL COMMENT '提供商类型',
    access_token TEXT NOT NULL COMMENT '加密后的访问令牌',
    priority INT DEFAULT 1 COMMENT '优先级',
    status VARCHAR(20) DEFAULT 'active' COMMENT '账户状态',
    max_concurrent INT DEFAULT 10 COMMENT '最大并发请求数',
    rate_limit_per_minute INT DEFAULT 60 COMMENT '每分钟请求限制',
    cost_multiplier DECIMAL(10,4) DEFAULT 1.0000 COMMENT '成本倍率',
    error_count INT DEFAULT 0 COMMENT '连续错误次数',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    last_error_at TIMESTAMP COMMENT '最后错误时间',
    last_error_message TEXT COMMENT '最后错误信息',
    metadata TEXT COMMENT '扩展信息(JSON)',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);
CREATE INDEX IF NOT EXISTS idx_backend_accounts_provider_status ON backend_accounts(provider, status);
CREATE INDEX IF NOT EXISTS idx_backend_accounts_priority ON backend_accounts(priority);

-- 14. 用户账户绑定表
CREATE TABLE IF NOT EXISTS user_account_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    api_key_id BIGINT COMMENT 'API密钥ID',
    backend_account_id BIGINT NOT NULL COMMENT '后端账户ID',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认账户',
    binding_type VARCHAR(20) DEFAULT 'user' COMMENT '绑定类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE,
    FOREIGN KEY (backend_account_id) REFERENCES backend_accounts(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_account UNIQUE (user_id, backend_account_id)
);
CREATE INDEX IF NOT EXISTS idx_user_account_bindings_user_id ON user_account_bindings(user_id);
CREATE INDEX IF NOT EXISTS idx_user_account_bindings_api_key_id ON user_account_bindings(api_key_id);

-- 15. 会话粘性映射表
CREATE TABLE IF NOT EXISTS session_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_hash VARCHAR(64) NOT NULL COMMENT '会话哈希',
    backend_account_id BIGINT NOT NULL COMMENT '绑定的后端账户ID',
    api_key_id BIGINT NOT NULL COMMENT 'API密钥ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    request_count INT DEFAULT 1 COMMENT '该会话请求次数',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    CONSTRAINT uk_session UNIQUE (session_hash),
    FOREIGN KEY (backend_account_id) REFERENCES backend_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_session_mappings_expires ON session_mappings(expires_at);
CREATE INDEX IF NOT EXISTS idx_session_mappings_account ON session_mappings(backend_account_id);
CREATE INDEX IF NOT EXISTS idx_session_mappings_user ON session_mappings(user_id);

-- 16. 用户配额管理表
CREATE TABLE IF NOT EXISTS user_quotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    quota_type VARCHAR(20) NOT NULL COMMENT '配额类型',
    quota_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '配额金额（元）',
    used_amount DECIMAL(20,2) DEFAULT 0.00 COMMENT '已使用金额（元）',
    reset_at TIMESTAMP NOT NULL COMMENT '下次重置时间',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用配额限制',
    alert_threshold DECIMAL(5,2) DEFAULT 80.00 COMMENT '告警阈值（百分比）',
    last_alert_at TIMESTAMP COMMENT '最后告警时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_quota_type UNIQUE (user_id, quota_type)
);
CREATE INDEX IF NOT EXISTS idx_user_quotas_reset_at ON user_quotas(reset_at);

-- 17. 视图：用户统计视图
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
-- 默认数据 - 模型配置 (MERGE INTO 实现幂等)
-- ============================================
MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5.1', 'GPT-5.1', 'OpenAI', 1.00, 8.00, 25.00, 128000, 1, 'OpenAI GPT-5.1 模型 - 最新旗舰模型', '["推荐", "最新"]', 10);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5.1-codex', 'GPT-5.1 Codex', 'OpenAI', 0.90, 10.00, 25.00, 128000, 1, 'OpenAI GPT-5.1 Codex 最新编程模型', '["推荐"]', 20);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5.1-codex-mini', 'GPT-5.1 Codex Mini', 'OpenAI', 0.30, 5.00, 20.00, 128000, 1, 'OpenAI GPT-5.1 Codex Mini - 经济高效', '["推荐", "低价"]', 30);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5.1-codex-max', 'GPT-5.1 Codex Max', 'OpenAI', 1.50, 15.00, 40.00, 128000, 1, 'OpenAI GPT-5.1 Codex Max - 超强编程', '["高级"]', 40);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5', 'GPT-5', 'OpenAI', 0.80, 6.00, 20.00, 128000, 1, 'OpenAI GPT-5 模型', '[]', 50);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5-mini', 'GPT-5 Mini', 'OpenAI', 0.30, 3.00, 10.00, 128000, 1, 'OpenAI GPT-5 Mini - 轻量快速', '["低价"]', 60);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gpt-5-codex', 'GPT-5 Codex', 'OpenAI', 0.70, 8.00, 22.00, 128000, 1, 'OpenAI GPT-5 Codex 编程模型', '[]', 70);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('claude-opus-4.5', 'Claude Opus 4.5', 'Anthropic', 2.00, 15.00, 75.00, 128000, 1, 'Anthropic Claude Opus 4.5 - 最强模型', '["高级"]', 80);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('claude-opus-41', 'Claude Opus 4.1', 'Anthropic', 1.80, 12.00, 60.00, 128000, 1, 'Anthropic Claude Opus 4.1', '["高级"]', 85);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('claude-sonnet-4.5', 'Claude Sonnet 4.5', 'Anthropic', 1.20, 10.00, 30.00, 128000, 1, 'Anthropic Claude Sonnet 4.5 编程模型', '["推荐"]', 90);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('claude-sonnet-4', 'Claude Sonnet 4', 'Anthropic', 1.00, 8.00, 24.00, 128000, 1, 'Anthropic Claude Sonnet 4', '[]', 100);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('claude-haiku-4.5', 'Claude Haiku 4.5', 'Anthropic', 0.30, 3.00, 10.00, 128000, 1, 'Anthropic Claude Haiku 4.5 - 快速轻量', '["低价"]', 110);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gemini-2.5-pro', 'Gemini 2.5 Pro', 'Google', 0.30, 7.00, 21.00, 128000, 1, 'Google Gemini 2.5 Pro', '[]', 120);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('gemini-3-pro-preview', 'Gemini 3.0 Pro Preview', 'Google', 0.80, 10.00, 30.00, 128000, 1, 'Google Gemini 3.0 Pro 预览版 - 最强编程', '["推荐", "最新"]', 130);

MERGE INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, max_context_tokens, status, description, tags, sort_order) KEY(model_name) VALUES
('grok-code-fast-1', 'Grok Code Fast', 'xAI', 0.50, 5.00, 15.00, 128000, 1, 'xAI Grok Code Fast - 快速编程模型', '[]', 140);

-- 默认数据 - 订阅套餐
MERGE INTO subscription_plans (plan_name, display_name, description, original_price, price, quota_amount, features, color_theme, badge_text, sort_order) KEY(plan_name) VALUES
('trial_card', '体验卡', '适合新用户体验', 9.90, 4.90, 10.00, '["1天有效期", "10元额度", "支持所有模型", "基础技术支持"]', 'green', '新用户专享', 1);

MERGE INTO subscription_plans (plan_name, display_name, description, original_price, price, quota_amount, features, color_theme, badge_text, sort_order) KEY(plan_name) VALUES
('max_100', 'Max 100', '适合轻度使用', 188.00, 98.00, 150.00, '["30天有效期", "150元额度", "支持所有模型", "优先技术支持", "API使用统计"]', 'blue', '推荐套餐', 2);

MERGE INTO subscription_plans (plan_name, display_name, description, original_price, price, quota_amount, features, color_theme, badge_text, sort_order) KEY(plan_name) VALUES
('max_200', 'Max 200', '适合中度使用', 700.00, 360.00, 600.00, '["30天有效期", "600元额度", "支持所有模型", "优先技术支持", "API使用统计", "专属客服"]', 'pink', '高级套餐', 3);

-- 默认数据 - 系统配置
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('copilot_api_url', 'http://localhost:4141', 'Copilot API 代理地址');
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('copilot_github_token', '', 'GitHub Token');
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('input_token_price', '4.1', '输入token价格（元/百万）');
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('output_token_price', '16.4', '输出token价格（元/百万）');
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('rate_limit_per_minute', '60', '每分钟请求限制');
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('daily_free_quota', '0', '每日免费额度（元）');
MERGE INTO system_config (config_key, config_value, description) KEY(config_key) VALUES
('system_notice', '欢迎使用 AI API Platform', '系统公告');

-- 默认数据 - 管理员账户 (密码: admin123, BCrypt加密)
MERGE INTO admins (username, password, role, status) KEY(username) VALUES
('admin', '$2b$10$89wowyqpCvaZI74KBGHFSOkXAT8BEDyHBMEvUn0BM6hIly7rvzlUq', 'super_admin', 1);

-- 默认数据 - 后端账户（占位符，需要替换真实token）
MERGE INTO backend_accounts (account_name, provider, access_token, priority, status, max_concurrent, rate_limit_per_minute, metadata) KEY(account_name) VALUES
('Copilot-Primary', 'copilot', 'ENCRYPTED_TOKEN_PLACEHOLDER_1', 1, 'active', 20, 100, '{"region": "global", "description": "主要 Copilot 账户"}');
