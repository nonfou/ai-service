PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL UNIQUE,
    password TEXT,
    api_key TEXT,
    balance NUMERIC NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

CREATE TABLE IF NOT EXISTS api_keys (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    key_name TEXT NOT NULL,
    api_key TEXT NOT NULL UNIQUE,
    relay_base_url TEXT,
    upstream_api_key TEXT,
    description TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    last_used_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_api_key ON api_keys(api_key);
CREATE INDEX IF NOT EXISTS idx_api_keys_status ON api_keys(status);

CREATE TABLE IF NOT EXISTS admins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'admin',
    status INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key TEXT NOT NULL UNIQUE,
    config_value TEXT,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

INSERT OR IGNORE INTO admins (username, password, role, status)
VALUES ('admin', '$2b$10$89wowyqpCvaZI74KBGHFSOkXAT8BEDyHBMEvUn0BM6hIly7rvzlUq', 'super_admin', 1);

INSERT OR IGNORE INTO users (email, password, balance, status)
VALUES ('system@local', '', 0, 1);

INSERT OR IGNORE INTO system_config (config_key, config_value, description)
VALUES ('copilot_api_url', '', 'Copilot API 代理地址');

INSERT OR IGNORE INTO system_config (config_key, config_value, description)
VALUES ('copilot_github_token', '', 'GitHub Token');

CREATE INDEX IF NOT EXISTS idx_token_usage_records_api_key_created_at ON token_usage_records(api_key_id, created_at);
CREATE INDEX IF NOT EXISTS idx_token_usage_records_endpoint_created_at ON token_usage_records(endpoint, created_at);
CREATE INDEX IF NOT EXISTS idx_token_usage_records_model_created_at ON token_usage_records(model, created_at);
CREATE INDEX IF NOT EXISTS idx_token_usage_records_success_created_at ON token_usage_records(success, created_at);
