# 사내 협업 지식관리 시스템 (Confluence Clone) — 마스터 개발계획서

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vue3 + Spring Boot + PostgreSQL 기반의 사내 협업 지식관리 시스템(Confluence 유사) 전체를 Phase 0~5에 걸쳐 구축한다.

**Architecture:** Spring Boot 3.x REST API 서버(모듈형 패키지 구조)와 Vue3 SPA 클라이언트를 분리 운영한다. 인증은 JWT, 권한은 Space/Content 단위 RBAC, 메일 연동은 IMAP Polling, 전문검색은 PostgreSQL tsvector를 사용한다.

**Tech Stack:**
- Frontend: Vue 3 (Composition API), DevExtreme, Pinia, Vue Router 4, Axios
- Backend: Java 21, Spring Boot 3.3, Spring Security 6 (JWT), Spring Data JPA, Spring Batch 5, Spring Mail (JavaMail/IMAP)
- DB: PostgreSQL 16 (Flyway 마이그레이션, tsvector Full-Text Search)
- Storage: MinIO (S3 호환 로컬 Object Storage) — 첨부파일
- Infra: Docker Compose (개발), GitHub Actions CI

---

## 범위 분리 안내

이 마스터 계획서는 **Phase 0(환경 구축)를 완전 상세화**하고, Phase 1~5는 **각 Phase 시작 전 별도 세부 계획서**를 작성하여 승인 후 실행한다.

| Phase | 계획서 파일 | 상태 |
|---|---|---|
| Phase 0 | 이 문서 (Task 1–8) | 상세화 완료 |
| Phase 1 | `2026-06-30-phase1-core.md` | Phase 0 완료 후 작성 |
| Phase 2 | `YYYY-MM-DD-phase2-mail.md` | Phase 1 완료 후 작성 |
| Phase 3 | `YYYY-MM-DD-phase3-search-comments.md` | Phase 2 완료 후 작성 |
| Phase 4 | `YYYY-MM-DD-phase4-admin-notifications.md` | Phase 3 완료 후 작성 |
| Phase 5 | `YYYY-MM-DD-phase5-qa-hardening.md` | Phase 4 완료 후 작성 |

---

## 전체 프로젝트 파일 구조

```
confluence-clone-proj/
├── backend/                                   # Spring Boot 멀티모듈 프로젝트
│   ├── pom.xml                                # 루트 POM
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/company/wiki/
│   │   │   │   ├── WikiApplication.java
│   │   │   │   ├── common/
│   │   │   │   │   ├── config/               # Security, JPA, S3, Mail 설정
│   │   │   │   │   ├── exception/            # GlobalExceptionHandler, ErrorResponse
│   │   │   │   │   ├── response/             # ApiResponse 래퍼
│   │   │   │   │   └── util/                 # AesEncryptUtil, JwtUtil
│   │   │   │   ├── auth/
│   │   │   │   │   ├── controller/           # AuthController (login, refresh, logout)
│   │   │   │   │   ├── service/              # AuthService
│   │   │   │   │   ├── dto/                  # LoginRequest, TokenResponse
│   │   │   │   │   └── security/             # JwtFilter, JwtProvider, UserDetailsImpl
│   │   │   │   ├── user/
│   │   │   │   │   ├── controller/           # UserController (CRUD, me)
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # User, Group, GroupMember
│   │   │   │   │   └── dto/
│   │   │   │   ├── space/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # Space
│   │   │   │   │   └── dto/
│   │   │   │   ├── content/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # Content, ContentVersion, Attachment
│   │   │   │   │   └── dto/
│   │   │   │   ├── permission/
│   │   │   │   │   ├── service/              # PermissionService (권한 판단 핵심)
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # SpacePermission, ContentPermission
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── aop/                  # PermissionCheckAspect
│   │   │   │   ├── mail/
│   │   │   │   │   ├── controller/           # MailAccountController
│   │   │   │   │   ├── service/              # MailSyncService, ImapService
│   │   │   │   │   ├── scheduler/            # MailPollingScheduler
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # MailAccount, MailMessage
│   │   │   │   │   └── dto/
│   │   │   │   ├── search/
│   │   │   │   │   ├── controller/           # SearchController
│   │   │   │   │   ├── service/              # SearchService (tsvector 쿼리)
│   │   │   │   │   └── dto/                  # SearchRequest, SearchResult
│   │   │   │   ├── comment/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # Comment
│   │   │   │   │   └── dto/
│   │   │   │   ├── label/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # Label, ContentLabel
│   │   │   │   │   └── dto/
│   │   │   │   ├── notification/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/               # Notification
│   │   │   │   │   └── dto/
│   │   │   │   └── audit/
│   │   │   │       ├── service/              # AuditLogService
│   │   │   │       ├── repository/
│   │   │   │       └── entity/               # AuditLog
│   │   │   └── resources/
│   │   │       ├── application.yml           # 공통 설정
│   │   │       ├── application-local.yml     # 로컬 개발용
│   │   │       └── db/migration/             # Flyway SQL 파일들
│   │   │           ├── V1__init_schema.sql
│   │   │           ├── V2__space_schema.sql
│   │   │           └── ...
│   │   └── test/java/com/company/wiki/       # 테스트 (모듈별 분리)
├── frontend/                                  # Vue3 프로젝트
│   ├── src/
│   │   ├── main.js
│   │   ├── App.vue
│   │   ├── router/
│   │   │   └── index.js                      # Vue Router 설정
│   │   ├── stores/                            # Pinia stores
│   │   │   ├── auth.js
│   │   │   ├── space.js
│   │   │   ├── content.js
│   │   │   └── notification.js
│   │   ├── api/                               # Axios 모듈별 클라이언트
│   │   │   ├── axios.js                       # 인터셉터, baseURL
│   │   │   ├── auth.js
│   │   │   ├── space.js
│   │   │   ├── content.js
│   │   │   ├── mail.js
│   │   │   └── search.js
│   │   ├── views/
│   │   │   ├── auth/
│   │   │   │   └── LoginView.vue
│   │   │   ├── space/
│   │   │   │   ├── SpaceListView.vue
│   │   │   │   └── SpaceHomeView.vue
│   │   │   ├── content/
│   │   │   │   ├── ContentView.vue
│   │   │   │   └── ContentEditorView.vue
│   │   │   ├── mail/
│   │   │   │   └── MailBoxView.vue
│   │   │   ├── search/
│   │   │   │   └── SearchResultView.vue
│   │   │   └── admin/
│   │   │       └── AdminDashboardView.vue
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   ├── AppHeader.vue
│   │   │   │   ├── SpaceSidebar.vue
│   │   │   │   └── ContentTree.vue
│   │   │   ├── content/
│   │   │   │   ├── TipTapEditor.vue
│   │   │   │   └── VersionHistoryPanel.vue
│   │   │   └── common/
│   │   │       └── PermissionGuard.vue
│   │   └── i18n/
│   │       ├── ko.json
│   │       └── en.json
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
├── docker-compose.yml                         # PostgreSQL + MinIO + Backend + Frontend
├── docker-compose.override.yml                # 로컬 개발용 오버라이드
├── docs/
│   └── superpowers/plans/
│       └── 2026-06-30-confluence-clone-master-plan.md  (이 파일)
└── PRD_협업지식관리시스템.md
```

---

## Phase 0: 프로젝트 기반 구축 (2주)

### Task 1: Docker Compose 환경 구성

**Files:**
- Create: `docker-compose.yml`
- Create: `docker-compose.override.yml`

- [ ] **Step 1: `docker-compose.yml` 작성**

```yaml
# docker-compose.yml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: wiki-postgres
    environment:
      POSTGRES_DB: wikidb
      POSTGRES_USER: wikiuser
      POSTGRES_PASSWORD: wikipass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wikiuser -d wikidb"]
      interval: 5s
      timeout: 5s
      retries: 5

  minio:
    image: minio/minio:latest
    container_name: wiki-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data

volumes:
  postgres-data:
  minio-data:
```

- [ ] **Step 2: 환경 구동 확인**

```bash
cd /Users/shinwon/confluence-clone-proj
docker compose up -d
docker compose ps
# postgres, minio 모두 running 상태 확인
docker compose logs postgres | tail -5
# "database system is ready to accept connections" 확인
```

- [ ] **Step 3: 커밋**

```bash
git init
git add docker-compose.yml
git commit -m "chore: add docker compose for postgres and minio"
```

---

### Task 2: Spring Boot 백엔드 프로젝트 초기화

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-local.yml`
- Create: `backend/src/main/java/com/company/wiki/WikiApplication.java`

- [ ] **Step 1: `backend/pom.xml` 작성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.1</version>
        <relativePath/>
    </parent>

    <groupId>com.company</groupId>
    <artifactId>wiki</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.5</jjwt.version>
        <flyway.version>10.13.0</flyway.version>
        <aws-sdk.version>2.25.60</aws-sdk.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway (PostgreSQL) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- AWS SDK v2 (S3/MinIO) -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>${aws-sdk.version}</version>
        </dependency>

        <!-- Spring Mail (IMAP/SMTP) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- Spring Batch -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-batch</artifactId>
        </dependency>

        <!-- AOP (권한 체크) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: `application.yml` 작성**

```yaml
# backend/src/main/resources/application.yml
spring:
  profiles:
    active: local
  application:
    name: wiki
  datasource:
    url: jdbc:postgresql://localhost:5432/wikidb
    username: wikiuser
    password: wikipass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

jwt:
  secret: ${JWT_SECRET:thisIsAVeryLongSecretKeyForJwtTokenGenerationAtLeast256BitsLong!}
  access-token-expiry-ms: 3600000      # 1시간
  refresh-token-expiry-ms: 604800000   # 7일

storage:
  s3:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: wiki-attachments
    region: us-east-1

server:
  port: 8080
  servlet:
    context-path: /api
```

- [ ] **Step 3: `WikiApplication.java` 작성**

```java
// backend/src/main/java/com/company/wiki/WikiApplication.java
package com.company.wiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WikiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WikiApplication.class, args);
    }
}
```

- [ ] **Step 4: 빌드 확인 (DB 연결 없이 컴파일만)**

```bash
cd backend
./mvnw compile -q
# BUILD SUCCESS 확인
```

- [ ] **Step 5: 커밋**

```bash
git add backend/
git commit -m "chore: initialize Spring Boot backend project"
```

---

### Task 3: Flyway DB 스키마 마이그레이션 작성

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_users_groups.sql`
- Create: `backend/src/main/resources/db/migration/V2__spaces.sql`
- Create: `backend/src/main/resources/db/migration/V3__contents.sql`
- Create: `backend/src/main/resources/db/migration/V4__permissions.sql`
- Create: `backend/src/main/resources/db/migration/V5__mail.sql`
- Create: `backend/src/main/resources/db/migration/V6__search_indexes.sql`
- Create: `backend/src/main/resources/db/migration/V7__audit_notifications.sql`

- [ ] **Step 1: V1 — 사용자 및 그룹**

```sql
-- V1__init_users_groups.sql
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
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SITE_ADMIN');
```

- [ ] **Step 2: V2 — Space**

```sql
-- V2__spaces.sql
CREATE TABLE spaces (
    id          BIGSERIAL PRIMARY KEY,
    space_key   VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(300) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',  -- PUBLIC | PRIVATE
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | ARCHIVED | DELETED
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
```

- [ ] **Step 3: V3 — 콘텐츠**

```sql
-- V3__contents.sql
CREATE TABLE contents (
    id                 BIGSERIAL PRIMARY KEY,
    space_id           BIGINT       NOT NULL REFERENCES spaces(id),
    parent_id          BIGINT       REFERENCES contents(id),
    type               VARCHAR(20)  NOT NULL DEFAULT 'PAGE',  -- PAGE | BLOG | FOLDER
    title              VARCHAR(500) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED', -- DRAFT | PUBLISHED | ARCHIVED
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
    body        TEXT      NOT NULL,  -- TipTap JSON 문자열
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
    space_id   BIGINT       REFERENCES spaces(id),  -- NULL = 전역 라벨
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
    id               BIGSERIAL PRIMARY KEY,
    content_id       BIGINT    NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    parent_comment_id BIGINT   REFERENCES comments(id),
    body             TEXT      NOT NULL,
    author_id        BIGINT    NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP
);

CREATE INDEX idx_contents_space ON contents(space_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contents_parent ON contents(parent_id);
CREATE INDEX idx_content_versions_content ON content_versions(content_id);
CREATE INDEX idx_attachments_content ON attachments(content_id);
CREATE INDEX idx_comments_content ON comments(content_id) WHERE deleted_at IS NULL;
```

- [ ] **Step 4: V4 — 권한**

```sql
-- V4__permissions.sql
CREATE TABLE space_permissions (
    id               BIGSERIAL PRIMARY KEY,
    space_id         BIGINT      NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    subject_type     VARCHAR(20) NOT NULL, -- USER | GROUP | ALL
    subject_id       BIGINT,               -- USER/GROUP ID, ALL이면 NULL
    permission_level VARCHAR(30) NOT NULL, -- SPACE_ADMIN | WRITE | READ | NONE
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
```

- [ ] **Step 5: V5 — 메일**

```sql
-- V5__mail.sql
CREATE TABLE mail_accounts (
    id               BIGSERIAL PRIMARY KEY,
    space_id         BIGINT       NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    email_address    VARCHAR(300) NOT NULL,
    imap_host        VARCHAR(500) NOT NULL,
    imap_port        INT          NOT NULL DEFAULT 993,
    imap_ssl         BOOLEAN      NOT NULL DEFAULT TRUE,
    smtp_host        VARCHAR(500),
    smtp_port        INT          DEFAULT 587,
    credential       TEXT         NOT NULL,  -- AES-256 암호화된 패스워드
    sync_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | ACTIVE | ERROR | DISABLED
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
    status            VARCHAR(20)  NOT NULL DEFAULT 'UNREAD', -- UNREAD | READ | PROCESSED | ARCHIVED
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
```

- [ ] **Step 6: V6 — Full-Text Search 인덱스**

```sql
-- V6__search_indexes.sql
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
```

- [ ] **Step 7: V7 — 감사로그 및 알림**

```sql
-- V7__audit_notifications.sql
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT       NOT NULL REFERENCES users(id),
    action_type VARCHAR(100) NOT NULL,  -- SPACE_CREATE, CONTENT_DELETE, PERMISSION_CHANGE, ...
    target_type VARCHAR(50)  NOT NULL,  -- SPACE | CONTENT | USER | PERMISSION | MAIL
    target_id   BIGINT,
    detail      JSONB,
    is_admin_access BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50) NOT NULL,  -- MENTION | COMMENT | CONTENT_CHANGE | MAIL_RECEIVED
    title       VARCHAR(500) NOT NULL,
    message     TEXT,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    link_url    VARCHAR(1000),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE is_read = FALSE;
```

- [ ] **Step 8: Flyway 마이그레이션 실행**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local &
# 서버 시작 후 로그에서 "Flyway: Successfully applied 7 migrations" 확인
# 서버 종료
kill %1
```

- [ ] **Step 9: 커밋**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add Flyway DB migrations V1-V7 (full schema)"
```

---

### Task 4: 공통 모듈 — 응답 형식, 예외 처리, JWT 유틸

**Files:**
- Create: `backend/src/main/java/com/company/wiki/common/response/ApiResponse.java`
- Create: `backend/src/main/java/com/company/wiki/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/company/wiki/common/exception/BusinessException.java`
- Create: `backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/company/wiki/common/util/JwtProvider.java`

- [ ] **Step 1: `ApiResponse.java` 작성**

```java
package com.company.wiki.common.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

- [ ] **Step 2: `ErrorCode.java` 작성**

```java
package com.company.wiki.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 리소스
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Space를 찾을 수 없습니다."),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),
    MAIL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "메일 계정을 찾을 수 없습니다."),

    // 비즈니스
    DUPLICATE_SPACE_KEY(HttpStatus.CONFLICT, "이미 사용 중인 Space Key입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    ARCHIVED_SPACE(HttpStatus.BAD_REQUEST, "보관된 Space는 수정할 수 없습니다."),

    // 서버
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
```

- [ ] **Step 3: `BusinessException.java` 작성**

```java
package com.company.wiki.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 4: `GlobalExceptionHandler.java` 작성**

```java
package com.company.wiki.common.exception;

import com.company.wiki.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
```

- [ ] **Step 5: `JwtProvider.java` 작성**

```java
package com.company.wiki.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    public String generateAccessToken(Long userId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }
}
```

- [ ] **Step 6: JwtProvider 단위 테스트 작성 및 통과 확인**

파일: `backend/src/test/java/com/company/wiki/common/util/JwtProviderTest.java`

```java
package com.company.wiki.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "thisIsAVeryLongSecretKeyForJwtTokenGenerationAtLeast256BitsLong!",
                3600000L,
                604800000L
        );
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtProvider.generateAccessToken(1L, "MEMBER");
        assertThat(jwtProvider.isValid(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void invalidToken_returnsfalse() {
        assertThat(jwtProvider.isValid("invalid.token.value")).isFalse();
    }
}
```

```bash
cd backend
./mvnw test -Dtest=JwtProviderTest -q
# BUILD SUCCESS 확인
```

- [ ] **Step 7: 커밋**

```bash
git add backend/src/
git commit -m "feat: add common response, exception handling, and JWT provider"
```

---

### Task 5: Spring Security 설정 및 JWT 필터

**Files:**
- Create: `backend/src/main/java/com/company/wiki/auth/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/company/wiki/auth/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/com/company/wiki/common/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/company/wiki/user/entity/User.java`
- Create: `backend/src/main/java/com/company/wiki/user/repository/UserRepository.java`

- [ ] **Step 1: `User.java` 엔티티 작성**

```java
package com.company.wiki.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 100)
    private String loginId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;   // SITE_ADMIN | MEMBER

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE | INACTIVE | DELETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (role == null) role = "MEMBER";
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

- [ ] **Step 2: `UserRepository.java` 작성**

```java
package com.company.wiki.user.repository;

import com.company.wiki.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 3: `UserDetailsServiceImpl.java` 작성**

JWT subject에 userId를 저장하므로, `loadUserByUsername`은 userId(String)로 사용자를 조회한다.
AuthService의 로그인 흐름은 `UserDetailsService`를 거치지 않고 `UserRepository`를 직접 사용한다.

```java
package com.company.wiki.auth.security;

import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * JWT 필터에서 userId(String)로 호출. Spring Security의 username = userId.
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
```

- [ ] **Step 4: `JwtAuthenticationFilter.java` 작성**

```java
package com.company.wiki.auth.security;

import com.company.wiki.common.util.JwtProvider;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtProvider.isValid(token)) {
            Long userId = jwtProvider.getUserId(token);
            var userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));
            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 5: `SecurityConfig.java` 작성**

```java
package com.company.wiki.common.config;

import com.company.wiki.auth.security.JwtAuthenticationFilter;
import com.company.wiki.common.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/admin/**").hasRole("SITE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 6: 서버 기동 및 401 응답 확인**

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local &
sleep 10
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/spaces
# 401 확인
kill %1
```

- [ ] **Step 7: 커밋**

```bash
git add backend/src/
git commit -m "feat: add JWT security filter and Spring Security config"
```

---

### Task 6: Vue3 프론트엔드 프로젝트 초기화

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/api/axios.js`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/auth.js`

- [ ] **Step 1: 프로젝트 생성**

```bash
cd /Users/shinwon/confluence-clone-proj
npm create vite@latest frontend -- --template vue
cd frontend
npm install
npm install pinia vue-router@4 axios
npm install devextreme devextreme-vue
npm install @tiptap/vue-3 @tiptap/starter-kit @tiptap/extension-image @tiptap/extension-link @tiptap/extension-table @tiptap/extension-code-block-lowlight lowlight
npm install vue-i18n@9
```

- [ ] **Step 2: `vite.config.js` 수정**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: `src/api/axios.js` 작성 (인터셉터 포함)**

```js
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  async err => {
    if (err.response?.status === 401) {
      // refresh 시도
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const { data } = await axios.post('/api/auth/refresh', { refreshToken })
          localStorage.setItem('accessToken', data.data.accessToken)
          err.config.headers.Authorization = `Bearer ${data.data.accessToken}`
          return api.request(err.config)
        } catch {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          window.location.href = '/login'
        }
      } else {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default api
```

- [ ] **Step 4: `src/stores/auth.js` 작성 (Pinia)**

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const accessToken = ref(localStorage.getItem('accessToken'))
  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(loginId, password) {
    const { data } = await api.post('/auth/login', { loginId, password })
    accessToken.value = data.data.accessToken
    localStorage.setItem('accessToken', data.data.accessToken)
    localStorage.setItem('refreshToken', data.data.refreshToken)
    user.value = data.data.user
  }

  function logout() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async function fetchMe() {
    const { data } = await api.get('/users/me')
    user.value = data.data
  }

  return { user, accessToken, isLoggedIn, login, logout, fetchMe }
})
```

- [ ] **Step 5: `src/router/index.js` 작성**

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/auth/LoginView.vue'), meta: { public: true } },
  { path: '/', redirect: '/spaces' },
  { path: '/spaces', component: () => import('@/views/space/SpaceListView.vue') },
  { path: '/spaces/:spaceKey', component: () => import('@/views/space/SpaceHomeView.vue') },
  { path: '/spaces/:spaceKey/contents/:contentId', component: () => import('@/views/content/ContentView.vue') },
  { path: '/spaces/:spaceKey/contents/:contentId/edit', component: () => import('@/views/content/ContentEditorView.vue') },
  { path: '/search', component: () => import('@/views/search/SearchResultView.vue') },
  { path: '/admin', component: () => import('@/views/admin/AdminDashboardView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
})

export default router
```

- [ ] **Step 6: `src/main.js` 작성**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// DevExtreme 테마
import 'devextreme/dist/css/dx.material.blue.light.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 7: 프론트엔드 기동 확인**

```bash
cd frontend
npm run dev
# http://localhost:3000 접속하여 빈 Vue 앱 확인 후 Ctrl+C
```

- [ ] **Step 8: 커밋**

```bash
git add frontend/
git commit -m "chore: initialize Vue3 frontend with DevExtreme, Pinia, Router, TipTap"
```

---

### Task 7: 인증 API — 로그인 / 토큰 발급 / 현재 사용자 조회

**Files:**
- Create: `backend/src/main/java/com/company/wiki/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/company/wiki/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/company/wiki/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/company/wiki/auth/dto/TokenResponse.java`
- Create: `backend/src/main/java/com/company/wiki/user/controller/UserController.java`
- Test: `backend/src/test/java/com/company/wiki/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: 테스트 먼저 작성 (TDD)**

```java
// AuthControllerTest.java
package com.company.wiki.auth.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void login_withValidCredentials_returnsTokens() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Admin1234!");
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void login_withInvalidPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest("admin", "wrongpassword");
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd backend
./mvnw test -Dtest=AuthControllerTest -q 2>&1 | tail -5
# FAILED — AuthController not found
```

- [ ] **Step 3: DTO 작성**

```java
// LoginRequest.java
package com.company.wiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank private String loginId;
    @NotBlank private String password;
}

// TokenResponse.java
package com.company.wiki.auth.dto;

import com.company.wiki.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private UserSummary user;

    @Getter @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String role;
    }

    public static TokenResponse of(String accessToken, String refreshToken, User user) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
```

- [ ] **Step 4: `AuthService.java` 작성**

```java
package com.company.wiki.auth.service;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.auth.dto.TokenResponse;
import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.common.util.JwtProvider;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        return TokenResponse.of(accessToken, refreshToken, user);
    }
}
```

- [ ] **Step 5: `AuthController.java` 작성**

```java
package com.company.wiki.auth.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.auth.dto.TokenResponse;
import com.company.wiki.auth.service.AuthService;
import com.company.wiki.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
cd backend
./mvnw test -Dtest=AuthControllerTest -q
# BUILD SUCCESS
```

- [ ] **Step 7: 현재 사용자 조회 API `/users/me` 추가**

```java
// UserController.java
package com.company.wiki.user.controller;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getMe(@AuthenticationPrincipal UserDetails principal) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));
    }
}
```

- [ ] **Step 8: 커밋**

```bash
git add backend/src/
git commit -m "feat: add login API and /users/me endpoint"
```

---

### Task 8: 로그인 화면 (Vue3)

**Files:**
- Create: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/space/SpaceListView.vue` (placeholder)

- [ ] **Step 1: `LoginView.vue` 작성**

```vue
<!-- frontend/src/views/auth/LoginView.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">사내 지식관리 시스템</h1>
      <DxForm
        :form-data="form"
        :col-count="1"
        @field-data-changed="onFieldChanged"
      >
        <DxSimpleItem data-field="loginId" :label="{ text: '아이디' }">
          <DxRequiredRule />
        </DxSimpleItem>
        <DxSimpleItem data-field="password" editor-type="dxTextBox"
          :editor-options="{ mode: 'password' }" :label="{ text: '비밀번호' }">
          <DxRequiredRule />
        </DxSimpleItem>
      </DxForm>
      <DxButton
        text="로그인"
        type="default"
        styling-mode="contained"
        :width="'100%'"
        :use-submit-behavior="false"
        @click="handleLogin"
      />
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DxForm, DxSimpleItem, DxRequiredRule } from 'devextreme-vue/form'
import { DxButton } from 'devextreme-vue/button'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ loginId: '', password: '' })
const errorMsg = ref('')

function onFieldChanged(e) {
  form[e.dataField] = e.value
}

async function handleLogin() {
  errorMsg.value = ''
  try {
    await auth.login(form.loginId, form.password)
    await auth.fetchMe()
    router.push('/spaces')
  } catch {
    errorMsg.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f5f5;
}
.login-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  width: 380px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
.login-title {
  text-align: center;
  margin-bottom: 32px;
  font-size: 20px;
  color: #333;
}
.error-msg {
  color: #e53e3e;
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
}
</style>
```

- [ ] **Step 2: Phase 0 통합 테스트 — 로그인 흐름 수동 확인**

```bash
# 터미널 1: 백엔드 기동
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 터미널 2: 프론트엔드 기동
cd frontend && npm run dev

# 브라우저에서 http://localhost:3000 접속
# /login 리다이렉트 확인
# admin / Admin1234! 로 로그인
# /spaces 로 이동 확인 (빈 화면이어도 OK)
```

- [ ] **Step 3: Phase 0 최종 커밋**

```bash
git add frontend/src/views/
git commit -m "feat: add login view with DevExtreme Form"
git tag -a v0.1.0-phase0 -m "Phase 0 complete: project foundation ready"
```

---

## Phase 1: 사용자·Space·콘텐츠·권한 기본 기능 (4주)

> **Phase 1 시작 시 별도 계획서 `docs/superpowers/plans/2026-06-30-phase1-core.md` 를 작성하고 승인 후 실행한다.**

### Phase 1 범위 개요 (상세 계획서 작성 전 참고)

| 기능 영역 | 주요 API | 주요 화면 |
|---|---|---|
| 사용자 관리 | GET/POST/PUT/DELETE /users | - |
| 그룹 관리 | /groups, /groups/{id}/members | - |
| Space CRUD | GET/POST/PUT/DELETE /spaces | SpaceListView, SpaceHomeView |
| 콘텐츠 트리 | GET /spaces/{key}/contents (tree) | ContentTree 컴포넌트 |
| 콘텐츠 CRUD | /contents/{id} | ContentView, ContentEditorView |
| 버전 관리 | GET /contents/{id}/versions | VersionHistoryPanel |
| 권한 관리 | /spaces/{id}/permissions | SpacePermissionView |
| Space 즐겨찾기 | POST/DELETE /spaces/{id}/favorite | SpaceListView |

### Phase 1 핵심 설계 결정 (계획서 작성 시 반영)

- **권한 판단 로직 (PermissionService)**: 개인 권한 > 그룹 권한 > 전체 권한 순으로 우선 적용. 콘텐츠 권한이 없으면 Space 권한 상속.
- **콘텐츠 트리 API**: `GET /spaces/{key}/contents?parentId=&depth=` — 재귀 쿼리(WITH RECURSIVE) 또는 경로 열거(path enumeration) 방식 선택 필요.
- **TipTap 콘텐츠 저장 포맷**: JSON (TipTap Document) 으로 저장, 뷰어에서 `@tiptap/vue-3` `EditorContent` 읽기 모드로 렌더링.
- **버전 저장 시점**: 게시(Publish) 버튼 클릭 시 새 version_no 생성. 자동 임시저장은 Draft 덮어쓰기.

---

## Phase 2: 메일 서버 연동 (4주)

> **Phase 2 시작 시 별도 계획서 작성.**

### Phase 2 범위 개요

- IMAP Polling 기반 메일 동기화 스케줄러 (`@Scheduled`, 5분 주기 기본)
- `mail_accounts` CRUD API (Space Admin 권한)
- JavaMail을 이용한 IMAP FETCH (메일 UID 기반 중복 방지)
- 메일 본문 / 첨부파일 파싱 → DB 및 MinIO 저장
- 메일 → Page 전환 기능 (`POST /mail-messages/{id}/convert-to-page`)
- 메일함 UI (DxDataGrid + 스레드 뷰)
- 자격증명 AES-256 암호화 저장
- 동기화 실패 시 재시도 3회 + `sync_status` 에러 기록

---

## Phase 3: 검색·라벨·댓글·첨부파일 (3주)

> **Phase 3 시작 시 별도 계획서 작성.**

### Phase 3 범위 개요

- PostgreSQL tsvector 기반 전문검색 API (`GET /search?q=&spaceKey=&type=&from=&to=`)
- 검색 결과 화면 (DxList)
- 라벨 CRUD 및 콘텐츠 라벨 할당 API
- 댓글 CRUD (대댓글 포함, soft delete)
- 첨부파일 업로드 (Multipart → MinIO), 다운로드 (Presigned URL)
- 이미지 미리보기, PDF 미리보기

---

## Phase 4: 관리자 대시보드·알림·감사로그 (3주)

> **Phase 4 시작 시 별도 계획서 작성.**

### Phase 4 범위 개요

- 사용자/그룹 관리 UI (Site Admin 전용)
- 시스템 통계 대시보드 (DevExtreme Chart — Space 수, 콘텐츠 수, 스토리지 사용량)
- In-App 알림 API + SSE 또는 Polling 방식 실시간 알림
- 감사 로그 조회 API + UI (DxDataGrid, 날짜/행위자/타입 필터)
- 메일서버 연동 상태 모니터링

---

## Phase 5: 성능·보안·UAT (2주)

> **Phase 5 시작 시 별도 계획서 작성.**

### Phase 5 범위 개요

- API 응답 < 500ms P95 성능 측정 (JMeter/k6)
- 권한 조회 복합 인덱스 튜닝
- JWT 서명 키 교체 절차 문서화
- XSS 방어 확인 (DOMPurify 프론트 적용)
- SQL Injection 검토 (JPA Named Query / PreparedStatement 확인)
- 첨부파일 확장자 화이트리스트 검증
- UAT 시나리오 작성 및 실행

---

## 개발 원칙

1. **TDD**: 모든 API는 테스트 먼저 작성 → 실패 확인 → 구현 → 통과 확인 순으로 진행한다.
2. **작은 커밋**: 각 Task 완료 시 즉시 커밋한다. PR은 Phase 단위로 생성한다.
3. **YAGNI**: PRD에 명시된 기능만 구현한다. 미래 요구사항에 대한 추측 구현 금지.
4. **DRY**: 권한 체크 로직은 `PermissionService` 단일 지점에서 관리한다.
5. **보안 기본값**: 모든 API는 기본적으로 인증 필요. 예외(public)만 명시적으로 허용.
6. **로그/감사**: 주요 행위(Space 생성·삭제, 권한 변경, 메일 연동)는 반드시 `AuditLog` 기록.

---

## 승인 요청

위 계획서를 검토해 주세요. 승인 전에는 코딩 작업을 시작하지 않겠습니다.

**검토 포인트:**
1. Phase 0 Task 1~8이 적절한 순서와 깊이로 상세화되어 있는지
2. 기술 스택 선택(TipTap 에디터, MinIO, Flyway 등)이 PRD 요구사항에 부합하는지
3. DB 스키마(V1~V7)가 PRD 데이터 모델과 일치하는지
4. Phase 1~5 범위 개요가 PRD 마일스톤과 일치하는지
5. 추가/변경할 요구사항이 있는지
