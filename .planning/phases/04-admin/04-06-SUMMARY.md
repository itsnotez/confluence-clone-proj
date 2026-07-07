---
phase: 04-admin
plan: "06"
subsystem: notification-frontend
tags: [notification, pinia, vue, polling, appheader, frontend]
dependency_graph:
  requires:
    - phase: 04-04
      provides: [NotificationController, GET /notifications, GET /notifications/unread-count, PATCH /notifications/{id}/read, PATCH /notifications/read-all]
  provides: [notificationApi, useNotificationStore, AppHeader notification bell, polling]
  affects: []
tech_stack:
  added: []
  patterns: [Pinia-CompositionAPI, storeToRefs, setInterval-polling, outside-click-handler]
key_files:
  created:
    - frontend/src/api/notification.js
    - frontend/src/stores/notification.js
  modified:
    - frontend/src/components/layout/AppHeader.vue
    - frontend/src/App.vue
key_decisions:
  - "AppHeader.vue outside-click 닫기 — document.addEventListener('click') + ref wrapper로 구현 (DevExtreme Popover 대신 순수 Vue)"
  - "App.vue 단일 startPolling() 진입점 — AppHeader가 아닌 App.vue onMounted에서 호출해 중복 폴링 방지"
  - "pollTimer clearInterval 중복 등록 방지 — startPolling() 호출 전 기존 timer clearInterval"
patterns-established:
  - "Pinia store polling 패턴: startPolling/stopPolling + clearInterval guard"
  - "App.vue watch(auth.isLoggedIn) → startPolling/stopPolling 로그인 상태 연동"
requirements-completed: [NOTIF-02]
duration: 8min
completed: "2026-07-07"
---

# Phase 4 Plan 06: Notification Frontend Summary

**notificationApi + useNotificationStore(30초 폴링) + AppHeader 벨 아이콘/드롭다운 패널 구현 완료 — Wave 3 알림 프론트엔드 완성**

## Performance

- **Duration:** ~8 min
- **Completed:** 2026-07-07
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- `frontend/src/api/notification.js` — named export `notificationApi` (4개 메서드)
- `frontend/src/stores/notification.js` — `useNotificationStore` with unreadCount, notifications[], startPolling/stopPolling (clearInterval 중복 방지), fetchUnreadCount try-catch
- `AppHeader.vue` — 벨 아이콘 + unreadCount 배지 + 드롭다운 패널 (목록, markRead, markAllRead, outside-click 닫기)
- `App.vue` — onMounted startPolling(isLoggedIn guard) + watch(isLoggedIn) + onUnmounted stopPolling
- `npm run build` 에러 없이 성공

## Task Commits

1. **feat(04-06): add notification API module and Pinia store** — `9684fe6`
2. **feat(04-06): add notification bell icon + dropdown to AppHeader, start polling in App.vue** — `7253116`

## Files Created/Modified

- `frontend/src/api/notification.js` — notificationApi named export
- `frontend/src/stores/notification.js` — useNotificationStore, polling, markRead/markAllRead
- `frontend/src/components/layout/AppHeader.vue` — 벨 아이콘 + 드롭다운 통합
- `frontend/src/App.vue` — 폴링 단일 진입점

## Decisions Made

- **outside-click 닫기:** DevExtreme Popover 없이 `document.addEventListener('click')` + `notifWrapperRef`로 순수 Vue 구현
- **startPolling 단일 진입점:** `App.vue onMounted`에서만 호출 — AppHeader는 폴링 관여 없음
- **clearInterval 중복 방지:** `startPolling()` 내부에서 기존 `pollTimer` 확인 후 clearInterval 후 재등록

## Deviations from Plan

None — plan executed exactly as written. AppHeader.vue 경로가 `src/components/layout/AppHeader.vue`임을 확인하여 올바른 파일 수정.

## Threat Model Coverage

| Threat ID | Mitigation | Status |
|-----------|-----------|--------|
| T-04-06-01 | startPolling() clearInterval 중복 방지 + App.vue 단일 진입점 | DONE |
| T-04-06-02 | router.push(n.linkUrl) — Vue Router 내부 경로만 처리 | DONE |
| T-04-06-03 | 백엔드 NotificationService.markAsRead() userId 검증 (04-02) | DONE (이전 플랜) |

## Known Stubs

None.

## Self-Check: PASSED

- `frontend/src/api/notification.js` — EXISTS
- `frontend/src/stores/notification.js` — EXISTS
- `frontend/src/components/layout/AppHeader.vue` — EXISTS (updated)
- `frontend/src/App.vue` — EXISTS (updated)
- Commit `9684fe6` — EXISTS
- Commit `7253116` — EXISTS
- Build: SUCCESS (✓ built in 4.30s)
