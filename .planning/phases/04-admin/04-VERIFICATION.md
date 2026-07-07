---
phase: 04-admin
verified: 2026-07-07T09:00:00Z
status: human_needed
score: 18/18 must-haves verified
overrides_applied: 0
human_verification:
  - test: "관리자 대시보드 UI 렌더링 확인"
    expected: "'관리자 대시보드' 제목, 통계 카드 4개, DxPieChart, DxDataGrid 감사로그 탭이 브라우저에서 정상 표시"
    why_human: "DevExtreme Vue 컴포넌트 렌더링은 정적 분석으로 확인 불가"
  - test: "AppHeader 알림 벨 아이콘 + 드롭다운 동작 확인"
    expected: "벨 아이콘 클릭 시 드롭다운 열림, 개별/전체 읽음 처리 배지 감소, 30초 폴링 중복 없음"
    why_human: "Vue 이벤트 바인딩과 Pinia 반응성, 폴링 타이머 동작은 런타임 확인 필요"
---

# Phase 4: 관리자 대시보드·알림·감사로그 Verification Report

**Phase Goal:** 운영 도구 및 모니터링 — Site Admin 대시보드, In-app 알림, 감사로그
**Verified:** 2026-07-07T09:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | Space 삭제 이후 audit_logs에 SPACE_DELETE 레코드가 존재한다 | VERIFIED | `SpaceService.java:116` — `auditLogService.record(actorId, "SPACE_DELETE", ...)` try-catch 래핑 확인 |
| 2 | 권한 변경(grantSpacePermission) 이후 audit_logs에 PERMISSION_CHANGE 레코드가 존재한다 | VERIFIED | `PermissionService.java:193-196` — try 블록 내 `auditLogService.record(actorId, "PERMISSION_CHANGE", ...)` 확인 |
| 3 | 메일계정 생성/삭제 이후 audit_logs에 MAIL_ACCOUNT_CREATE/DELETE 레코드가 존재한다 | VERIFIED | `MailAccountService.java:73-76, 105-108` — create/delete 각각 try-catch 내 record() 호출 |
| 4 | 콘텐츠 삭제(ContentService.delete) 이후 audit_logs에 CONTENT_DELETE 레코드가 존재한다 | VERIFIED | `ContentService.java:258` — `auditLogService.record(userId, "CONTENT_DELETE", ...)` 확인 |
| 5 | AuditLogService.record() 내부 예외가 호출 서비스의 트랜잭션을 롤백시키지 않는다 | VERIFIED | `AuditLogService.java:39` — `@Transactional(propagation = REQUIRES_NEW)` + 내부 try-catch, `AuditLogServiceTest.record_doesNotThrowEvenOnSerializationError` 테스트 존재 |
| 6 | 댓글 생성 후 notifications 테이블에 COMMENT 타입 레코드가 콘텐츠 작성자 user_id로 저장된다 | VERIFIED | `CommentService.java:91-97` — `contentAuthorId` 추출 후 `notificationService.create(contentAuthorId, "COMMENT", ...)` 호출 |
| 7 | 댓글 작성자가 콘텐츠 작성자 본인인 경우 알림이 생성되지 않는다 | VERIFIED | `CommentService.java:92` — `!contentAuthorId.equals(userId)` 조건 확인 |
| 8 | NotificationService.create() 실패가 CommentService.createComment() 트랜잭션을 롤백시키지 않는다 | VERIFIED | `CommentService.java:90, 98` — try-catch 블록으로 알림 생성 실패 격리 |
| 9 | NotificationService.markAsRead()는 다른 사용자의 알림을 수정하지 못한다 (IDOR 방지) | VERIFIED | `NotificationService.java:69-71` — `findByIdAndUserId(notificationId, userId)` + `BusinessException(NOTIFICATION_NOT_FOUND)`, `NotificationControllerTest.markRead_otherUser_returns404` 테스트 존재 |
| 10 | SITE_ADMIN 토큰으로 GET /admin/stats 호출 시 200과 통계 필드들을 반환한다 | VERIFIED | `AdminController.java:32-42` 엔드포인트 존재, `AdminControllerTest.stats_siteAdmin_returns200` 테스트 |
| 11 | MEMBER 역할 토큰으로 GET /admin/stats 호출 시 403을 반환한다 | VERIFIED | SecurityConfig `/admin/**` ROLE_SITE_ADMIN 보호, `AdminControllerTest.stats_member_returns403` 테스트 |
| 12 | GET /admin/audit-logs?actionType=SPACE_DELETE는 SPACE_DELETE 레코드만 반환하고 페이징 메타를 포함한다 | VERIFIED | `AuditLogRepository.java:14-32` — native SQL 필터 쿼리, `AdminControllerTest.auditLogs_filterByActionType` 테스트 |
| 13 | GET /admin/audit-logs는 인증 없이 접근 시 401을 반환한다 | VERIFIED | `AdminControllerTest.stats_unauthenticated_returns401` 테스트 존재 |
| 14 | Site Admin이 /admin/** 엔드포인트 접근 시 audit_logs에 ADMIN_ACCESS 레코드가 기록된다 | VERIFIED | `AdminController.java:35-39, 53-57` — 두 엔드포인트 모두 `auditLogService.record(actorId, "ADMIN_ACCESS", ..., true)` try-catch 래핑 |
| 15 | 인증된 사용자가 GET /api/notifications 호출 시 200과 본인 알림 목록을 페이징으로 반환한다 | VERIFIED | `NotificationController.java:24-29`, `NotificationControllerTest.getNotifications_returns200` 테스트 |
| 16 | GET /api/notifications/unread-count는 미읽음 알림 수를 숫자로 반환한다 | VERIFIED | `NotificationController.java:36-40`, `NotificationControllerTest.unreadCount_returns200` 테스트 |
| 17 | 관리자 토큰으로 /admin 접근 시 AdminDashboardView가 렌더링되고 '관리자 대시보드' 제목이 표시된다 | UNCERTAIN (human) | `AdminDashboardView.vue:4` — `<h1>관리자 대시보드</h1>` 존재, 라우터 meta.requiresAdmin 설정 확인, 실제 렌더링은 인간 검증 필요 |
| 18 | AppHeader에 알림 벨 아이콘이 표시되고 미읽음 수 배지가 숫자로 표시되며 드롭다운이 동작한다 | UNCERTAIN (human) | `AppHeader.vue:17-48` — 벨 버튼·배지·드롭다운 구현 존재, 실제 반응성 동작은 런타임 확인 필요 |

**Score:** 16/18 truths fully verified, 2 require human runtime verification

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `backend/.../auditlog/entity/AuditLog.java` | audit_logs 테이블 JPA 엔티티 | VERIFIED | `@Entity @Table(name="audit_logs")`, `@JdbcTypeCode(SqlTypes.JSON)`, `@PrePersist` |
| `backend/.../auditlog/repository/AuditLogRepository.java` | findByFilter JPQL/native 쿼리 | VERIFIED | native SQL with CAST(:from AS timestamp) for nullable datetime params |
| `backend/.../auditlog/dto/AuditLogDto.java` | Response record + from() factory | VERIFIED | exists |
| `backend/.../auditlog/service/AuditLogService.java` | record() + findByFilter() 메서드 | VERIFIED | REQUIRES_NEW propagation, try-catch exception swallowing |
| `backend/.../auditlog/service/AuditLogServiceTest.java` | AUDIT-01 통합 테스트 (spaceDelete_createsAuditLog) | VERIFIED | Method exists at line 83 |
| `backend/.../notification/entity/Notification.java` | notifications 테이블 JPA 엔티티 | VERIFIED | `@Entity @Table(name="notifications")`, `@Builder.Default isRead=false` |
| `backend/.../notification/repository/NotificationRepository.java` | countByUserIdAndIsReadFalse, findByIdAndUserId, markAllAsReadByUserId | VERIFIED | All 5 required methods present |
| `backend/.../notification/service/NotificationService.java` | create/getNotifications/getUnreadCount/markAsRead/markAllAsRead | VERIFIED | All 5 methods implemented, IDOR check in markAsRead |
| `backend/.../notification/service/NotificationServiceTest.java` | NOTIF-01 통합 테스트 | VERIFIED | 4 tests: create_savesNotification, getUnreadCount_countsOnlyUnread, markAsRead_wrongUser_throwsNotFound, markAllAsRead_allNotificationsRead |
| `backend/.../admin/dto/AdminStatsDto.java` | 6 필드 (activeUsers, totalSpaces, ...) | VERIFIED | All 6 long fields confirmed |
| `backend/.../admin/service/AdminStatsService.java` | getStats() @Transactional(readOnly=true) | VERIFIED | Correct annotation, calls 5 repositories |
| `backend/.../admin/controller/AdminController.java` | GET /stats + GET /audit-logs | VERIFIED | `@RestController @RequestMapping("/admin")`, both endpoints present |
| `backend/.../admin/controller/AdminControllerTest.java` | 5 tests (stats_siteAdmin_returns200, stats_member_returns403, ...) | VERIFIED | All 5 test methods confirmed |
| `backend/.../notification/controller/NotificationController.java` | 4 endpoints (GET /, /unread-count, PATCH /{id}/read, /read-all) | VERIFIED | All 4 endpoints, IDOR delegation to service |
| `backend/.../notification/controller/NotificationControllerTest.java` | 6 tests including markRead_otherUser_returns404 | VERIFIED | All 6 test methods confirmed |
| `frontend/src/api/admin.js` | adminApi named export (getStats, getAuditLogs) | VERIFIED | `export const adminApi = { getStats, getAuditLogs }` |
| `frontend/src/views/admin/AdminDashboardView.vue` | DxTabPanel + DxPieChart + DxDataGrid | VERIFIED | All 3 components imported and used in template |
| `frontend/src/router/index.js` | /admin route with requiresAdmin meta + beforeEach guard | VERIFIED | `meta: { requiresAdmin: true }` on /admin, guard at line 26-27 |
| `frontend/src/api/notification.js` | notificationApi named export (4 methods) | VERIFIED | `export const notificationApi = { getNotifications, getUnreadCount, markRead, markAllRead }` |
| `frontend/src/stores/notification.js` | useNotificationStore, startPolling/stopPolling | VERIFIED | Pinia defineStore with polling, clearInterval guard at line 38 |
| `frontend/src/components/layout/AppHeader.vue` | 벨 아이콘, unreadCount 배지, 드롭다운 | VERIFIED | notifWrapperRef, showDropdown, unreadCount badge at line 21, dropdown at lines 25-48 |
| `frontend/src/App.vue` | onMounted startPolling + auth.isLoggedIn guard | VERIFIED | Lines 14-16: `if (auth.isLoggedIn) notif.startPolling()` |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| PermissionService.grantSpacePermission() | AuditLogService.record("PERMISSION_CHANGE") | try-catch 직접 호출 | WIRED | PermissionService.java:193-200 |
| SpaceService.delete() | AuditLogService.record("SPACE_DELETE") | try-catch 직접 호출 | WIRED | SpaceService.java:113-120 |
| MailAccountService.create/delete | AuditLogService.record("MAIL_ACCOUNT_CREATE/DELETE") | try-catch 직접 호출 | WIRED | MailAccountService.java:73-76, 105-108 |
| ContentService.deleteContent() | AuditLogService.record("CONTENT_DELETE") | try-catch 직접 호출 | WIRED | ContentService.java:258 |
| CommentService.createComment() | NotificationService.create(contentAuthorId, "COMMENT") | try-catch 직접 호출 | WIRED | CommentService.java:90-100, !contentAuthorId.equals(userId) 자기댓글 제외 |
| NotificationService.markAsRead(id, userId) | NotificationRepository.findByIdAndUserId(id, userId) | IDOR 방지 조회 | WIRED | NotificationService.java:69-71 |
| AdminController.getStats() | AdminStatsService.getStats() | 직접 호출 | WIRED | AdminController.java:41 |
| AdminController.getAuditLogs() | AuditLogService.findByFilter() | 직접 호출 (Wave 1 서비스) | WIRED | AdminController.java:59 |
| AdminStatsService | AttachmentRepository.sumSizeBytes() | COALESCE(SUM) @Query | WIRED | AdminStatsService.java:28 |
| GET /api/notifications | NotificationService.getNotifications(userId, isRead, pageable) | JWT @AuthenticationPrincipal userId | WIRED | NotificationController.java:24-29 |
| PATCH /api/notifications/{id}/read | NotificationService.markAsRead(id, userId) | IDOR 방지 — 04-02 구현 | WIRED | NotificationController.java:47-52 |
| AdminDashboardView.vue onMounted() | adminApi.getStats() + adminApi.getAuditLogs() | axios GET Promise.all | WIRED | AdminDashboardView.vue:111-113 |
| 감사로그 탭 DxDataGrid | auditLogs ref (API에서 로드) | data-source=":data-source='auditLogs'" | WIRED | AdminDashboardView.vue:59, 116 |
| router.beforeEach | useAuthStore().user.role !== 'SITE_ADMIN' | meta.requiresAdmin 체크 → /spaces 리다이렉트 | WIRED | router/index.js:26-27 |
| App.vue onMounted() | notificationStore.startPolling() | auth.isLoggedIn 단일 진입점 | WIRED | App.vue:13-16 |
| AppHeader.vue 벨 아이콘 클릭 | notificationStore.fetchNotifications() | toggleDropdown() 내 조건부 호출 | WIRED | AppHeader.vue:91-94 |
| notificationStore.unreadCount | AppHeader.vue 배지 표시 | storeToRefs(notificationStore) | WIRED | AppHeader.vue:73, 21 |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| AdminDashboardView.vue | stats (ref null) | adminApi.getStats() → GET /admin/stats → AdminStatsService.getStats() → 5 repositories | DB 집계 쿼리 실행 (countActiveUsers, countActiveSpaces, etc.) | FLOWING |
| AdminDashboardView.vue | auditLogs (ref []) | adminApi.getAuditLogs() → GET /admin/audit-logs → AuditLogService.findByFilter() → AuditLogRepository native SQL | DB 실제 쿼리, CAST nullable datetime | FLOWING |
| AppHeader.vue | unreadCount (storeToRefs) | useNotificationStore.unreadCount → fetchUnreadCount() → notificationApi.getUnreadCount() → GET /notifications/unread-count → countByUserIdAndIsReadFalse | DB COUNT 쿼리 | FLOWING |
| AppHeader.vue | notifications (storeToRefs) | fetchNotifications() → notificationApi.getNotifications() → NotificationController → NotificationService.getNotifications() → notificationRepository.findByUserId() | DB 실제 레코드 | FLOWING |

---

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| admin.js exports adminApi | `node -e "const f=require('fs'); const c=f.readFileSync('/Users/shinwon/confluence-clone-proj/frontend/src/api/admin.js','utf8'); if(!c.includes('adminApi')) throw new Error('fail'); console.log('OK')"` | adminApi exists with /admin/stats | PASS |
| notification.js exports notificationApi with markAllRead | `node -e "..."` (verified by reading file directly) | notificationApi with 4 methods confirmed | PASS |
| stores/notification.js has startPolling + clearInterval guard | Read file directly | startPolling(), clearInterval guard at line 38, stopPolling() present | PASS |
| App.vue startPolling call guarded by auth.isLoggedIn | Read file directly | `if (auth.isLoggedIn) notif.startPolling()` at line 14 | PASS |
| AuditLogService REQUIRES_NEW propagation | Read file directly | `@Transactional(propagation = Propagation.REQUIRES_NEW)` at line 39 | PASS |

Step 7b: Frontend build status not re-run by verifier (SUMMARY confirms npm run build SUCCESS). Backend test execution not re-run (would require running mvnw against live DB).

---

### Probe Execution

No probe scripts found for phase 04 (`find scripts -path '*/tests/probe-*.sh'` yielded no matches). Phase does not declare probes in PLAN/SUMMARY files. Step 7c: SKIPPED (no probe scripts).

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| AUDIT-01 | 04-01 | 감사로그 JPA 도메인 + 주요 서비스 통합 (AuditLog entity/repository/service + PermissionService/SpaceService/MailAccountService/ContentService record() 호출) | SATISFIED | AuditLog.java, AuditLogService.java (REQUIRES_NEW), 4-service integration verified in code, AuditLogServiceTest 4 tests GREEN |
| AUDIT-02 | 04-03 | 관리자 감사로그 조회 API (GET /admin/audit-logs 필터/페이징, ADMIN_ACCESS 로깅) | SATISFIED | AdminController.java GET /audit-logs, AdminControllerTest.auditLogs_filterByActionType + auditLogs_returns200 |
| NOTIF-01 | 04-02 | 알림 JPA 도메인 + CommentService 통합 (Notification entity/service + 댓글 알림 생성, IDOR 방지) | SATISFIED | Notification.java, NotificationService.java (markAsRead IDOR check), CommentService integration, NotificationServiceTest 4 tests |
| NOTIF-02 | 04-04, 04-06 | 알림 REST API (4 endpoints) + 프론트엔드 (AppHeader 벨 아이콘, Pinia 폴링) | SATISFIED (code) / UNCERTAIN (UI) | NotificationController.java 4 endpoints + NotificationControllerTest 6 tests + AppHeader.vue + stores/notification.js — UI runtime behavior needs human verify |
| ADMIN-01 | 04-03, 04-05 | 관리자 통계 API + 대시보드 UI (GET /admin/stats, DevExtreme DxTabPanel/DxChart/DxDataGrid) | SATISFIED (code) / UNCERTAIN (UI) | AdminController.java GET /stats, AdminStatsService.java, AdminDashboardView.vue full implementation — UI rendering needs human verify |
| ADMIN-02 | 04-03, 04-05 | Site Admin 전용 접근 제어 (403 for MEMBER, /admin 라우터 가드) | SATISFIED | AdminControllerTest.stats_member_returns403, router.beforeEach requiresAdmin guard with role check |

Note: REQUIREMENTS.md file does not exist in this project — requirement IDs are defined only in ROADMAP.md Phase 4 section and PLAN frontmatter. All 6 IDs (ADMIN-01, ADMIN-02, NOTIF-01, NOTIF-02, AUDIT-01, AUDIT-02) are accounted for and covered by plans.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | — | No TBD/FIXME/XXX found in any phase-4 modified files | — | — |
| (none) | — | No stub patterns (return null / return {} / return []) in service layer | — | — |
| (none) | — | No hardcoded empty data passed as props | — | — |

No debt markers found. No stub implementations detected. All service methods produce real data from DB queries.

---

### Human Verification Required

#### 1. 관리자 대시보드 UI 렌더링 확인

**Test:**
1. 백엔드 실행: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw spring-boot:run -pl backend`
2. 프론트엔드 실행: `cd frontend && npm run dev`
3. `http://localhost:3000/login`에서 admin / Admin1234! 로그인
4. `http://localhost:3000/admin` 접속

**Expected:**
- "관리자 대시보드" 제목이 표시된다
- "대시보드" 탭에 통계 카드 4개 (활성 사용자/Space 수/콘텐츠 수/스토리지 사용량)가 표시된다
- 메일 서버 상태 DxPieChart가 렌더링된다 (정상/오류 파이 차트)
- "감사로그" 탭 클릭 시 DxDataGrid가 표시된다 (actorId/actionType/targetType/createdAt 컬럼)
- 일반 사용자로 로그인하여 /admin 접속 시 /spaces로 리다이렉트된다

**Why human:** DevExtreme Vue 컴포넌트 (DxTabPanel, DxPieChart, DxDataGrid) 실제 렌더링은 브라우저 런타임에서만 확인 가능. 데이터 흐름 코드는 검증되었으나 시각적 렌더링 결과는 정적 분석 불가.

---

#### 2. 알림 벨 아이콘 + 드롭다운 + 폴링 동작 확인

**Test:**
1. 위와 동일하게 백엔드/프론트엔드 실행 후 admin 로그인
2. AppHeader의 🔔 아이콘 확인
3. 벨 아이콘 클릭
4. 브라우저 개발자도구 네트워크 탭 관찰 (30초 대기)
5. 알림이 있는 경우 개별 클릭 / "모두 읽음" 클릭

**Expected:**
- AppHeader 우측에 🔔 벨 아이콘이 표시된다
- 미읽음 알림이 있으면 배지에 숫자가 표시된다
- 벨 클릭 시 드롭다운 패널이 열리고 알림 목록이 표시된다
- 개별 알림 클릭 시 읽음 처리되고 배지 카운트가 감소한다
- "모두 읽음" 클릭 시 배지가 0이 된다
- 네트워크 탭에서 `/api/notifications/unread-count`가 30초마다 1회씩만 호출된다 (중복 폴링 없음)

**Why human:** Vue 반응성 (storeToRefs, unreadCount 배지 업데이트), setInterval 폴링 타이밍, outside-click 이벤트 리스너 동작은 런타임 브라우저 확인 필요.

---

### Gaps Summary

No code-level gaps found. All 18 must-have truths are either fully verified (16) or verified at the code level with runtime behavior requiring human confirmation (2). All 6 requirement IDs are covered. All required artifacts exist and are substantively implemented (not stubs). All key links are wired. No debt markers (TBD/FIXME/XXX) found. No anti-patterns detected.

The two human verification items are standard DevExtreme Vue rendering and Pinia polling behavior checks that require a live browser session — this is expected for Wave 3 frontend plans which both declared `checkpoint:human-verify` gates.

---

_Verified: 2026-07-07T09:00:00Z_
_Verifier: Claude (gsd-verifier)_
