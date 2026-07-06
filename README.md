# 사내 협업 지식관리 시스템

사내 직원이 문서를 작성·공유·검색하고, 수신 메일을 Wiki 페이지로 전환할 수 있는 Confluence 유사 협업 플랫폼입니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.3.1, Java 21, Spring Security 6, JWT |
| Frontend | Vue 3 (Composition API), Vite 5, Pinia, Vue Router 4 |
| UI 라이브러리 | DevExtreme Vue, TipTap Editor |
| DB | PostgreSQL 16, Flyway (마이그레이션) |
| Storage | MinIO (S3 호환, 첨부파일) |
| 인프라 | Docker Compose |

## 주요 기능

- **사용자·그룹 관리** — 회원 CRUD, 그룹 멤버십
- **Space 관리** — Space 생성/수정/삭제, 즐겨찾기
- **콘텐츠 편집** — TipTap 리치텍스트 에디터, 계층 트리 구조, 버전 관리
- **RBAC 권한 제어** — Space/콘텐츠 단위 개인·그룹·전체 권한 (개인 > 그룹 > 전체 우선순위)
- **메일 연동** — IMAP Polling 기반 메일 동기화, 수신 메일 → Wiki 페이지 변환

## 빠른 시작

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- Node.js 18+

### 1. 인프라 실행

```bash
docker compose up -d
```

PostgreSQL(5432), MinIO(9000/9001)가 시작됩니다.

### 2. 백엔드 실행

```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"   # macOS Homebrew
cd backend
./mvnw spring-boot:run
```

`http://localhost:8080/api` 에서 실행됩니다.

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

`http://localhost:3000` 에서 실행됩니다.

### 기본 관리자 계정

| 항목 | 값 |
|------|-----|
| 아이디 | `admin` |
| 비밀번호 | `Admin1234!` |

## 프로젝트 구조

```
confluence-clone-proj/
├── backend/                  # Spring Boot API 서버
│   └── src/main/java/com/company/wiki/
│       ├── auth/             # JWT 인증
│       ├── user/             # 사용자·그룹
│       ├── space/            # Space 관리
│       ├── content/          # 콘텐츠·버전
│       ├── permission/       # RBAC 권한
│       ├── mail/             # 메일 연동 (IMAP)
│       └── common/           # 공통 모듈 (ApiResponse, ErrorCode 등)
├── frontend/                 # Vue 3 SPA
│   └── src/
│       ├── views/            # 페이지 컴포넌트
│       ├── components/       # 공통 컴포넌트
│       ├── stores/           # Pinia 상태 관리
│       ├── api/              # Axios API 클라이언트
│       └── router/           # Vue Router 설정
├── docker-compose.yml        # PostgreSQL + MinIO
└── .planning/                # GSD 프로젝트 계획 (ROADMAP, STATE 등)
```

## API 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/login` | 로그인 (JWT 발급) |
| GET | `/api/users` | 사용자 목록 |
| GET/POST | `/api/spaces` | Space 목록/생성 |
| GET | `/api/spaces/{key}/contents/tree` | 콘텐츠 트리 조회 |
| POST | `/api/spaces/{key}/contents` | 콘텐츠 생성 |
| POST | `/api/spaces/{key}/contents/{id}/publish` | 콘텐츠 게시 |
| GET/POST | `/api/spaces/{key}/mail-accounts` | 메일 계정 관리 |
| GET | `/api/spaces/{key}/mail-accounts/{id}/messages` | 수신 메일 목록 |
| POST | `/api/spaces/{key}/mail-accounts/{id}/messages/{msgId}/convert` | 메일 → 페이지 변환 |

## 개발 진행 상태

| Phase | 내용 | 상태 |
|-------|------|------|
| 0 | 프로젝트 기반 구축 (Docker, DB 스키마, JWT, Vue3 초기화) | ✅ 완료 |
| 1 | 사용자·Space·콘텐츠·권한 기본 기능 | ✅ 완료 |
| 2 | 메일 서버 연동 (IMAP Polling, 메일함 UI) | ✅ 완료 |
| 3 | 검색·라벨·댓글·첨부파일 | 예정 |
| 4 | 관리자 대시보드·알림·감사로그 | 예정 |
| 5 | 성능·보안·UAT | 예정 |

## 테스트 실행

```bash
cd backend
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
./mvnw test
```

> 테스트는 실제 PostgreSQL Docker 컨테이너를 사용합니다. `docker compose up -d` 선행 필요.
