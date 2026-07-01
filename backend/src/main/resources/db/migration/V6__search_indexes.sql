ALTER TABLE contents ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A')
    ) STORED;

CREATE TABLE content_search_bodies (
    content_id    BIGINT PRIMARY KEY REFERENCES contents(id) ON DELETE CASCADE,
    search_vector tsvector NOT NULL
);

CREATE INDEX idx_contents_search ON contents USING gin(search_vector);
CREATE INDEX idx_content_search_bodies ON content_search_bodies USING gin(search_vector);

CREATE TABLE mail_message_search (
    mail_message_id BIGINT PRIMARY KEY REFERENCES mail_messages(id) ON DELETE CASCADE,
    search_vector   tsvector NOT NULL
);

CREATE INDEX idx_mail_message_search ON mail_message_search USING gin(search_vector);
