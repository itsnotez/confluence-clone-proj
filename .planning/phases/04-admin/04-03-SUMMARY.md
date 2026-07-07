---
phase: 04-admin
plan: "03"
subsystem: backend-admin
tags: [admin, stats-api, audit-log, spring-boot, integration-test]
dependency_graph:
  requires: [04-01]
  provides: [AdminController GET /admin/stats, AdminController GET /admin/audit-logs]
  affects: [AuditLogService, AttachmentRepository, SpaceRepository, ContentRepository, UserRepository, MailAccountRepository]
tech_stack:
  added: []
  patterns:
    - Admin controller with SecurityConfig ROLE_SITE_ADMIN boundary (no @PreAuthorize needed)
    - COALESCE(SUM) JPQL for nullable aggregate on AttachmentRepository
    - countBySyncStatus() Spring Data derived query for MailAccountRepository
    - ADMIN_ACCESS audit log recording with try-catch isolation in controller
key_files:
  created:
    - backend/src/main/java/com/company/wiki/admin/dto/AdminStatsDto.java
    - backend/src/main/java/com/company/wiki/admin/service/AdminStatsService.java
    - backend/src/main/java/com/company/wiki/admin/controller/AdminController.java
    - backend/src/test/java/com/company/wiki/admin/controller/AdminControllerTest.java
  modified:
    - backend/src/main/java/com/company/wiki/attachment/repository/AttachmentRepository.java
    - backend/src/main/java/com/company/wiki/user/repository/UserRepository.java
    - backend/src/main/java/com/company/wiki/space/repository/SpaceRepository.java
    - backend/src/main/java/com/company/wiki/content/repository/ContentRepository.java
    - backend/src/main/java/com/company/wiki/mail/repository/MailAccountRepository.java
decisions:
  - AdminController relies on SecurityConfig /admin/** ROLE_SITE_ADMIN rule — no @PreAuthorize annotation added (plan requirement honored)
  - AdminStatsService uses @Transactional(readOnly=true) for multi-repository read aggregation
  - AttachmentRepository.sumSizeBytes() uses COALESCE(SUM) JPQL to return 0 when no attachments exist
  - countBySyncStatus() added as Spring Data derived query (avoids @Query boilerplate, semantically equivalent)
  - ADMIN_ACCESS audit log recorded in controller with try-catch — audit failure never blocks stats/logs response
metrics:
  duration: "~3 minutes"
  completed: "2026-07-07"
  tasks_completed: 2
  files_created: 4
  files_modified: 5
---

# Phase 04 Plan 03: Admin Stats API and Audit Log Query API Summary

**One-liner:** AdminController with GET /admin/stats (AdminStatsDto aggregation) and GET /admin/audit-logs (AuditLogService.findByFilter delegation), protected by SecurityConfig SITE_ADMIN boundary, with 5-test GREEN integration suite.

## What Was Built

### Task 1: AdminStatsDto, AdminStatsService, Repository Count Methods

- **AdminStatsDto.java**: Lombok `@Builder @Getter @NoArgsConstructor @AllArgsConstructor` class in `com.company.wiki.admin.dto`. Fields: `activeUsers`, `totalSpaces`, `totalContents`, `storageUsedBytes`, `mailAccountsOk`, `mailAccountsFailed` (all `long`).
- **AdminStatsService.java**: `@Service @RequiredArgsConstructor @Transactional(readOnly=true)`. `getStats()` calls all 5 repositories and assembles AdminStatsDto via builder.
- **AttachmentRepository**: Added `@Query("SELECT COALESCE(SUM(a.sizeBytes), 0) FROM Attachment a") long sumSizeBytes()` — returns 0 when table is empty.
- **UserRepository**: Added `@Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'") long countActiveUsers()`.
- **SpaceRepository**: Added `@Query("SELECT COUNT(s) FROM Space s WHERE s.deletedAt IS NULL") long countActiveSpaces()`.
- **ContentRepository**: Added `@Query("SELECT COUNT(c) FROM Content c WHERE c.deletedAt IS NULL") long countActiveContents()`.
- **MailAccountRepository**: Added `long countBySyncStatus(String syncStatus)` (Spring Data derived query — no @Query needed).

### Task 2: AdminController + AdminControllerTest

- **AdminController.java**: `@RestController @RequestMapping("/admin") @RequiredArgsConstructor`.
  - `GET /stats` → calls `adminStatsService.getStats()`, records `ADMIN_ACCESS` audit log.
  - `GET /audit-logs` → delegates to `auditLogService.findByFilter(actorId, actionType, from, to, pageable)` with all optional query params, records `ADMIN_ACCESS` audit log.
  - Both endpoints: no `@PreAuthorize` — SecurityConfig already enforces `hasRole("SITE_ADMIN")` on `/admin/**`.
- **AdminControllerTest.java**: `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("local") @Transactional`.
  - `stats_siteAdmin_returns200` — 200 + `data.activeUsers` exists.
  - `stats_member_returns403` — MEMBER token → 403.
  - `stats_unauthenticated_returns401` — no token → 401.
  - `auditLogs_returns200` — 200 + `data.content` is array.
  - `auditLogs_filterByActionType` — `?actionType=SPACE_DELETE` → 200.

## Test Results

```
AdminControllerTest
  ✓ stats_siteAdmin_returns200
  ✓ stats_member_returns403
  ✓ stats_unauthenticated_returns401
  ✓ auditLogs_returns200
  ✓ auditLogs_filterByActionType

Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Deviations from Plan

None — plan executed exactly as written. All repository methods were added as specified (countActiveUsers via @Query, countActiveSpaces via @Query, countActiveContents via @Query, sumSizeBytes via @Query, countBySyncStatus via derived query). AdminController does not use @PreAuthorize as the plan specified SecurityConfig covers the boundary.

## Known Stubs

None.

## Threat Flags

No new trust boundaries introduced beyond what is documented in the plan's threat model. All admin endpoints remain behind `SecurityConfig /admin/** hasRole("SITE_ADMIN")` (T-04-03-01, T-04-03-02 mitigated). JPQL @Query uses parameterized binding — no SQL injection risk (T-04-03-03 mitigated).

## Self-Check: PASSED

- AdminStatsDto.java created: verified
- AdminStatsService.java created: verified
- AdminController.java created: verified
- AdminControllerTest.java created: verified
- Task 1 commit 819c15d verified in git log
- Task 2 commit e341c3e verified in git log
- AdminControllerTest: 5/5 tests GREEN (BUILD SUCCESS)
- mvnw compile: BUILD SUCCESS (97 source files)
