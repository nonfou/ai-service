-- ============================================
-- AI API Platform - 添加 max_context_tokens 字段
-- 版本: 2.1.0
-- 创建时间: 2025-12-15
-- 说明: 添加最大上下文 Token 限制字段，用于请求预检测
-- ============================================

-- 添加 max_context_tokens 字段（MySQL 兼容写法）
-- 如果字段已存在会报错，可忽略
ALTER TABLE models
ADD COLUMN max_context_tokens INT DEFAULT 128000 COMMENT '最大上下文Token数（输入+输出），用于请求预检测' AFTER max_tokens;

-- 更新现有模型的 max_context_tokens 值
-- Claude 系列模型使用 200k 上下文
UPDATE models SET max_context_tokens = 200000 WHERE model_name LIKE 'claude-%';

-- GPT-5.1 系列使用 128k 上下文
UPDATE models SET max_context_tokens = 128000 WHERE model_name LIKE 'gpt-5.1%';

-- GPT-5 系列使用 128k 上下文
UPDATE models SET max_context_tokens = 128000 WHERE model_name LIKE 'gpt-5%' AND model_name NOT LIKE 'gpt-5.1%';

-- 验证更新结果
SELECT model_name, display_name, max_context_tokens FROM models ORDER BY sort_order;

SELECT '✅ max_context_tokens 字段添加完成！' AS message;
