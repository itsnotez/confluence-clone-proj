CREATE TABLE mail_message_attachments (
    id              BIGSERIAL PRIMARY KEY,
    mail_message_id BIGINT        NOT NULL REFERENCES mail_messages(id) ON DELETE CASCADE,
    file_name       VARCHAR(500)  NOT NULL,
    content_type    VARCHAR(200),
    file_size       BIGINT,
    file_data       BYTEA         NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mail_attachments_message_id ON mail_message_attachments(mail_message_id);
