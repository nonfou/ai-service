-- ============================================
-- AI API Platform - 升级脚本
-- 版本: 2.1.0
-- 创建时间: 2025-12-13
-- 说明:
--   1. 将模型 provider 从 'Azure OpenAI' 统一改为 'OpenAI'
--   2. 删除 GPT-4 系列、GPT-3.5 系列、VS Code 专用模型
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;

-- ============================================
-- 1. 更新 models 表中的 provider
-- ============================================
-- 将所有 'Azure OpenAI' 改为 'OpenAI'
UPDATE models
SET provider = 'OpenAI',
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'Azure OpenAI';

-- ============================================
-- 2. 删除 GPT-4 系列模型
-- ============================================
DELETE FROM models WHERE model_name IN (
    'gpt-4.1',
    'gpt-4.1-2025-04-14',
    'gpt-41-copilot',
    'gpt-4o',
    'gpt-4o-mini',
    'gpt-4o-2024-05-13',
    'gpt-4o-2024-08-06',
    'gpt-4o-2024-11-20',
    'gpt-4o-mini-2024-07-18',
    'gpt-4-o-preview',
    'gpt-4',
    'gpt-4-0613',
    'gpt-4-0125-preview'
);

-- ============================================
-- 3. 删除 GPT-3.5 系列模型
-- ============================================
DELETE FROM models WHERE model_name IN (
    'gpt-3.5-turbo',
    'gpt-3.5-turbo-0613'
);

-- ============================================
-- 4. 删除 VS Code 专用模型
-- ============================================
DELETE FROM models WHERE model_name IN (
    'oswe-vscode-prime',
    'oswe-vscode-secondary'
);

-- ============================================
-- 5. 更新 api_calls 表中的 provider（如果有历史数据）
-- ============================================
UPDATE api_calls
SET provider = 'OpenAI'
WHERE provider = 'Azure OpenAI';

-- ============================================
-- 6. 查看更新结果
-- ============================================
SELECT '✅ 升级完成！' AS message;

SELECT
    provider AS '提供商',
    COUNT(*) AS '模型数量'
FROM models
GROUP BY provider
ORDER BY COUNT(*) DESC;

SELECT
    '📊 当前模型列表：' AS info;

SELECT
    model_name AS '模型名称',
    display_name AS '显示名称',
    provider AS '提供商',
    status AS '状态'
FROM models
ORDER BY sort_order;
