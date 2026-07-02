CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    login_id    VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE group_members (
    group_id   BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members(user_id);

-- 초기 Site Admin 계정 (비밀번호: Admin1234! → bcrypt)
INSERT INTO users (login_id, name, email, password, role)
VALUES ('admin', 'Site Admin', 'admin@company.com',
        '$2a$10$emFjSKuytOxWelbOlkasgu5sxib.AUTQ4OlorXsYp.4zTRzf8bLXO', 'SITE_ADMIN');
