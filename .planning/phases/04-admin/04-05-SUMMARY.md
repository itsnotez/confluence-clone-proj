---
phase: 04-admin
plan: "05"
subsystem: frontend-admin
tags: [admin, dashboard, devextreme, vue3, router-guard, audit-log]
dependency_graph:
  requires: [04-03]
  provides: [AdminDashboardView, adminApi, requiresAdmin router guard]
  affects: [frontend/src/api/admin.js, frontend/src/views/admin/AdminDashboardView.vue, frontend/src/router/index.js]
tech_stack:
  added: []
  patterns:
    - DevExtreme DxTabPanel with named slot template (two tabs)
    - DxPieChart with computed data from API response
    - DxDataGrid for audit logs with datetime column
    - named export adminApi pattern (same as mail.js)
    - router.beforeEach requiresAdmin guard with role check
key_files:
  created:
    - frontend/src/api/admin.js
  modified:
    - frontend/src/views/admin/AdminDashboardView.vue
    - frontend/src/router/index.js
decisions:
  - adminApi uses named export pattern (consistent with mailAccountApi/mailMessageApi in mail.js)
  - requiresAdmin guard checks auth.user?.role — if user is null (no session yet), redirects to /spaces; isLoggedIn guard fires first for unauthenticated users
  - onMounted loads stats and auditLogs in parallel with Promise.all for faster initial render
  - DxPieChart mailStatusData computed from stats.mailAccountsOk / mailAccountsFailed
  - audit-log detail column omitted from DxDataGrid per threat model T-04-05-03
metrics:
  duration: "~2 minutes"
  completed: "2026-07-07"
  tasks_completed: 2
  tasks_total: 3
  files_created: 1
  files_modified: 2
---

# Phase 04 Plan 05: Admin Dashboard Frontend Summary

**One-liner:** AdminDashboardView with DxTabPanel(대시보드/감사로그) + 4 stats cards + DxPieChart (mail server status) + DxDataGrid (audit logs), plus requiresAdmin router guard blocking non-SITE_ADMIN users.

## What Was Built

### Task 1: admin.js API Module + Router Site Admin Guard

- **frontend/src/api/admin.js**: Named export `adminApi` with `getStats()` (GET /admin/stats) and `getAuditLogs(params)` (GET /admin/audit-logs). Follows mail.js named-export pattern.
- **frontend/src/router/index.js**: Added `meta: { requiresAdmin: true }` to `/admin` route. Added guard in `router.beforeEach`: if `to.meta.requiresAdmin` and `auth.user?.role !== 'SITE_ADMIN'`, redirects to `/spaces`.

### Task 2: AdminDashboardView.vue Full Implementation

- **`<script setup>` Composition API**: imports `adminApi`, `DxTabPanel`/`DxItem`, `DxPieChart`/`DxSeries`, `DxDataGrid`/`DxColumn`/`DxPaging`.
- **State**: `stats` (ref null), `auditLogs` (ref []), `auditLogTotal` (ref 0), `loading` (ref false).
- **`onMounted`**: `Promise.all([adminApi.getStats(), adminApi.getAuditLogs({ page: 0, size: 20 })])` — assigns `stats.value` and `auditLogs.value`.
- **Template**: `<h1>관리자 대시보드</h1>` + DxTabPanel with two DxItem tabs.
  - Tab 1 "대시보드": 4 stat cards (활성 사용자, Space 수, 콘텐츠 수, 스토리지 사용량 via `formatBytes`) + `DxPieChart` with `mailStatusData` computed (정상/오류 counts).
  - Tab 2 "감사로그": `DxDataGrid` with columns actorId, actionType, targetType, targetId, isAdminAccess, createdAt (datetime) + `DxPaging(:page-size="20")`.
- **Build**: `npm run build` succeeds — `AdminDashboardView-BybnJwt_.js` 394.70 kB chunk generated.

### Task 3: checkpoint:human-verify

Paused at checkpoint — visual verification required by human.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None. All data flows from `adminApi.getStats()` and `adminApi.getAuditLogs()` — no hardcoded placeholders.

## Threat Flags

No new trust boundaries beyond plan's threat model. T-04-05-01 (requiresAdmin guard) mitigated. T-04-05-03 (detail column omitted from DxDataGrid) honored.

## Self-Check: PASSED

- frontend/src/api/admin.js: created
- frontend/src/views/admin/AdminDashboardView.vue: updated (stub → full impl)
- frontend/src/router/index.js: updated (requiresAdmin meta + beforeEach guard)
- Task 1 commit 3e191b8: verified
- Task 2 commit 2fbb4e0: verified
- npm run build: SUCCESS (AdminDashboardView chunk generated)
