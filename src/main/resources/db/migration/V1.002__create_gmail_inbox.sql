CREATE TABLE gmail_inbox (

    id BIGSERIAL PRIMARY KEY,

    mailbox_id BIGINT NOT NULL,

    -- vendor identifiers
    vendor_message_id VARCHAR(128) NOT NULL,
    vendor_thread_id VARCHAR(128) NOT NULL,
    parent_vendor_message_id VARCHAR(128),

    -- content
    subject TEXT,
    from_address VARCHAR(255),
    body_text TEXT,

    sent_at TIMESTAMP,
    received_at TIMESTAMP,

    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_email_inbox_mailbox
        FOREIGN KEY (mailbox_id)
        REFERENCES gmail_mailbox(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_inbox_message
        UNIQUE (mailbox_id, vendor_message_id)
);


CREATE INDEX idx_inbox_mailbox
    ON gmail_inbox (mailbox_id);

CREATE INDEX idx_inbox_thread
    ON gmail_inbox (vendor_thread_id);

CREATE INDEX idx_inbox_parent
    ON gmail_inbox (parent_vendor_message_id);

