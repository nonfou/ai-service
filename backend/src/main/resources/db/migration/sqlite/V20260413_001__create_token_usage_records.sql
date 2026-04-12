CREATE TABLE IF NOT EXISTS token_usage_records (
    id INTEGER PRIMARY KEY,
    api_key_id INTEGER NOT NULL,
    api_key_name_snapshot TEXT NOT NULL,
    endpoint TEXT NOT NULL,
    model TEXT NOT NULL,
    request_type TEXT NOT NULL,
    success INTEGER NOT NULL DEFAULT 1,
    input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    cache_read_tokens INTEGER NOT NULL DEFAULT 0,
    cache_write_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    first_token_latency_ms INTEGER,
    duration_ms INTEGER,
    error_message TEXT,
    user_agent TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_token_usage_records_api_key_created_at
    ON token_usage_records(api_key_id, created_at);

CREATE INDEX IF NOT EXISTS idx_token_usage_records_endpoint_created_at
    ON token_usage_records(endpoint, created_at);

CREATE INDEX IF NOT EXISTS idx_token_usage_records_model_created_at
    ON token_usage_records(model, created_at);

CREATE INDEX IF NOT EXISTS idx_token_usage_records_success_created_at
    ON token_usage_records(success, created_at);
