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

    -- system-level (keep minimal)
    sync_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_synced_at TIMESTAMP,
    last_sync_error TEXT,

    -- audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);


CREATE INDEX idx_gmail_mailbox_sync_status
    ON gmail_mailbox (sync_status);

CREATE INDEX idx_gmail_mailbox_watch_expires_at
    ON gmail_mailbox (watch_expires_at);

