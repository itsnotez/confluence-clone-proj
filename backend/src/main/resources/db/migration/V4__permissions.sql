CREATE TABLE space_permissions (
    id               BIGSERIAL PRIMARY KEY,
    space_id         BIGINT      NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    subject_type     VARCHAR(20) NOT NULL,
    subject_id       BIGINT,
    permission_level VARCHAR(30) NOT NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (space_id, subject_type, subject_id)
);

CREATE TABLE content_permissions (
    id               BIGSERIAL PRIMARY KEY,
    content_id       BIGINT      NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    subject_type     VARCHAR(20) NOT NULL,
    subject_id       BIGINT,
    permission_level VARCHAR(30) NOT NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (content_id, subject_type, subject_id)
);

CREATE INDEX idx_space_perm_space ON space_permissions(space_id);
CREATE INDEX idx_space_perm_subject ON space_permissions(subject_type, subject_id);
CREATE INDEX idx_content_perm_content ON content_permissions(content_id);
