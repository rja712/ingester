CREATE TABLE gmail_mailbox (
    id BIGSERIAL PRIMARY KEY,

    -- identity
    email_address VARCHAR(255) NOT NULL UNIQUE,

    -- oauth (gmail-specific)
    access_token TEXT,
    refresh_token TEXT NOT NULL,
    access_token_expires_at TIMESTAMP,

    -- gmail sync
    history_id BIGINT NOT NULL,
    watch_expires_at BIGINT,

    -- system-level (⚠️ keep this MINIMAL)
    sync_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_synced_at TIMESTAMP,
    last_sync_error TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at_
