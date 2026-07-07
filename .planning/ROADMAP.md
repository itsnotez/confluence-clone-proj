# Roadmap: 사내 협업 지식관리 시스템 (Confluence Clone)

## Overview

Vue3 + Spring Boot + PostgreSQL 기반의 사내 협업 지식관리 시스템을 Phase 0~5에 걸쳐 구축한다.
Phase 0(기반 환경)은 완료되었으며, Phase 1(핵심 기능)부터 순차적으로 실행한다.

## Phases

- [x] **Phase 0: 프로젝트 기반 구축** - Docker, Spring Boot, DB 스키마, JWT 인증, Vue3 초기화 (완료)
- [x] **Phase 1: 사용자·Space·콘텐츠·권한 기본 기능** - CRUD API + 프론트엔드 핵심 화면 (완료)
- [x] **Phase 2: 메일 서버 연동** - IMAP Polling 기반 메일 동기화 (완료)
- [x] **Phase 3: 검색·라벨·댓글·첨부파일** - PostgreSQL tsvector 전문검색 + 파일 관리 (완료)
- [ ] **Phase 4: 관리자 대시보드·알림·감사로그** - 운영 도구
- [ ] **Phase 5: 성능·보안·UAT** - 품질 강화

## Phase Details

### Phase 0: 프로젝트 기반 구축 ✓
**Goal**: 개발 환경 및 기반 코드 구축
**Status**: Complete (2026-07-01), tag: v0.1.0-phase0
**Plans**: 8 tasks — all complete

### Phase 1: 사용자·Space·콘텐츠·권한 기본 기능
**Goal**: Space와 콘텐츠를 CRUD하고 RBAC 권한 제어가 동작하는 완전한 핵심 기능
**Status**: Complete (2026-07-06), tag: v0.2.0-phase1
**Depends on**: Phase 0
**Requirements**: REQ-USER, REQ-GROUP, REQ-SPACE, REQ-CONTENT, REQ-PERMISSION, REQ-VERSION, REQ-FRONTEND
**Success Criteria** (what must be TRUE):
  1. 사용자/그룹 CRUD API가 정상 동작하고 테스트를 통과한다
  2. Space 생성·조회·수정·삭제가 동작하며 SpaceListView에서 확인된다
  3. 콘텐츠 계층 트리 API가 동작하고 ContentTree에서 렌더링된다
  4. TipTap 에디터로 콘텐츠를 작성·게시하면 DB에 저장된다
  5. 버전 목록 조회가 동작하고 VersionHistoryPanel에서 확인된다
  6. PermissionService가 개인>그룹>전체 우선순위로 권한을 판단한다
**Plans**: 7 plans

Plans:
- [x] 01-01: 사용자/그룹 CRUD API (백엔드)
- [x] 01-02: Space CRUD API (백엔드)
- [x] 01-03: 권한 관리 — PermissionService + Entity + API (백엔드)
- [x] 01-04: 콘텐츠 CRUD + 트리 API (백엔드)
- [x] 01-05: Space 프론트엔드 — SpaceListView, SpaceHomeView, ContentTree
- [x] 01-06: 콘텐츠 편집기 — ContentView, ContentEditorView (TipTap)
- [x] 01-07: 버전 관리 + VersionHistoryPanel + SpacePermissionView

### Phase 2: 메일 서버 연동
**Goal**: IMAP Polling 기반 메일 동기화 및 메일함 UI
**Status**: Complete (2026-07-06)
**Depends on**: Phase 1
**Requirements**: REQ-MAIL-ACCOUNT, REQ-MAIL-FRONTEND
**Plans**: 4 plans

Plans:
- [x] 02-01: 메일 계정 CRUD API + AES-256 자격증명 암호화 (백엔드)
- [x] 02-02: IMAP Polling 메일 동기화 서비스 (MailMessage + ImapService + MailSyncService + MailPollingScheduler)
- [x] 02-03: 메일 메시지 조회 + 페이지 변환 API (MailMessageDto + MailMessageService + MailMessageController)
- [x] 02-04: 메일함 프론트엔드 (mail API + Pinia 스토어 + MailBoxView.vue + 라우터)

### Phase 3: 검색·라벨·댓글·첨부파일
**Goal**: PostgreSQL tsvector 전문검색 + 파일 관리
**Status**: Complete (2026-07-07)
**Depends on**: Phase 2
**Requirements**: SEARCH-01, LABEL-01, LABEL-02, COMMENT-01, COMMENT-02, ATTACH-01, ATTACH-02
**Plans**: 6 plans

Wave 1:
- [x] 03-01: 전문검색 API — SearchService + SearchController + ContentSearchBody upsert (백엔드)

Wave 2 *(blocked on Wave 1 completion)*:
- [x] 03-02: 라벨 API — Label/ContentLabel entity/service/controller + PermissionService 체크 (백엔드)
- [x] 03-03: 댓글 API — Comment entity/service/controller + soft delete (백엔드)
- [x] 03-04: 첨부파일 API — Attachment entity + S3Config + StorageService + MinIO 연동 (백엔드)

Wave 3 *(blocked on Wave 2 completion)*:
- [x] 03-05: 검색·라벨 프론트엔드 — SearchResultView + AppHeader 검색창 + ContentView 라벨 패널

Wave 4 *(blocked on Wave 3 completion)*:
- [x] 03-06: 댓글·첨부파일 프론트엔드 — CommentPanel + AttachmentPanel + ContentView 통합

### Phase 4: 관리자 대시보드·알림·감사로그
**Goal**: 운영 도구 및 모니터링 — Site Admin 대시보드, In-app 알림, 감사로그
**Depends on**: Phase 3
**Requirements**: ADMIN-01, ADMIN-02, NOTIF-01, NOTIF-02, AUDIT-01, AUDIT-02
**Plans**: 6 plans

Wave 1 *(독립 실행 가능)*:
- [x] 04-01-PLAN.md — 감사로그 백엔드 (AuditLog entity/repository/service + PermissionService/SpaceService/MailAccountService/ContentService 통합)
- [x] 04-02-PLAN.md — 알림 백엔드 (Notification entity/repository/service + CommentService 통합)

Wave 2 *(blocked on Wave 1 completion)*:
- [x] 04-03-PLAN.md — 관리자 통계 API (AdminStatsService + AdminController: GET /admin/stats, GET /admin/audit-logs)
- [x] 04-04-PLAN.md — 알림 조회/읽음처리 API (NotificationController: GET /notifications, PATCH read/read-all)

Wave 3 *(blocked on Wave 2 completion)*:
- [ ] 04-05-PLAN.md — 관리자 대시보드 프론트엔드 (AdminDashboardView.vue: DxTabPanel + DxChart + DxDataGrid)
- [ ] 04-06-PLAN.md — 알림 프론트엔드 (AppHeader 알림 벨 + 드롭다운 + Pinia 스토어 폴링)

### Phase 5: 성능·보안·UAT
**Goal**: P95 < 500ms + XSS/SQL Injection 방어 + UAT
**Depends on**: Phase 4
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 0. 기반 구축 | 8/8 | Complete | 2026-07-01 |
| 1. 핵심 기능 | 7/7 | Complete | 2026-07-06 |
| 2. 메일 연동 | 4/4 | Complete | 2026-07-06 |
| 3. 검색·첨부 | 6/6 | Complete | 2026-07-07 |
| 4. 관리자·알림 | 4/6 | In Progress|  |
| 5. 성능·UAT | TBD | Not started | - |
