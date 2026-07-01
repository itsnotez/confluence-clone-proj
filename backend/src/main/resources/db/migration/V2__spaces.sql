CREATE TABLE spaces (
    id          BIGSERIAL PRIMARY KEY,
    space_key   VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(300) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    icon_emoji  VARCHAR(10),
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE TABLE space_favorites (
    space_id   BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (space_id, user_id)
);

CREATE INDEX idx_spaces_status ON spaces(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_spaces_created_by ON spaces(created_by);
