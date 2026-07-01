CREATE TABLE contents (
    id                 BIGSERIAL PRIMARY KEY,
    space_id           BIGINT       NOT NULL REFERENCES spaces(id),
    parent_id          BIGINT       REFERENCES contents(id),
    type               VARCHAR(20)  NOT NULL DEFAULT 'PAGE',
    title              VARCHAR(500) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    current_version_id BIGINT,
    position           INT          NOT NULL DEFAULT 0,
    created_by         BIGINT       NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP
);

CREATE TABLE content_versions (
    id          BIGSERIAL PRIMARY KEY,
    content_id  BIGINT    NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    version_no  INT       NOT NULL,
    body        TEXT      NOT NULL,
    author_id   BIGINT    NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (content_id, version_no)
);

ALTER TABLE contents ADD CONSTRAINT fk_current_version
    FOREIGN KEY (current_version_id) REFERENCES content_versions(id) DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE attachments (
    id           BIGSERIAL PRIMARY KEY,
    content_id   BIGINT       NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    file_name    VARCHAR(500) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    mime_type    VARCHAR(200),
    size_bytes   BIGINT       NOT NULL DEFAULT 0,
    version      INT          NOT NULL DEFAULT 1,
    uploaded_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE labels (
    id         BIGSERIAL PRIMARY KEY,
    space_id   BIGINT       REFERENCES spaces(id),
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20),
    UNIQUE (space_id, name)
);

CREATE TABLE content_labels (
    content_id BIGINT NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    label_id   BIGINT NOT NULL REFERENCES labels(id)   ON DELETE CASCADE,
    PRIMARY KEY (content_id, label_id)
);

CREATE TABLE comments (
    id                BIGSERIAL PRIMARY KEY,
    content_id        BIGINT    NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    parent_comment_id BIGINT    REFERENCES comments(id),
    body              TEXT      NOT NULL,
    author_id         BIGINT    NOT NULL REFERENCES users(id),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP
);

CREATE INDEX idx_contents_space ON contents(space_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contents_parent ON contents(parent_id);
CREATE INDEX idx_content_versions_content ON content_versions(content_id);
CREATE INDEX idx_attachments_content ON attachments(content_id);
CREATE INDEX idx_comments_content ON comments(content_id) WHERE deleted_at IS NULL;
