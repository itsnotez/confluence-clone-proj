---
phase: 04-admin
reviewed: 2026-07-07T00:00:00Z
depth: standard
files_reviewed: 35
files_reviewed_list:
  - backend/src/main/java/com/company/wiki/admin/controller/AdminController.java
  - backend/src/main/java/com/company/wiki/admin/dto/AdminStatsDto.java
  - backend/src/main/java/com/company/wiki/admin/service/AdminStatsService.java
  - backend/src/main/java/com/company/wiki/attachment/repository/AttachmentRepository.java
  - backend/src/main/java/com/company/wiki/auditlog/dto/AuditLogDto.java
  - backend/src/main/java/com/company/wiki/auditlog/entity/AuditLog.java
  - backend/src/main/java/com/company/wiki/auditlog/repository/AuditLogRepository.java
  - backend/src/main/java/com/company/wiki/auditlog/service/AuditLogService.java
  - backend/src/main/java/com/company/wiki/comment/service/CommentService.java
  - backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java
  - backend/src/main/java/com/company/wiki/content/repository/ContentRepository.java
  - backend/src/main/java/com/company/wiki/content/service/ContentService.java
  - backend/src/main/java/com/company/wiki/mail/repository/MailAccountRepository.java
  - backend/src/main/java/com/company/wiki/mail/service/MailAccountService.java
  - backend/src/main/java/com/company/wiki/notification/controller/NotificationController.java
  - backend/src/main/java/com/company/wiki/notification/dto/NotificationDto.java
  - backend/src/main/java/com/company/wiki/notification/entity/Notification.java
  - backend/src/main/java/com/company/wiki/notification/repository/NotificationRepository.java
  - backend/src/main/java/com/company/wiki/notification/service/NotificationService.java
  - backend/src/main/java/com/company/wiki/permission/service/PermissionService.java
  - backend/src/main/java/com/company/wiki/space/repository/SpaceRepository.java
  - backend/src/main/java/com/company/wiki/space/service/SpaceService.java
  - backend/src/main/java/com/company/wiki/user/repository/UserRepository.java
  - backend/src/test/java/com/company/wiki/admin/controller/AdminControllerTest.java
  - backend/src/test/java/com/company/wiki/auditlog/service/AuditLogServiceTest.java
  - backend/src/test/java/com/company/wiki/auditlog/service/AuditLogTestCleaner.java
  - backend/src/test/java/com/company/wiki/notification/controller/NotificationControllerTest.java
  - backend/src/test/java/com/company/wiki/notification/service/NotificationServiceTest.java
  - frontend/src/api/admin.js
  - frontend/src/api/notification.js
  - frontend/src/App.vue
  - frontend/src/components/layout/AppHeader.vue
  - frontend/src/router/index.js
  - frontend/src/stores/notification.js
  - frontend/src/views/admin/AdminDashboardView.vue
findings:
  critical: 3
  warning: 5
  info: 3
  total: 11
status: issues_found
---

# Phase 04: Code Review Report

**Reviewed:** 2026-07-07T00:00:00Z
**Depth:** standard
**Files Reviewed:** 35
**Status:** issues_found

## Summary

This review covers the admin dashboard, audit log, and notification subsystems introduced in phase 04. The backend security layer (JWT + Spring Security) is structurally sound and the IDOR protection on notifications is correctly implemented. However, three blockers were found: a broken notification deep-link (numeric spaceId embedded in a URL that the router expects to resolve with a string spaceKey), an incorrect mail-failure stat counter (querying "DISABLED" instead of "FAILED"), and an unhandled promise rejection in the notification store that can silently crash the dropdown. Five warnings cover latent runtime failures (native query sorting, null-safety, unread count undercount) and a missing router guard meta handler.

---

## Critical Issues

### CR-01: Notification linkUrl Uses spaceId Instead of spaceKey — Deep Links Are Broken

**File:** `backend/src/main/java/com/company/wiki/comment/service/CommentService.java:98`

**Issue:** The notification `linkUrl` is built as `/spaces/{spaceId}/contents/{contentId}` where `spaceId` is a numeric database primary key (e.g., `/spaces/3/contents/17`). The Vue Router only defines the path pattern `/spaces/:spaceKey/contents/:contentId` where `:spaceKey` is the string key (e.g., `WIKI`, `HR`). When the user clicks this notification and `router.push(n.linkUrl)` is called in `AppHeader.vue:103`, the router will match `:spaceKey` against the numeric ID, pass it to `ContentView.vue`, and the subsequent API call `GET /spaces/3` will fail with 404 because the space API is keyed on the string `spaceKey` column. All comment notifications deliver dead links.

**Fix:** Resolve the spaceKey before building the linkUrl. The `Content` entity carries the `Space` relationship through `spaceId`, so look up the `Space` to get `spaceKey`:

```java
// CommentService.java — in createComment(), replace line 98:
Space space = spaceRepository.findById(content.getSpaceId())
        .orElse(null);
String spaceKey = space != null ? space.getSpaceKey() : String.valueOf(content.getSpaceId());
notificationService.create(
        contentAuthorId,
        "COMMENT",
        "새 댓글이 달렸습니다",
        content.getTitle() + "에 댓글이 달렸습니다.",
        "/spaces/" + spaceKey + "/contents/" + contentId
);
```

Inject `SpaceRepository` into `CommentService` (it is not currently a dependency). Alternatively, if the `Content` entity can be made to carry the `Space` object via a join, use that instead.

---

### CR-02: AdminStatsService Counts "DISABLED" Mail Accounts as Failures — "ACTIVE" Stat May Also Be Wrong

**File:** `backend/src/main/java/com/company/wiki/admin/service/AdminStatsService.java:29-30`

**Issue:** `mailAccountsOk` queries `countBySyncStatus("ACTIVE")` and `mailAccountsFailed` queries `countBySyncStatus("DISABLED")`. The `MailAccount.prePersist()` sets the initial `syncStatus` to `"PENDING"`, and `MailSyncService` sets it to `"ACTIVE"` on success or `"DISABLED"` on repeated failure. Newly created accounts in `"PENDING"` state and any accounts in intermediate states (if they exist) are counted in neither bucket. More critically, a `"DISABLED"` account is one that has been intentionally disabled after repeated failures — but real transient sync failures are not captured here because there is no `"FAILED"` status in the lifecycle. The dashboard will show 0 "failed" accounts for any account that has not yet hit 3 consecutive failures, silently hiding accounts that are actively failing. The `mailAccountsOk` count will similarly never include `PENDING` accounts even if they are working.

**Fix:** Either (a) align the stat labels with the actual status lifecycle by adding `PENDING` accounts to a third category, or (b) ensure the dashboard documentation reflects that `mailAccountsFailed` means "permanently disabled". At minimum, rename the stat field or the query to avoid the misleading impression:

```java
// AdminStatsService.java
long mailAccountsOk      = mailAccountRepository.countBySyncStatus("ACTIVE");
long mailAccountsPending  = mailAccountRepository.countBySyncStatus("PENDING");
long mailAccountsDisabled = mailAccountRepository.countBySyncStatus("DISABLED");
```

And update `AdminStatsDto` accordingly so the frontend can display an accurate picture.

---

### CR-03: `fetchNotifications()` in Notification Store Has No Error Handling — Unhandled Promise Rejection Crashes the Dropdown

**File:** `frontend/src/stores/notification.js:19-22`

**Issue:** `fetchNotifications` has no `try/catch`. If the API call fails (network error, 401 after token expiry, 5xx), the promise rejection propagates uncaught to `toggleDropdown` in `AppHeader.vue`, which also has no `try/catch` (line 93). The unhandled rejection will appear as a JavaScript console error and, depending on the browser/Vue error boundary, can freeze the dropdown in an indeterminate state or prevent it from opening at all. `fetchUnreadCount` (line 10) is wrapped in `try/catch` — the same pattern must be applied to `fetchNotifications`.

**Fix:**

```js
// frontend/src/stores/notification.js
async function fetchNotifications() {
  try {
    const { data } = await notificationApi.getNotifications({ page: 0, size: 20 })
    notifications.value = data.data.content
  } catch (e) {
    // 알림 목록 로드 실패 — 빈 목록 유지
  }
}
```

---

## Warnings

### WR-01: Native Query with Hardcoded ORDER BY Will Fail at Runtime If Client Passes a `?sort=` Parameter

**File:** `backend/src/main/java/com/company/wiki/auditlog/repository/AuditLogRepository.java:14-32`

**Issue:** The `findByFilter` query is a `nativeQuery = true` with a hardcoded `ORDER BY a.created_at DESC`. Spring Data JPA with Hibernate 6 (Spring Boot 3.3.1) does not support applying `Pageable` sort to native queries. If a client sends `GET /admin/audit-logs?sort=actionType,asc`, Hibernate will throw `org.springframework.data.jpa.repository.query.InvalidJpaQueryMethodException` or a related runtime exception because it cannot inject the sort clause into a native SQL string that already contains an `ORDER BY`. The `Pageable` parameter is injected directly from the controller without any sort validation.

**Fix:** Either document and enforce that `sort` must not be passed to this endpoint, or replace the native query with a JPQL query which supports `Pageable` sort natively:

```java
// AuditLogRepository.java — JPQL alternative
@Query("SELECT a FROM AuditLog a " +
       "WHERE (:actorId IS NULL OR a.actorId = :actorId) " +
       "AND (:actionType IS NULL OR a.actionType = :actionType) " +
       "AND (:from IS NULL OR a.createdAt >= :from) " +
       "AND (:to IS NULL OR a.createdAt <= :to)")
Page<AuditLog> findByFilter(
        @Param("actorId") Long actorId,
        @Param("actionType") String actionType,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
);
```

If the native query is retained for dialect-specific reasons, add `@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)` in the controller and reject requests with non-default sort to fail safely.

---

### WR-02: `content.getCreatedBy()` Called Without Null Guard in CommentService — NPE if createdBy Is Lazily Unloaded

**File:** `backend/src/main/java/com/company/wiki/comment/service/CommentService.java:91`

**Issue:** `content.getCreatedBy().getId()` is called at line 91 with no null check. The `Content.createdBy` is `@ManyToOne(fetch = FetchType.LAZY)` with `nullable = false` at the DB level. However, the content is loaded via `contentRepository.findByIdAndDeletedAtIsNull(contentId)` which returns a `Content` entity where `createdBy` is a lazy proxy. If the transaction is not active at the point of access (e.g., due to transaction boundary issues or test data without a creator), this will throw a `LazyInitializationException` or `NullPointerException`. Additionally, `nullable = false` is a JPA constraint hint and does not prevent the proxy from being null in edge cases (e.g., orphaned data, manual DB mutations).

**Fix:** Add a null guard:

```java
// CommentService.java line 90-103
try {
    User contentAuthor = content.getCreatedBy();
    if (contentAuthor != null) {
        Long contentAuthorId = contentAuthor.getId();
        if (!contentAuthorId.equals(userId)) {
            notificationService.create(
                    contentAuthorId,
                    "COMMENT",
                    "새 댓글이 달렸습니다",
                    content.getTitle() + "에 댓글이 달렸습니다.",
                    "/spaces/" + content.getSpaceId() + "/contents/" + contentId
            );
        }
    }
} catch (Exception e) {
    log.warn("알림 생성 실패 (비중단): {}", e.getMessage());
}
```

---

### WR-03: `unreadCount` Can Become Stale/Negative When `markRead` Is Called on a Notification Already Read

**File:** `frontend/src/stores/notification.js:24-28`

**Issue:** `markRead(id)` always calls `unreadCount.value--` (line 27) as long as `unreadCount.value > 0`, regardless of whether the notification being marked was actually unread. If `notifications` array was fetched earlier and contains a mix of read and unread items, and a notification that is already `isRead = true` somehow reaches `handleNotifClick` (e.g., a race between two browser tabs), the count decrements incorrectly. More concretely: the guard in `AppHeader.vue:98` (`if (!n.isRead)`) prevents the extra call in single-tab usage, but the guard is on the component side — the store `markRead` function itself should be authoritative.

**Fix:** Check the local notification state before decrementing:

```js
// frontend/src/stores/notification.js
async function markRead(id) {
  const target = notifications.value.find(n => n.id === id)
  await notificationApi.markRead(id)
  notifications.value = notifications.value.map(n => n.id === id ? { ...n, isRead: true } : n)
  if (target && !target.isRead && unreadCount.value > 0) unreadCount.value--
}
```

---

### WR-04: `formatBytes` in AdminDashboardView Has No GB/TB Branch — Large Storage Values Are Misrepresented

**File:** `frontend/src/views/admin/AdminDashboardView.vue:101-106`

**Issue:** The `formatBytes` function handles B, KB, and MB but has no branch for GB or TB. If `storageUsedBytes` is, for example, 5 GB (5,368,709,120 bytes), the function returns `5120.0 MB` instead of `5.0 GB`. For a system that stores file attachments, storage totals exceeding 1 GB are realistic. The dashboard stat card will display a misleadingly large MB number.

**Fix:**

```js
function formatBytes(bytes) {
  if (bytes == null) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}
```

---

### WR-05: Router `meta: { requiresAuth: true }` on MailBox Route Is Never Checked — Dead Guard Meta

**File:** `frontend/src/router/index.js:13`

**Issue:** The `MailBoxView` route declares `meta: { requiresAuth: true }` but the `beforeEach` guard never reads `to.meta.requiresAuth`. The guard checks `to.meta.public` and `to.meta.requiresAdmin` only. The MailBox route IS implicitly protected (it has no `meta.public`, so the catch-all `!to.meta.public && !auth.isLoggedIn` guard applies), but the explicit `requiresAuth` meta is misleading: it signals a distinct guard layer that does not exist. A future developer seeing `requiresAuth: true` may incorrectly believe this route has special protection beyond basic auth, or may add a new route with only `requiresAuth: true` expecting it to work, and receive no protection.

**Fix:** Either add the `requiresAuth` handler to `beforeEach`, or remove the dead meta field:

```js
// Option A: Remove dead meta
{ path: '/spaces/:spaceKey/mail', name: 'MailBox', component: () => import('@/views/mail/MailBoxView.vue') },

// Option B: Add explicit handler in beforeEach (for future clarity)
if (to.meta.requiresAuth && !auth.isLoggedIn) return '/login'
```

---

## Info

### IN-01: `auditLogTotal` Declared and Populated but Never Used in Template

**File:** `frontend/src/views/admin/AdminDashboardView.vue:90,117`

**Issue:** `const auditLogTotal = ref(0)` is set to `logsRes.data.data.totalElements` but is never referenced in the template or any computed. The audit log DxDataGrid uses client-side paging (`DxPaging :page-size="20"`) over the initial 20 records, not server-side pagination. The total element count is fetched and discarded. This also means the DxDataGrid only ever shows the first 20 audit logs with no way to page beyond them.

**Fix:** Remove the unused `auditLogTotal` ref, or implement server-side pagination in the DxDataGrid using `DxRemoteOperations` and a custom `DataSource`.

---

### IN-02: Self-Audit Logging in AdminController Is Redundant and Adds DB Round-Trips

**File:** `backend/src/main/java/com/company/wiki/admin/controller/AdminController.java:35-40,53-58`

**Issue:** Every call to `GET /admin/stats` and `GET /admin/audit-logs` creates a new `AuditLog` record via `auditLogService.record()`. This means that viewing audit logs generates yet another audit log entry, causing the audit log to grow rapidly with self-referential `ADMIN_ACCESS` entries. Each page load of the admin dashboard generates at least two extra DB writes (one for stats, one for audit-logs). While the try-catch prevents failure, the log table will be polluted with noise that makes real audit events harder to find.

**Fix:** Either remove the audit logging for `GET /admin/stats` / `GET /admin/audit-logs` (read-only admin views are typically not audit-worthy), or add a filter in `findByFilter` to exclude `ADMIN_ACCESS` action types from the default audit log view.

---

### IN-03: `console.error` in AdminDashboardView onMounted Catch Block

**File:** `frontend/src/views/admin/AdminDashboardView.vue:119`

**Issue:** `console.error('관리자 데이터 로딩 실패:', e)` is left in the production error handler. This exposes potentially sensitive error details (stack traces, API response body) in browser devtools. The existing `v-else` fallback (`데이터를 불러올 수 없습니다.`) handles the user-facing display correctly, but the `console.error` should not remain in shipped code.

**Fix:** Remove the `console.error` or replace it with a structured logging call if the project has a frontend logger. At minimum, only log `e.message` rather than the full error object:

```js
} catch (e) {
  // console.error removed — fallback UI handles the empty state
} finally {
```

---

_Reviewed: 2026-07-07T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
