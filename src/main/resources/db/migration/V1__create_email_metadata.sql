CREATE TABLE email_metadata (
    id BIGSERIAL PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    access_token TEXT,
    refresh_token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
