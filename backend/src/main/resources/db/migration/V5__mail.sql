CREATE TABLE mail_accounts (
    id               BIGSERIAL PRIMARY KEY,
    space_id         BIGINT       NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    email_address    VARCHAR(300) NOT NULL,
    imap_host        VARCHAR(500) NOT NULL,
    imap_port        INT          NOT NULL DEFAULT 993,
    imap_ssl         BOOLEAN      NOT NULL DEFAULT TRUE,
    smtp_host        VARCHAR(500),
    smtp_port        INT          DEFAULT 587,
    credential       TEXT         NOT NULL,
    sync_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    last_synced_at   TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE mail_messages (
    id                BIGSERIAL PRIMARY KEY,
    mail_account_id   BIGINT       NOT NULL REFERENCES mail_accounts(id) ON DELETE CASCADE,
    message_uid       VARCHAR(500) NOT NULL,
    thread_id         VARCHAR(500),
    subject           VARCHAR(1000),
    sender            VARCHAR(500),
    recipients        TEXT,
    received_at       TIMESTAMP,
    body_text         TEXT,
    body_html         TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'UNREAD',
    linked_content_id BIGINT       REFERENCES contents(id),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (mail_account_id, message_uid)
);

CREATE TABLE mail_attachments (
    id              BIGSERIAL PRIMARY KEY,
    mail_message_id BIGINT       NOT NULL REFERENCES mail_messages(id) ON DELETE CASCADE,
    file_name       VARCHAR(500) NOT NULL,
    storage_path    VARCHAR(1000),
    mime_type       VARCHAR(200),
    size_bytes      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_mail_messages_account ON mail_messages(mail_account_id);
CREATE INDEX idx_mail_messages_status ON mail_messages(status);
CREATE INDEX idx_mail_messages_received ON mail_messages(received_at DESC);
