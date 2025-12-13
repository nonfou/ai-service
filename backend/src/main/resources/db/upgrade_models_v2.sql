-- ============================================
-- AI API Platform - 模型配置升级脚本
-- 版本: 2.1.0
-- 创建时间: 2025-12-13
-- 说明: 根据 /v1/models API 返回结果更新模型配置，去除 embedding/vscode/gpt-3.5 模型
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 删除不需要的模型（embedding/vscode/gpt-3.5）
-- ============================================
DELETE FROM models WHERE model_name LIKE '%embedding%';
DELETE FROM models WHERE model_name LIKE '%vscode%';
DELETE FROM models WHERE model_name LIKE 'gpt-3.5%';

-- ============================================
-- 2. 插入/更新所有可用模型
-- ============================================
INSERT INTO models (model_name, display_name, provider, price_multiplier, input_token_price, output_token_price, status, description, tags, sort_order) VALUES
-- OpenAI GPT-5.1 系列 (最新)
('gpt-5.1', 'GPT-5.1', 'OpenAI', 1.00, 8.00, 25.00, 1, 'OpenAI GPT-5.1 模型 - 最新旗舰模型', JSON_ARRAY('推荐', '最新'), 10),
('gpt-5.1-codex', 'GPT-5.1 Codex', 'OpenAI', 0.90, 10.00, 25.00, 1, 'OpenAI GPT-5.1 Codex 最新编程模型', JSON_ARRAY('推荐'), 20),
('gpt-5.1-codex-mini', 'GPT-5.1 Codex Mini', 'OpenAI', 0.30, 5.00, 20.00, 1, 'OpenAI GPT-5.1 Codex Mini - 经济高效', JSON_ARRAY('推荐', '低价'), 30),
('gpt-5.1-codex-max', 'GPT-5.1 Codex Max', 'OpenAI', 1.50, 15.00, 40.00, 1, 'OpenAI GPT-5.1 Codex Max - 超强编程', JSON_ARRAY('高级'), 40),

-- OpenAI GPT-5 系列
('gpt-5', 'GPT-5', 'Azure OpenAI', 0.80, 6.00, 20.00, 1, 'OpenAI GPT-5 模型', JSON_ARRAY(), 50),
('gpt-5-mini', 'GPT-5 Mini', 'Azure OpenAI', 0.30, 3.00, 10.00, 1, 'OpenAI GPT-5 Mini - 轻量快速', JSON_ARRAY('低价'), 60),
('gpt-5-codex', 'GPT-5 Codex', 'OpenAI', 0.70, 8.00, 22.00, 1, 'OpenAI GPT-5 Codex 编程模型', JSON_ARRAY(), 70),

-- Claude 系列
('claude-opus-4.5', 'Claude Opus 4.5', 'Anthropic', 2.00, 15.00, 75.00, 1, 'Anthropic Claude Opus 4.5 - 最强模型', JSON_ARRAY('高级'), 80),
('claude-sonnet-4.5', 'Claude Sonnet 4.5', 'Anthropic', 1.20, 10.00, 30.00, 1, 'Anthropic Claude Sonnet 4.5 编程模型', JSON_ARRAY('推荐'), 90),
('claude-sonnet-4', 'Claude Sonnet 4', 'Anthropic', 1.00, 8.00, 24.00, 1, 'Anthropic Claude Sonnet 4', JSON_ARRAY(), 100),
('claude-opus-41', 'Claude Opus 4.1', 'Anthropic', 1.80, 12.00, 60.00, 1, 'Anthropic Claude Opus 4.1', JSON_ARRAY('高级'), 85),
('claude-haiku-4.5', 'Claude Haiku 4.5', 'Anthropic', 0.30, 3.00, 10.00, 1, 'Anthropic Claude Haiku 4.5 - 快速轻量', JSON_ARRAY('低价'), 110),

-- Google Gemini 系列
('gemini-2.5-pro', 'Gemini 2.5 Pro', 'Google', 0.30, 7.00, 21.00, 1, 'Google Gemini 2.5 Pro', JSON_ARRAY(), 120),
('gemini-3-pro-preview', 'Gemini 3.0 Pro Preview', 'Google', 0.80, 10.00, 30.00, 1, 'Google Gemini 3.0 Pro 预览版 - 最强编程', JSON_ARRAY('推荐', '最新'), 130),

-- xAI Grok 系列
('grok-code-fast-1', 'Grok Code Fast', 'xAI', 0.50, 5.00, 15.00, 1, 'xAI Grok Code Fast - 快速编程模型', JSON_ARRAY(), 140),

-- GPT-4 系列
('gpt-4.1', 'GPT-4.1', 'Azure OpenAI', 0.60, 5.00, 15.00, 1, 'OpenAI GPT-4.1 模型', JSON_ARRAY(), 150),
('gpt-4.1-2025-04-14', 'GPT-4.1 (2025-04-14)', 'Azure OpenAI', 0.60, 5.00, 15.00, 1, 'OpenAI GPT-4.1 指定版本', JSON_ARRAY(), 155),
('gpt-41-copilot', 'GPT-4.1 Copilot', 'Azure OpenAI', 0.60, 5.00, 15.00, 1, 'OpenAI GPT-4.1 Copilot 版本', JSON_ARRAY(), 158),
('gpt-4o', 'GPT-4o', 'Azure OpenAI', 0.50, 5.00, 15.00, 1, 'OpenAI GPT-4o 多模态模型', JSON_ARRAY(), 160),
('gpt-4o-mini', 'GPT-4o Mini', 'Azure OpenAI', 0.15, 0.15, 0.60, 1, 'OpenAI GPT-4o Mini - 超低价', JSON_ARRAY('低价'), 165),
('gpt-4o-2024-05-13', 'GPT-4o (2024-05-13)', 'Azure OpenAI', 0.50, 5.00, 15.00, 1, 'OpenAI GPT-4o 指定版本', JSON_ARRAY(), 170),
('gpt-4o-2024-08-06', 'GPT-4o (2024-08-06)', 'Azure OpenAI', 0.50, 5.00, 15.00, 1, 'OpenAI GPT-4o 指定版本', JSON_ARRAY(), 175),
('gpt-4o-2024-11-20', 'GPT-4o (2024-11-20)', 'Azure OpenAI', 0.50, 5.00, 15.00, 1, 'OpenAI GPT-4o 指定版本', JSON_ARRAY(), 180),
('gpt-4o-mini-2024-07-18', 'GPT-4o Mini (2024-07-18)', 'Azure OpenAI', 0.15, 0.15, 0.60, 1, 'OpenAI GPT-4o Mini 指定版本', JSON_ARRAY('低价'), 185),
('gpt-4-o-preview', 'GPT-4o Preview', 'Azure OpenAI', 0.50, 5.00, 15.00, 1, 'OpenAI GPT-4o 预览版', JSON_ARRAY(), 190),
('gpt-4', 'GPT-4', 'Azure OpenAI', 0.60, 30.00, 60.00, 1, 'OpenAI GPT-4 基础模型', JSON_ARRAY(), 200),
('gpt-4-0613', 'GPT-4 (0613)', 'Azure OpenAI', 0.60, 30.00, 60.00, 1, 'OpenAI GPT-4 指定版本', JSON_ARRAY(), 210),
('gpt-4-0125-preview', 'GPT-4 Turbo Preview', 'Azure OpenAI', 0.40, 10.00, 30.00, 1, 'OpenAI GPT-4 Turbo 预览版', JSON_ARRAY(), 220)

ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    provider = VALUES(provider),
    price_multiplier = VALUES(price_multiplier),
    input_token_price = VALUES(input_token_price),
    output_token_price = VALUES(output_token_price),
    description = VALUES(description),
    tags = VALUES(tags),
    sort_order = VALUES(sort_order);

-- ============================================
-- 3. 验证结果
-- ============================================
SELECT
    '✅ 模型配置升级完成！' AS message,
    (SELECT COUNT(*) FROM models) AS '总模型数',
    (SELECT COUNT(*) FROM models WHERE status = 1) AS '启用模型数';

SELECT model_name, display_name, provider, price_multiplier, status
FROM models
ORDER BY sort_order;
