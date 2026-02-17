CREATE TABLE gmail_attachment (

    id BIGSERIAL PRIMARY KEY,

    fk_gmail_inbox_id BIGINT NOT NULL,

    -- gmail metadata
    gmail_attachment_id VARCHAR(255),
    file_name VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255),
    size_in_bytes BIGINT,

    -- storage
    storage_path VARCHAR(1000) NOT NULL,
    storage_provider VARCHAR(100) NOT NULL DEFAULT 'S3',

    -- processing
    is_inline BOOLEAN NOT NULL DEFAULT FALSE,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,

    -- audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_gmail_gmail_attachment_inbox
            FOREIGN KEY (fk_gmail_inbox_id)
            REFERENCES gmail_inbox(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_inbox_attachment ON gmail_attachment(fk_gmail_inbox_id);