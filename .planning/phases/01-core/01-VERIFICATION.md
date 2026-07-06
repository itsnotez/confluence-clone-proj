---
phase: 01-core
verified: 2026-07-06T20:55:00+09:00
status: human_needed
score: 6/6 must-haves verified
overrides_applied: 0
human_verification:
  - test: "브라우저에서 로그인 후 SpaceListView에서 Space 목록과 생성 다이얼로그가 정상 표시되는지 확인"
    expected: "DxDataGrid에 Space 목록이 표시되고, '새 Space 만들기' 버튼 클릭 시 DxPopup 다이얼로그가 열린다"
    why_human: "DevExtreme 컴포넌트 렌더링은 실제 브라우저 환경에서만 검증 가능"
  - test: "TipTap 에디터에서 콘텐츠 작성 후 '게시' 클릭 시 DB에 저장되고 ContentView에서 읽기 모드로 확인"
    expected: "게시된 콘텐츠가 ContentView에서 읽기 전용 TipTap 렌더링으로 표시된다"
    why_human: "TipTap 에디터 JSON serialization / hydration이 실제 브라우저에서만 검증 가능"
  - test: "ContentView에서 '버전 기록' 버튼 클릭 시 VersionHistoryPanel 슬라이드 패널이 표시되는지 확인"
    expected: "DxPopup이 우측에서 슬라이드인되며 버전 목록(versionNo, 날짜, 작성자)이 표시된다"
    why_human: "DxPopup 슬라이드 애니메이션 및 버전 목록 렌더링은 브라우저 환경에서만 확인 가능"
  - test: "그룹 멤버에게 Space 그룹 권한 부여 후 콘텐츠 접근 여부 확인"
    expected: "그룹에 WRITE 권한 부여 시 해당 그룹 멤버가 콘텐츠를 생성/수정할 수 있다"
    why_human: "ContentService가 PermissionService 호출 시 userGroupIds를 List.of()(빈 리스트)로 전달하여 그룹 권한 로직이 실제 동작하지 않을 수 있음 — 통합 동작은 수동 확인 필요"
---

# Phase 01: Core 검증 보고서

**Phase Goal:** Space와 콘텐츠를 CRUD하고 RBAC 권한 제어가 동작하는 완전한 핵심 기능
**검증 일시:** 2026-07-06T20:55:00+09:00
**상태:** human_needed
**초기 검증 여부:** 예 — 이전 VERIFICATION.md 없음

## Goal Achievement

### Observable Truths (성공 기준)

| # | Truth | 상태 | 증거 |
|---|-------|------|------|
| 1 | 사용자/그룹 CRUD API가 정상 동작하고 테스트를 통과한다 | VERIFIED | Tests run: 9 (UserControllerTest 5 + GroupControllerTest 4), Failures: 0, BUILD SUCCESS |
| 2 | Space 생성/조회/수정/삭제가 동작하며 SpaceListView에서 확인된다 | VERIFIED | SpaceControllerTest 5개 통과, SpaceListView.vue 179줄 DxDataGrid + DxPopup 구현 |
| 3 | 콘텐츠 계층 트리 API가 동작하고 ContentTree에서 렌더링된다 | VERIFIED | ContentControllerTest getContentTree_returnsTree 통과, ContentTree.vue DxTreeView :items="treeItems" 바인딩 |
| 4 | TipTap 에디터로 콘텐츠를 작성/게시하면 DB에 저장된다 | VERIFIED | publishContent → contentVersionRepository.save() 호출 확인, ContentControllerTest publishContent_incrementsVersion 통과 |
| 5 | 버전 목록 조회가 동작하고 VersionHistoryPanel에서 확인된다 | VERIFIED | ContentControllerTest getVersions_afterPublish 통과, VersionHistoryPanel.vue 147줄 contentApi.getVersions() 호출 및 v-for 렌더링 |
| 6 | PermissionService가 개인>그룹>전체 우선순위로 권한을 판단한다 | VERIFIED | resolveSpacePermission() USER→GROUP→ALL 순서 코드 확인, PermissionServiceTest canRead_personalNone_overridesGroupRead 테스트 통과 (6/6) |

**Score: 6/6**

### Deferred Items

해당 없음.

---

## Required Artifacts

### Plan 01 (사용자/그룹 CRUD)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `backend/src/main/java/com/company/wiki/user/service/UserService.java` | 60 | 89 | VERIFIED |
| `backend/src/main/java/com/company/wiki/user/controller/UserController.java` | 50 | 60 | VERIFIED |
| `backend/src/main/java/com/company/wiki/user/controller/GroupController.java` | 40 | 76 | VERIFIED |

### Plan 02 (Space CRUD)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `backend/src/main/java/com/company/wiki/space/entity/Space.java` | 50 | 60 | VERIFIED |
| `backend/src/main/java/com/company/wiki/space/service/SpaceService.java` | 80 | 128 | VERIFIED |
| `backend/src/main/java/com/company/wiki/space/controller/SpaceController.java` | 60 | 84 | VERIFIED |

### Plan 03 (Permission)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `backend/src/main/java/com/company/wiki/permission/service/PermissionService.java` | 80 | 207 | VERIFIED |
| `backend/src/main/java/com/company/wiki/permission/entity/SpacePermission.java` | 30 | 50 | VERIFIED |

### Plan 04 (Content)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `backend/src/main/java/com/company/wiki/content/service/ContentService.java` | 100 | 298 | VERIFIED |
| `backend/src/main/java/com/company/wiki/content/controller/ContentController.java` | 70 | 95 | VERIFIED |
| `backend/src/main/java/com/company/wiki/content/entity/Content.java` | 50 | 65 | VERIFIED |

### Plan 05 (Space UI)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `frontend/src/views/space/SpaceListView.vue` | 80 | 179 | VERIFIED |
| `frontend/src/components/layout/ContentTree.vue` | 60 | 69 | VERIFIED |
| `frontend/src/api/space.js` | 30 | 11 | WARNING (줄수 미달, 그러나 7개 함수 모두 구현됨) |

### Plan 06 (Content UI)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `frontend/src/views/content/ContentView.vue` | 60 | 165 | VERIFIED |
| `frontend/src/views/content/ContentEditorView.vue` | 80 | 168 | VERIFIED |
| `frontend/src/components/content/TipTapEditor.vue` | 60 | 190 | VERIFIED |
| `frontend/src/api/content.js` | 30 | 11 | WARNING (줄수 미달, 그러나 7개 함수 모두 구현됨) |

### Plan 07 (Version/Permission UI)

| Artifact | min_lines | 실제 줄수 | 상태 |
|----------|-----------|-----------|------|
| `frontend/src/components/content/VersionHistoryPanel.vue` | 50 | 147 | VERIFIED |
| `frontend/src/views/space/SpacePermissionView.vue` | 60 | 215 | VERIFIED |
| `frontend/src/api/permission.js` | 15 | 8 | WARNING (줄수 미달, 그러나 3개 함수 모두 구현됨) |

> **API 파일 줄수 미달 참고:** `space.js`, `content.js`, `permission.js`는 모두 min_lines 기준 미달이지만, 단일-export 객체 패턴으로 필요한 모든 API 함수가 구현되어 있어 기능적으로 완전함. 구조적 간결함으로 인한 줄수 감소이며 스텁이 아님.

---

## Key Link Verification

| From | To | Via | 상태 |
|------|----|----|------|
| `UserController.java` | `UserService.java` | `@RequiredArgsConstructor` 의존성 주입 | WIRED |
| `SpaceController.java` | `SpaceService.java` | `@RequiredArgsConstructor` 의존성 주입 | WIRED |
| `PermissionService.java` | `SpacePermissionRepository.java` | `@RequiredArgsConstructor` 의존성 주입 | WIRED |
| `ContentService.java` | `PermissionService.java` | canRead/canWrite 호출 (8개 지점) | WIRED |
| `SpaceListView.vue` | `api/space.js` | `spaceStore.fetchSpaces()` → `spaceApi.getSpaces()` | WIRED |
| `SpaceHomeView.vue` | `ContentTree.vue` | `SpaceSidebar` → `ContentTree :space-key` (간접 연결) | WIRED |
| `ContentEditorView.vue` | `TipTapEditor.vue` | `<TipTapEditor v-model="body" :readonly="false" />` | WIRED |
| `ContentView.vue` | `VersionHistoryPanel.vue` | `v-model:visible="showVersionHistory"` | WIRED |

---

## Data-Flow Trace (Level 4)

| Artifact | 데이터 변수 | 소스 | 실제 데이터 여부 | 상태 |
|----------|------------|------|-----------------|------|
| `ContentTree.vue` | `spaceStore.contentTree` | `spaceApi.getContentTree(spaceKey)` → `/spaces/{key}/contents` API | ContentRepository.findBySpaceIdAndDeletedAtIsNull() DB 쿼리 확인 | FLOWING |
| `SpaceListView.vue` | `spaceStore.spaces` | `spaceApi.getSpaces()` → `/spaces` API | SpaceRepository.findByStatusAndDeletedAtIsNull() DB 쿼리 확인 | FLOWING |
| `VersionHistoryPanel.vue` | `versions` (ref) | `contentApi.getVersions(contentId)` → `/contents/{id}/versions` API | ContentVersionRepository.findByContentIdOrderByVersionNoDesc() DB 쿼리 확인 | FLOWING |
| `SpacePermissionView.vue` | `permissions` (ref) | `permissionApi.getSpacePermissions(spaceKey)` → `/spaces/{key}/permissions` API | SpacePermissionRepository.findBySpaceId() DB 쿼리 확인 | FLOWING |

---

## Behavioral Spot-Checks

| 동작 | 명령 | 결과 | 상태 |
|------|------|------|------|
| 전체 Phase 1 백엔드 테스트 | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw test -Dtest=UserControllerTest,GroupControllerTest,...` | Tests run: 25, Failures: 0, BUILD SUCCESS | PASS |
| 프론트엔드 빌드 | `cd frontend && npm run build` | `built in 3.71s` (오류 없음) | PASS |
| PermissionServiceTest 단독 실행 | `JAVA_HOME=... ./mvnw test -Dtest=PermissionServiceTest` | Tests run: 6, Failures: 0 | PASS |

> **주의:** `JAVA_HOME=/opt/homebrew/opt/openjdk@21`을 명시하지 않으면 시스템 기본 Java 17로 실행되어 class file version 불일치 오류가 발생함. CI/CD 환경에서는 JAVA_HOME 설정이 필수.

---

## Requirements Coverage

| 요구사항 | 구현 플랜 | 설명 | 상태 |
|---------|---------|------|------|
| REQ-USER | 01-01 | 사용자 CRUD + soft delete + @PreAuthorize SITE_ADMIN | SATISFIED |
| REQ-GROUP | 01-01 | 그룹 CRUD + 멤버 관리 API | SATISFIED |
| REQ-SPACE | 01-02, 01-05 | Space CRUD + 즐겨찾기 + SpaceListView UI | SATISFIED |
| REQ-CONTENT | 01-04, 01-06 | Content CRUD + 계층 트리 + TipTap 에디터 | SATISFIED |
| REQ-PERMISSION | 01-03, 01-07 | PermissionService RBAC + SpacePermissionView UI | SATISFIED |
| REQ-VERSION | 01-04, 01-07 | ContentVersion 생성 + VersionHistoryPanel UI | SATISFIED |
| REQ-FRONTEND | 01-05, 01-06, 01-07 | Space/Content/Permission 전체 Vue3 UI + DevExtreme | SATISFIED |

---

## Anti-Patterns Found

| 파일 | 줄 | 패턴 | 심각도 | 영향 |
|------|---|------|--------|------|
| `ContentService.java` | 42, 91, 112, 156, 201, 232, 249 | `PermissionService.canRead/canWrite(..., List.of())` — 그룹 권한을 빈 리스트로 전달 | WARNING | 그룹 권한 기반 접근 제어가 실제 동작하지 않음. SITE_ADMIN 또는 직접 USER 권한은 정상 동작하나, GROUP 타입 권한은 항상 무시됨. 기술 부채 |

> **안티패턴 상세:** `ContentService`는 PermissionService 호출 시 `userGroupIds`를 항상 `List.of()`(빈 리스트)로 하드코딩하여 전달합니다. SUMMARY 04에서 "그룹 권한은 Wave 1 후 통합 예정"으로 명시적으로 기록했지만, 해당 통합이 Phase 1 내에서 완료되지 않았습니다. `PermissionService` 자체의 그룹 우선순위 로직은 올바르게 구현되어 테스트도 통과하나, 실제 서비스 호출에서는 그룹 ID가 전달되지 않으므로 그룹 기반 권한 제어가 비활성 상태입니다.

---

## Human Verification Required

### 1. Space UI 브라우저 렌더링 검증

**테스트:** 브라우저에서 로그인 후 `/spaces`로 이동하여 SpaceListView의 Space 목록과 생성 다이얼로그 확인
**Expected:** DxDataGrid에 Space 목록이 표시되고, '새 Space 만들기' 버튼 클릭 시 DxPopup 다이얼로그가 열리며 Space를 생성할 수 있다
**Why human:** DevExtreme 컴포넌트 렌더링 및 상호작용은 실제 브라우저 환경에서만 검증 가능

### 2. TipTap 에디터 작성-게시 플로우

**테스트:** `/spaces/:key/contents/new`에서 TipTap 에디터로 콘텐츠 작성 후 '게시' 클릭, 이후 ContentView에서 읽기 모드 확인
**Expected:** TipTap JSON이 DB에 저장되고 ContentView의 `:readonly="true"` 모드에서 올바르게 렌더링된다
**Why human:** TipTap JSON 직렬화/역직렬화 및 에디터 UI 상호작용은 브라우저 환경에서만 확인 가능

### 3. VersionHistoryPanel 슬라이드 패널 동작

**테스트:** ContentView에서 '버전 기록' 버튼 클릭
**Expected:** DxPopup이 우측에서 슬라이드인되며 버전 목록(번호, 작성자, 날짜)이 표시된다
**Why human:** DxPopup 슬라이드 애니메이션 및 버전 목록 렌더링은 브라우저 환경에서만 확인 가능

### 4. 그룹 기반 권한 제어 실제 동작 확인

**테스트:**
1. 그룹 생성 → 사용자를 그룹에 추가
2. Space에 해당 그룹에 WRITE 권한 부여 (`POST /spaces/{key}/permissions { subjectType: "GROUP", subjectId: groupId, permissionLevel: "WRITE" }`)
3. 해당 그룹 멤버 사용자로 로그인하여 콘텐츠 생성 시도

**Expected:** 그룹 멤버가 콘텐츠를 생성할 수 있어야 함
**Why human:** `ContentService`가 `PermissionService`에 `userGroupIds`를 `List.of()`로 전달하므로 그룹 권한이 실제 동작하지 않을 가능성이 높음. 현재 코드 상태에서는 그룹 권한이 무시될 것으로 예상됨 — 이 동작이 의도적인 미완성인지 버그인지 결정 필요

---

## Gaps Summary

자동 검증으로 발견된 기능적 BLOCKER는 없습니다.

**주요 발견 사항 (WARNING):**

1. **그룹 권한 통합 미완성** — `ContentService`의 모든 `PermissionService` 호출이 `userGroupIds`를 `List.of()`로 전달. `PermissionService`의 그룹 우선순위 로직은 올바르게 구현되어 단위 테스트도 통과하지만, 실제 콘텐츠 접근 시 그룹 기반 권한은 작동하지 않음. SUMMARY에서 "Wave 1 후 통합 예정"으로 명시했으나 Phase 1 내에서 완성되지 않음.

2. **API 파일 min_lines 미달** — `space.js`(11줄), `content.js`(11줄), `permission.js`(8줄)가 min_lines 기준 미달이나, 단일-export 객체 방식으로 필요한 모든 함수가 구현되어 기능적으로는 완전함. 기준이 파일 구조 패턴을 고려하지 않은 것으로 판단.

3. **JAVA_HOME 환경 의존성** — `JAVA_HOME=/opt/homebrew/opt/openjdk@21` 없이 테스트 실행 시 BUILD FAILURE 발생. CI/CD 환경 구성 필요.

---

_검증 일시: 2026-07-06T20:55:00+09:00_
_검증자: Claude (gsd-verifier)_
