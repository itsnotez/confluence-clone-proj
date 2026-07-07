---
phase: 4
slug: 04-admin
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-07
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + MockMvc |
| **Config file** | none — Spring Boot auto-config |
| **Quick run command** | `cd backend && export JAVA_HOME="/opt/homebrew/opt/openjdk@21" && export PATH="$JAVA_HOME/bin:$PATH" && ./mvnw test -Dtest={TestClass} -Dspring.profiles.active=local` |
| **Full suite command** | `cd backend && export JAVA_HOME="/opt/homebrew/opt/openjdk@21" && export PATH="$JAVA_HOME/bin:$PATH" && ./mvnw test -Dspring.profiles.active=local` |
| **Estimated runtime** | ~90 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick test for the relevant test class
- **After every plan wave:** Run full suite
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 04-01 | 1 | AUDIT-01 | T-04-01-01 | AuditLog 삭제 불가 | integration | `-Dtest=AuditLogServiceTest` | ❌ W0 | ⬜ pending |
| 04-01-02 | 04-01 | 1 | AUDIT-01 | T-04-01-03 | 감사로그 실패 시 트랜잭션 롤백 없음 | integration | `-Dtest=AuditLogServiceTest` | ❌ W0 | ⬜ pending |
| 04-02-01 | 04-02 | 1 | NOTIF-01 | T-04-02-01 | 다른 사용자 알림 읽음처리 차단(IDOR) | integration | `-Dtest=NotificationServiceTest` | ❌ W0 | ⬜ pending |
| 04-03-01 | 04-03 | 2 | ADMIN-02, AUDIT-02 | — | MEMBER 역할로 /admin/** 접근 시 403 | integration | `-Dtest=AdminControllerTest` | ❌ W0 | ⬜ pending |
| 04-04-01 | 04-04 | 2 | NOTIF-02 | T-04-02-02 | 인증 없이 알림 조회 시 401 | integration | `-Dtest=NotificationControllerTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `AuditLogServiceTest.java` — AUDIT-01
- [ ] `NotificationServiceTest.java` — NOTIF-01
- [ ] `AdminControllerTest.java` — ADMIN-01, ADMIN-02, AUDIT-02
- [ ] `NotificationControllerTest.java` — NOTIF-02

All Wave 0 test stubs are created within each plan's tasks.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| 관리자 대시보드 DevExtreme Chart 렌더링 확인 | ADMIN-01 | Vue 렌더링 | 브라우저에서 /admin 접속 후 차트 표시 확인 |
| AppHeader 알림 벨 아이콘 + 드롭다운 동작 확인 | NOTIF-02 | Vue 렌더링 | 댓글 생성 후 헤더 배지 카운트 증가 확인 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
