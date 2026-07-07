---
phase: 04-admin
plan: "01"
subsystem: backend-auditlog
tags: [audit-log, jpa, spring-boot, integration-test]
dependency_graph:
  requires: []
  provides: [AuditLogService.record, AuditLogService.findByFilter, AuditLog entity]
  affects: [PermissionService, SpaceService, MailAccountService, ContentService]
tech_stack:
  added: []
  patterns:
    - REQUIRES_NEW propagation for audit log isolation
    - Native SQL query with CAST for nullable timestamp parameters in PostgreSQL
    - @JdbcTypeCode(SqlTypes.JSON) for JSONB column mapping with Hibernate 6
key_files:
  created:
    - backend/src/main/java/com/company/wiki/auditlog/entity/AuditLog.java
    - backend/src/main/java/com/company/wiki/auditlog/repository/AuditLogRepository.java
    - backend/src/main/java/com/company/wiki/auditlog/dto/AuditLogDto.java
    - backend/src/main/java/com/company/wiki/auditlog/service/AuditLogService.java
    - backend/src/test/java/com/company/wiki/auditlog/service/AuditLogServiceTest.java
    - backend/src/test/java/com/company/wiki/auditlog/service/AuditLogTestCleaner.java
  modified:
    - backend/src/main/java/com/company/wiki/permission/service/PermissionService.java
    - backend/src/main/java/com/company/wiki/space/service/SpaceService.java
    - backend/src/main/java/com/company/wiki/mail/service/MailAccountService.java
    - backend/src/main/java/com/company/wiki/content/service/ContentService.java
decisions:
  - AuditLogService.record() uses REQUIRES_NEW propagation so its internal failure never taints the caller's JPA session
  - Native SQL query with CAST(:from AS timestamp) to handle nullable LocalDateTime parameters in PostgreSQL (JPQL causes type inference failure)
  - @JdbcTypeCode(SqlTypes.JSON) on AuditLog.detail field to correctly map String to PostgreSQL JSONB column
  - AuditLogTestCleaner helper component with REQUIRES_NEW to clean up committed test data between integration tests
  - actorId extracted from SecurityContextHolder in PermissionService/SpaceService/MailAccountService (no signature change)
  - ContentService.deleteContent() uses userId parameter directly (already available in method signature)
metrics:
  duration: "~25 minutes"
  completed: "2026-07-07"
  tasks_completed: 2
  files_created: 6
  files_modified: 4
---

# Phase 04 Plan 01: AuditLog Backend — Entity, Service, and Integration Summary

**One-liner:** AuditLog JPA entity + REQUIRES_NEW service integrated into 4 existing services with JSONB-safe persistence and 4-test GREEN integration suite.

## What Was Built

### Task 1: AuditLog Domain (entity / repository / dto / service)

- **AuditLog.java**: JPA entity mapped to `audit_logs` table. `detail` field uses `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition="jsonb"` for correct JSONB handling. `@PrePersist` sets `createdAt`.
- **AuditLogRepository.java**: `JpaRepository<AuditLog, Long>` with `findByFilter` as a **native SQL query** using `CAST(:from AS timestamp)` / `CAST(:to AS timestamp)` to allow null parameters without PostgreSQL type inference errors.
- **AuditLogDto.java**: Inner `record Response(...)` with static factory `from(AuditLog)`.
- **AuditLogService.java**: `record()` annotated `@Transactional(propagation = REQUIRES_NEW)` — runs in its own DB connection so exceptions never taint the caller's JPA session. `findByFilter()` returns `PagedResponse<AuditLogDto.Response>`.

### Task 2: Integration into Existing Services

| Service | Method | Action Type | Notes |
|---------|--------|-------------|-------|
| PermissionService | grantSpacePermission() | PERMISSION_CHANGE | actorId from SecurityContextHolder |
| SpaceService | delete() | SPACE_DELETE | actorId from SecurityContextHolder |
| MailAccountService | create() | MAIL_ACCOUNT_CREATE | isAdminAccess=true |
| MailAccountService | delete() | MAIL_ACCOUNT_DELETE | isAdminAccess=true |
| ContentService | deleteContent() | CONTENT_DELETE | userId already in method signature |

All audit calls are wrapped in try-catch to prevent audit failures from rolling back business transactions.

## Test Results

```
AuditLogServiceTest
  ✓ record_savesAuditLogToDatabase
  ✓ record_doesNotThrowEvenOnSerializationError
  ✓ spaceDelete_createsAuditLog
  ✓ findByFilter_appliesDateRangeFilter

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] JSONB type mismatch — @JdbcTypeCode(SqlTypes.JSON) added to AuditLog.detail**
- **Found during:** Task 1 — first test run
- **Issue:** PostgreSQL rejected `character varying` value for `jsonb` column. The plan's `@Column(columnDefinition="jsonb")` alone is insufficient for Hibernate 6.
- **Fix:** Added `@JdbcTypeCode(SqlTypes.JSON)` annotation on `detail` field.
- **Files modified:** `AuditLog.java`
- **Commit:** b0695b1

**2. [Rule 1 - Bug] JPQL null parameter type inference failure for LocalDateTime**
- **Found during:** Task 1 — test 3/4 failures
- **Issue:** JPQL `(:from IS NULL OR a.createdAt >= :from)` causes PostgreSQL error `could not determine data type of parameter $5` when `from` is null.
- **Fix:** Converted to native SQL query with `CAST(:from AS timestamp)` — PostgreSQL can always infer the cast target type.
- **Files modified:** `AuditLogRepository.java`
- **Commit:** b0695b1

**3. [Rule 1 - Bug] Hibernate session taint from REQUIRES_NEW exception in same session**
- **Found during:** Task 1 — tests with subsequent `findAll()` calls
- **Issue:** `record()` with default `@Transactional` caused caught exception to taint outer JPA session, triggering `AssertionFailure: null id` on subsequent queries.
- **Fix:** Changed `record()` to `@Transactional(propagation = REQUIRES_NEW)` — separate session/transaction. Added `AuditLogTestCleaner` with REQUIRES_NEW to purge committed test data before each test.
- **Files modified:** `AuditLogService.java`
- **Files created:** `AuditLogTestCleaner.java`
- **Commit:** b0695b1

## Known Stubs

None.

## Threat Flags

No new trust boundaries introduced. All audit log writes go through JPA parameterized queries (T-04-01-02 mitigated). `AuditLogRepository` has no delete methods (T-04-01-01 mitigated). Exception isolation via REQUIRES_NEW + try-catch (T-04-01-03 mitigated).

## Self-Check: PASSED

- All 6 created files exist on disk
- Commits b0695b1 and 8f47270 verified in git log
- AuditLogServiceTest: 4/4 tests GREEN (BUILD SUCCESS)
- mvnw compile: BUILD SUCCESS
