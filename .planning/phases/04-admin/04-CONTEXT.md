# Phase 4: 관리자 대시보드·알림·감사로그 - Context

**Gathered:** 2026-07-07
**Status:** Ready for planning
**Source:** PRD Express Path (PRD_협업지식관리시스템.md)

<domain>
## Phase Boundary

Phase 4는 운영 도구 및 모니터링을 구축한다. 세 가지 독립적인 기능 도메인으로 구성된다:

1. **관리자 대시보드** — Site Admin 전용 운영 현황 뷰 (사용자/Space 통계, 스토리지 사용량, 메일서버 상태) — DevExtreme Chart 기반
2. **In-app 알림** — 멘션·댓글·콘텐츠 변경·메일 수신 이벤트를 `notifications` 테이블에 적재하고 프론트엔드에서 폴링 또는 SSE로 표시
3. **감사로그** — 권한 변경·콘텐츠 삭제·관리자 접근 등 주요 행위를 `audit_logs` 테이블에 기록하고 조회 API + 관리자 UI 제공

DB 스키마(V7__audit_notifications.sql)는 이미 마이그레이션 완료 상태다:
- `audit_logs(id, actor_id, action_type, target_type, target_id, detail JSONB, is_admin_access, created_at)`
- `notifications(id, user_id, type, title, message, is_read, link_url, created_at)`

이 Phase는 스키마 없는 순수 서비스/API/프론트엔드 구현 Phase다.

**이 Phase에서 하지 않는 것:**
- WebSocket 기반 실시간 알림 (1차는 폴링 또는 SSE)
- 메일 발송(SMTP) 알림
- Guest 계정 기능
- 콘텐츠 내보내기(PDF/HTML)

</domain>

<decisions>
## Implementation Decisions

### 관리자 대시보드
- Site Admin 전용 (`ROLE_SITE_ADMIN`), Spring Security `@PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")`
- 통계 API: Space 수, 활성 사용자 수, 콘텐츠 수, 스토리지 사용량(MinIO) 집계
- 메일서버 연동 상태 포함 (MailAccount sync_status 집계)
- 프론트엔드: `AdminDashboardView.vue` stub → DevExtreme Chart + DataGrid 구현

### In-app 알림
- `notifications` 테이블 기반 (V7 스키마 이미 존재)
- 알림 발생 시점: **댓글 생성(콘텐츠 작성자에게 알림)만 구현** — Space 구독자/멘션/메일수신 알림은 이 Phase에서 제외 (사용자 결정: 2026-07-07)
- 알림 조회: `GET /api/notifications` (페이징, is_read 필터)
- 읽음 처리: `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`
- 폴링 방식 (30초 간격) — SSE는 Claude 재량
- 프론트엔드: AppHeader에 알림 벨 아이콘 + 드롭다운 패널

### 감사로그
- AOP (`@Aspect`) 또는 서비스 메서드에서 직접 `AuditLogService.record()` 호출
- 기록 대상: 권한 변경(PERMISSION_CHANGE), 콘텐츠 삭제(CONTENT_DELETE), 관리자 접근(ADMIN_ACCESS), Space 삭제(SPACE_DELETE), 메일계정 등록/삭제(MAIL_ACCOUNT_CREATE/DELETE)
- `is_admin_access=true` 는 Site Admin이 수행한 모든 행위에 설정
- 조회 API: `GET /api/admin/audit-logs` (기간·actor·action_type 필터, 페이징)
- 프론트엔드: 관리자 대시보드 내 탭 또는 별도 AuditLogView

### Claude's Discretion
- 알림 생성 시 AOP vs 직접 호출 방식 선택
- 알림 드롭다운 UI 세부 레이아웃 (DevExtreme Popover 또는 커스텀)
- 관리자 대시보드 탭 구조 (대시보드 / 감사로그 / 사용자관리 등)
- 스토리지 사용량 집계 방법 (MinIO Admin API vs attachments 테이블 합산)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 기존 패턴 (Phase 1~3에서 검증된 구조)
- `backend/src/main/java/com/company/wiki/comment/service/CommentService.java` — 서비스 패턴 참조
- `backend/src/main/java/com/company/wiki/space/controller/SpaceController.java` — 컨트롤러 패턴
- `backend/src/main/java/com/company/wiki/common/response/ApiResponse.java` — 공통 응답 래퍼
- `backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java` — 에러 코드 추가 위치
- `backend/src/main/java/com/company/wiki/permission/service/PermissionService.java` — 권한 체크 인터페이스

### DB 스키마 (이미 완료)
- `backend/src/main/resources/db/migration/V7__audit_notifications.sql` — audit_logs, notifications 스키마

### 프론트엔드 패턴
- `frontend/src/views/admin/AdminDashboardView.vue` — 구현 대상 stub
- `frontend/src/api/space.js` — API 모듈 패턴 (default export)
- `frontend/src/stores/auth.js` — Pinia 스토어 패턴
- `frontend/src/router/index.js` — 라우터 등록 위치

### 인증/권한
- `backend/src/main/java/com/company/wiki/auth/` — JWT 인증 구조
- Phase 1 결정: `@PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")` 로 Site Admin 체크

</canonical_refs>

<specifics>
## Specific Ideas

- DevExtreme Chart: `DxChart`, `DxPieChart` 활용 (이미 `devextreme-vue` 패키지 존재)
- 통계 집계는 단순 COUNT/SUM SQL — 복잡한 집계 뷰 생성 필요 없음
- 알림 미읽음 카운트: `GET /api/notifications/unread-count` — 헤더 배지용
- `audit_logs.detail` 컬럼은 JSONB — 변경 전후 값, IP, 추가 메타데이터 저장 가능

</specifics>

<deferred>
## Deferred Ideas

- WebSocket(SockJS/STOMP) 기반 실시간 Push 알림 — Phase 5 이후 고려
- 메일 발송(SMTP) 알림 — 1차 범위 외
- Guest 계정 관리 — Open Question (Phase 5 이후)
- Elasticsearch 검색엔진 연동 — 이미 Out of Scope (PRD 명시)
- 첨부파일 바이러스 스캔 — PRD 선택사항, 이 Phase에서 제외
- i18n 다국어 — Phase 5 QA 단계에서 검토

</deferred>

---

*Phase: 04-admin*
*Context gathered: 2026-07-07 via PRD Express Path (PRD_협업지식관리시스템.md)*
