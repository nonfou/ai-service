-- Stripe 支付字段迁移
-- 执行时间: 2024-xx-xx

ALTER TABLE recharge_orders ADD COLUMN payment_intent_id VARCHAR(255) COMMENT 'Stripe PaymentIntent ID';
CREATE INDEX idx_recharge_orders_payment_intent ON recharge_orders(payment_intent_id);
