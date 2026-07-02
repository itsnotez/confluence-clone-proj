# Phase 0 작업 결과 보고서

> 작성일: 2026-07-02 | 태그: `v0.1.0-phase0`

---

## 1. 개요

PRD(협업지식관리시스템)를 기반으로 작성된 마스터 개발계획서에 따라 Phase 0(프로젝트 기반 구축)을 완료하였다.

---

## 2. 개발 환경

| 항목 | 버전/내용 |
|---|---|
| Java | OpenJDK 21.0.11 (`/opt/homebrew/opt/openjdk@21`) |
| Maven | 3.9.16 (Maven Wrapper `./mvnw` 포함) |
| Node.js | 22.3.0 |
| npm | 10.8.1 |
| Docker | 29.2.0 |
| Docker Compose | v5.0.2 |
| Spring Boot | 3.3.1 |
| Vue | 3.x + Vite 5 |
| PostgreSQL | 16-alpine (Docker) |
| MinIO | latest (Docker) |

### 환경 변수 (로컬 개발 시 필수)
```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
```

---

## 3. 생성된 파일 목록

### 3.1 프로젝트 루트
```
confluence-clone-proj/
├── .gitignore
├── docker-compose.yml
└── docs/
    ├── PHASE0_RESULT.md          ← 이 파일
    └── superpowers/plans/
        └── 2026-06-30-confluence-clone-master-plan.md
```

### 3.2 백엔드 (`backend/`)
```
backend/
├── mvnw  (Maven Wrapper)
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/company/wiki/
    │   │   ├── WikiApplication.java
    │   │   ├── auth/
    │   │   │   ├── controller/AuthController.java     # POST /auth/login
    │   │   │   ├── dto/LoginRequest.java
    │   │   │   ├── dto/TokenResponse.java
    │   │   │   ├── security/JwtAuthenticationFilter.java
    │   │   │   ├── security/UserDetailsServiceImpl.java
    │   │   │   └── service/AuthService.java
    │   │   ├── common/
    │   │   │   ├── config/SecurityConfig.java         # Spring Security, JWT 설정
    │   │   │   ├── exception/BusinessException.java
    │   │   │   ├── exception/ErrorCode.java
    │   │   │   ├── exception/GlobalExceptionHandler.java
    │   │   │   ├── response/ApiResponse.java
    │   │   │   └── util/JwtProvider.java
    │   │   └── user/
    │   │       ├── controller/UserController.java     # GET /users/me
    │   │       ├── entity/User.java
    │   │       └── repository/UserRepository.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           ├── V1__init_users_groups.sql
    │           ├── V2__spaces.sql
    │           ├── V3__contents.sql
    │           ├── V4__permissions.sql
    │           ├── V5__mail.sql
    │           ├── V6__search_indexes.sql
    │           └── V7__audit_notifications.sql
    └── test/java/com/company/wiki/
        ├── auth/controller/AuthControllerTest.java    # 3개 테스트
        └── common/util/JwtProviderTest.java           # 3개 테스트
```

### 3.3 프론트엔드 (`frontend/`)
```
frontend/
├── vite.config.js
├── package.json
└── src/
    ├── App.vue
    ├── main.js
    ├── api/axios.js                # Axios + JWT 인터셉터 + 토큰 갱신
    ├── router/index.js             # Vue Router (8개 라우트)
    ├── stores/auth.js              # Pinia 인증 스토어
    └── views/
        ├── auth/LoginView.vue      # 로그인 화면 (DevExtreme Form)
        ├── space/SpaceListView.vue         (placeholder)
        ├── space/SpaceHomeView.vue         (placeholder)
        ├── content/ContentView.vue         (placeholder)
        ├── content/ContentEditorView.vue   (placeholder)
        ├── search/SearchResultView.vue     (placeholder)
        └── admin/AdminDashboardView.vue    (placeholder)
```

---

## 4. DB 스키마 (Flyway V1~V7)

총 **21개 테이블** 생성 완료.

| 마이그레이션 | 테이블 |
|---|---|
| V1 | `users`, `groups`, `group_members` |
| V2 | `spaces`, `space_favorites` |
| V3 | `contents`, `content_versions`, `attachments`, `labels`, `content_labels`, `comments` |
| V4 | `space_permissions`, `content_permissions` |
| V5 | `mail_accounts`, `mail_messages`, `mail_attachments` |
| V6 | `content_search_bodies`, `mail_message_search` (tsvector) |
| V7 | `audit_logs`, `notifications` |

---

## 5. 구현된 API

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/login` | 불필요 | 로그인 → Access/Refresh Token 발급 |
| GET | `/api/users/me` | 필요 | 현재 로그인 사용자 조회 |

### 응답 형식 (공통)
```json
{ "success": true, "data": { ... }, "message": null }
{ "success": false, "data": null, "message": "오류 메시지" }
```

### 로그인 예시
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"Admin1234!"}'
```

---

## 6. 테스트 결과

| 테스트 클래스 | 테스트 수 | 결과 |
|---|---|---|
| `JwtProviderTest` | 3 | ✅ 전체 통과 |
| `AuthControllerTest` | 3 | ✅ 전체 통과 |
| **합계** | **6** | **✅ 전체 통과** |

```bash
# 전체 테스트 실행
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend && ./mvnw test
```

---

## 7. 기동 방법

### Docker (DB + Storage)
```bash
# 프로젝트 루트에서
docker compose up -d
```

### 백엔드
```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# → http://localhost:8080/api
```

### 프론트엔드
```bash
cd frontend && npm run dev
# → http://localhost:3000
```

---

## 8. 주요 기술 결정 및 이슈 해결

| 이슈 | 결정 |
|---|---|
| Vite 8.x ARM64 rolldown 바인딩 오류 | Vite 5 LTS로 다운그레이드 |
| Java 21 미설치 | `brew install openjdk@21` |
| Maven 미설치 | `brew install maven` + `mvn wrapper:wrapper` |
| admin 초기 비밀번호 해시 불일치 | V1 SQL에 올바른 bcrypt 해시 반영 |
| JWT `UserDetailsService` 설계 | `loadUserByUsername(userId)` — loginId 아닌 userId로 조회 |
| 미인증 요청 HTTP 상태 | `AuthenticationEntryPoint` 추가로 403 → 401 수정 |

---

## 9. 초기 계정 정보

| 항목 | 값 |
|---|---|
| 로그인 ID | `admin` |
| 비밀번호 | `Admin1234!` |
| 권한 | `SITE_ADMIN` |
| 이메일 | `admin@company.com` |

---

## 10. 다음 단계 (Phase 1)

계획서: `docs/superpowers/plans/2026-06-30-phase1-core.md` 작성 및 승인 후 시작.

**Phase 1 범위:**
- Space CRUD + 즐겨찾기
- 콘텐츠(Page/Blog) CRUD + 트리 구조
- 콘텐츠 버전 관리
- Space/콘텐츠 단위 RBAC 권한 관리
- TipTap 에디터 연동
- Vue3 Space 목록/홈/콘텐츠 뷰어/에디터 화면
