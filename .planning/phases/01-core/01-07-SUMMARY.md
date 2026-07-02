---
phase: 01-core
plan: "07"
subsystem: frontend-ui
tags: [version-history, permissions, vue3, devextreme, phase1-complete]
dependency_graph:
  requires: ["01-04", "01-05", "01-06"]
  provides: [VersionHistoryPanel, SpacePermissionView, PermissionAPI]
  affects: [frontend/src/views/content/ContentView.vue, frontend/src/router/index.js]
tech_stack:
  added: []
  patterns: [DxPopup-slide-panel, DxDataGrid-crud, DxSelectBox-form, v-model-visible]
key_files:
  created:
    - frontend/src/api/permission.js
    - frontend/src/components/content/VersionHistoryPanel.vue
    - frontend/src/views/space/SpacePermissionView.vue
  modified:
    - frontend/src/views/content/ContentView.vue
    - frontend/src/views/space/SpaceHomeView.vue
    - frontend/src/router/index.js
decisions:
  - VersionHistoryPanel을 DxPopup(position right)으로 구현 — DxDrawer 대신 DxPopup 사용하여 별도 설치 없이 처리
  - 백엔드 테스트 BUILD FAILURE는 Java 버전 불일치(class 65.0 vs 61.0)이므로 기록 후 계속 진행
  - SpaceHomeView에 RouterLink로 권한 설정 링크 추가
metrics:
  duration: "~20 minutes"
  completed: "2026-07-02"
  tasks_completed: 4
  files_created: 3
  files_modified: 3
---

# Phase 1 Plan 07: Version History Panel & Space Permission View Summary

**One-liner:** DxPopup 기반 버전 기록 슬라이드 패널과 DxDataGrid 기반 Space 권한 관리 화면 구현으로 Phase 1 UI 완성

## What Was Built

### Task 1: Permission API 클라이언트 (commit: 1318075)
- `frontend/src/api/permission.js` 생성
- getSpacePermissions, grantPermission, revokePermission 3개 함수 구현
- space.js 패턴과 동일한 axios 래퍼 방식

### Task 2: VersionHistoryPanel + ContentView 업데이트 (commit: bdf3e4d)
- `frontend/src/components/content/VersionHistoryPanel.vue` 생성 (91줄)
  - props: visible(Boolean), contentId(Number)
  - emits: ['update:visible']
  - DxPopup position='right center'로 슬라이드 패널 구현
  - contentApi.getVersions() 호출 → v-for로 버전 목록 표시
  - watch(visible): visible=true 시 자동 새로고침
- `frontend/src/views/content/ContentView.vue` 업데이트
  - VersionHistoryPanel import 및 v-model:visible="showVersionHistory" 바인딩
  - "버전 기록" 버튼이 이미 @click="showVersionHistory = true" 연결됨

### Task 3: SpacePermissionView + 라우터 (commit: cf25e60)
- `frontend/src/views/space/SpacePermissionView.vue` 생성 (175줄)
  - DxDataGrid로 권한 목록 표시 (대상유형/ID/권한레벨/생성일/삭제버튼)
  - DxPopup 권한 추가 다이얼로그 (DxSelectBox x2, DxNumberBox)
  - onMounted: permissionApi.getSpacePermissions(spaceKey) 호출
- `frontend/src/router/index.js`: /spaces/:spaceKey/permissions 라우트 추가
- `frontend/src/views/space/SpaceHomeView.vue`: 권한 설정 RouterLink 추가

### Task 4: Phase 1 최종 빌드 검증 및 태그 (commit: c3e5f95)
- 프론트엔드 빌드: SUCCESS (✓ built in 3.57s)
- 백엔드 테스트: BUILD FAILURE (Java 버전 불일치 — 아래 참조)
- git tag v0.2.0-phase1 생성 완료

## Deviations from Plan

### Auto-recorded Issues

**1. [Recorded - Non-blocking] VersionHistoryPanel 위치 수정**
- **Found during:** Task 2
- **Issue:** 처음 VersionHistoryPanel을 v-if와 v-else-if 사이에 잘못 삽입
- **Fix:** empty-state div 이후로 이동 (v-if/v-else-if 체인 밖으로)
- **Files modified:** frontend/src/views/content/ContentView.vue
- **Impact:** 빌드 영향 없음 — 즉시 수정

**2. [Recorded - Non-blocking] 백엔드 테스트 BUILD FAILURE**
- **Found during:** Task 4
- **Issue:** `AuthControllerTest has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 61.0`
  - 테스트 클래스는 Java 21(65.0)로 컴파일, 실행 JVM은 Java 17(61.0)
- **Fix:** 계획 지침에 따라 기록 후 계속 진행 (빌드 성공이 primary goal)
- **Resolution path:** JVM 환경 통일 필요 (`JAVA_HOME` 설정 확인)
- **Frontend build:** 영향 없음 — SUCCESS 확인

## Commits

| Hash | Message | Task |
|------|---------|------|
| 1318075 | feat(01-07): add Permission API client | Task 1 |
| bdf3e4d | feat(01-07): add VersionHistoryPanel and wire to ContentView | Task 2 |
| cf25e60 | feat(01-07): add SpacePermissionView, permission route, space home link | Task 3 |
| c3e5f95 | feat: Phase 1 complete — core CRUD features with frontend | Task 4 |

## Success Criteria Checklist

- [x] frontend/src/api/permission.js — getSpacePermissions, grantPermission, revokePermission 존재
- [x] VersionHistoryPanel.vue — DxPopup 사용, props(visible/contentId), emits update:visible
- [x] ContentView.vue — VersionHistoryPanel v-model:visible 바인딩 완료
- [x] SpacePermissionView.vue — DxDataGrid 권한 목록, DxPopup 추가 다이얼로그
- [x] router/index.js — /spaces/:spaceKey/permissions 라우트 존재
- [x] 프론트엔드 빌드 SUCCESS
- [x] git tag v0.2.0-phase1 생성
- [ ] 백엔드 테스트 BUILD SUCCESS — DEFERRED (Java 버전 불일치)

## Known Stubs

없음 — 모든 컴포넌트가 실제 API를 호출하도록 구현됨. (API 응답 데이터 없으면 빈 목록 표시)

## Threat Flags

없음 — 새로운 네트워크 엔드포인트나 인증 경로가 백엔드에 추가되지 않았음 (프론트엔드 UI만 구현).

## Self-Check: PASSED

- [x] frontend/src/api/permission.js — 존재
- [x] frontend/src/components/content/VersionHistoryPanel.vue — 존재 (91줄, min_lines 50 충족)
- [x] frontend/src/views/space/SpacePermissionView.vue — 존재 (175줄, min_lines 60 충족)
- [x] commit 1318075 — 존재
- [x] commit bdf3e4d — 존재
- [x] commit cf25e60 — 존재
- [x] tag v0.2.0-phase1 — 존재
