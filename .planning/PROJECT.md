# 사내 협업 지식관리 시스템 (Confluence Clone)

## What This Is

사내 직원이 문서를 작성·공유·검색하고, 메일 수신 내용을 지식으로 전환할 수 있는 Confluence 유사 협업 플랫폼이다. Spring Boot REST API와 Vue3 SPA를 분리 운영하며, Space 단위로 콘텐츠를 계층 구조로 관리한다.

## Core Value

Space·콘텐츠 CRUD와 RBAC 권한 제어가 올바르게 동작해야 한다 — 이것 없이는 다른 모든 기능이 의미 없다.

## Requirements

### Validated

- ✓ Docker Compose 환경 (PostgreSQL 16 + MinIO) — Phase 0
- ✓ Spring Boot 3.3.1 백엔드 기반 (JWT 인증, Spring Security 6) — Phase 0
- ✓ Flyway V1~V7 DB 스키마 (21개 테이블 전체) — Phase 0
- ✓ 공통 모듈 (ApiResponse, ErrorCode, JwtProvider) — Phase 0
- ✓ Vue3 + Vite5 프론트엔드 (DevExtreme, Pinia, Vue Router 4, TipTap) — Phase 0
- ✓ 로그인 API + LoginView (admin/Admin1234! 동작 확인) — Phase 0

### Active

- [ ] 사용자·그룹 CRUD API + 그룹 멤버 관리
- [ ] Space CRUD API (SpaceListView, SpaceHomeView)
- [ ] 콘텐츠 트리 API (재귀 쿼리) + ContentTree 컴포넌트
- [ ] 콘텐츠 CRUD + TipTap 편집기 (ContentView, ContentEditorView)
- [ ] 버전 관리 (Publish 시 새 version_no 생성, Draft 자동저장)
- [ ] Space/Content 단위 RBAC 권한 관리 (PermissionService + AOP)
- [ ] Space 즐겨찾기 (POST/DELETE /spaces/{id}/favorite)

### Out of Scope

- 실시간 협업 편집 (WebSocket/CRDT) — PRD 미포함, 복잡도 과다
- Elasticsearch 도입 — PostgreSQL tsvector로 전문검색 충분
- 모바일 앱 — 웹 브라우저만 지원
- OAuth/SSO 로그인 — ID/PW 기반 JWT 인증으로 확정
- H2 인메모리 테스트 — 실제 PostgreSQL Docker 사용으로 확정 (mock/real 불일치 방지)

## Context

- **Phase 0 완료** (2026-07-01), 태그 `v0.1.0-phase0`
- **Java 경로**: `/opt/homebrew/opt/openjdk@21/bin` (PATH 수동 추가 필요)
- **DB 접속**: `wikidb / wikiuser / wikipass @ localhost:5432`
- **Backend**: `http://localhost:8080/api`, **Frontend**: `http://localhost:3000`
- **admin 계정**: `admin / Admin1234!` (bcrypt: `$2a$10$emFjSKuytOxWelbOlkasgu5sxib.AUTQ4OlorXsYp.4zTRzf8bLXO`)
- TipTap Document JSON 포맷으로 콘텐츠 저장, 뷰어에서 읽기 모드 렌더링
- Phase 1~5 각 시작 전 별도 세부 계획서 작성 및 승인 후 실행

## Constraints

- **Tech Stack**: Vue3 (Composition API) + Spring Boot 3.3 + PostgreSQL 16 — 변경 금지
- **Frontend Build**: Vite 5 고정 (Vite 8 ARM64 rolldown 바인딩 버그로 다운그레이드)
- **Auth**: JWT only (access 1h, refresh 7d) — UserDetailsService는 userId(String) 기반
- **Storage**: MinIO (S3 호환) — 첨부파일 전용, DB에 직접 바이너리 저장 금지
- **Search**: PostgreSQL tsvector — 외부 검색엔진 도입 금지
- **Test**: 실제 PostgreSQL Docker 사용 — H2/Mock DB 금지 (Phase 0에서 결정)
- **Migration**: Flyway 전용 — JPA `ddl-auto: validate` (스키마 자동 변경 금지)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| TipTap 에디터 선택 | Confluence 수준 리치텍스트, Vue3 공식 지원, 확장 생태계 | — Pending |
| JWT subject = userId (loginId 아님) | UserDetailsService DB 조회 일관성, 로그인 ID 변경 시 토큰 무효화 불필요 | ✓ Good |
| PostgreSQL tsvector 전문검색 | 별도 인프라 없이 한국어 포함 검색 가능, simple 사전으로 충분 | — Pending |
| MinIO S3 호환 스토리지 | 로컬 개발환경에서 AWS S3 동일 인터페이스, 프로덕션 마이그레이션 용이 | — Pending |
| Flyway + ddl-auto:validate | 스키마 변경 이력 관리, 실수로 인한 DROP 방지 | ✓ Good |
| Vite 5 고정 (8 → 5 다운그레이드) | ARM64 환경 rolldown 네이티브 바인딩 빌드 실패 | ✓ Good |
| 권한: 개인 > 그룹 > 전체 우선순위 | Confluence 표준 동작, 세밀한 개인 권한 부여 가능 | — Pending |
| 테스트: 실제 PostgreSQL 사용 | 지난 경험: mock 테스트 통과 후 실 DB 마이그레이션 실패 사례 방지 | ✓ Good |

---
*Last updated: 2026-07-02 after Phase 0 complete (v0.1.0-phase0)*
