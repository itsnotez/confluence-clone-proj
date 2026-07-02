---
phase: 01-core
plan: "06"
subsystem: frontend-content
tags: [vue3, tiptap, pinia, devextreme, content-editor]
dependency_graph:
  requires: ["01-04", "01-05"]
  provides: ["content-read-view", "content-editor-view", "tiptap-editor"]
  affects: ["01-07"]
tech_stack:
  added:
    - "@tiptap/vue-3 useEditor + EditorContent"
    - "@tiptap/extension-table (Table, TableRow, TableCell, TableHeader)"
    - "DxToast for save/publish notifications"
  patterns:
    - "Pinia Composition API store for content state"
    - "TipTap JSON string as modelValue (JSON.parse/stringify)"
    - "readonly prop으로 읽기/쓰기 모드 분리"
key_files:
  created:
    - frontend/src/api/content.js
    - frontend/src/stores/content.js
    - frontend/src/components/content/TipTapEditor.vue
  modified:
    - frontend/src/views/content/ContentView.vue
    - frontend/src/views/content/ContentEditorView.vue
    - frontend/src/router/index.js
decisions:
  - "TableRow/TableCell/TableHeader는 @tiptap/extension-table 단일 패키지에서 named export됨 (별도 패키지 불필요)"
  - "ContentView에서 버전 기록 버튼은 stub으로 두고 Plan 07에서 구현"
metrics:
  duration: "15분"
  completed: "2026-07-02"
  tasks_completed: 4
  files_changed: 6
---

# Phase 01 Plan 06: 콘텐츠 뷰 & TipTap 에디터 Summary

콘텐츠 읽기(ContentView)와 TipTap JSON 기반 리치텍스트 편집기(ContentEditorView + TipTapEditor)를 구현하고 라우터에 `/contents/new` 라우트를 추가함.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Content API + Pinia Store | 11e4b43 | api/content.js, stores/content.js |
| 2 | TipTapEditor 컴포넌트 | 82e42f6 | components/content/TipTapEditor.vue |
| 3 | ContentView 읽기 모드 | c82a995 | views/content/ContentView.vue |
| 4 | ContentEditorView + 라우터 | c5091c5 | views/content/ContentEditorView.vue, router/index.js |

## What Was Built

### frontend/src/api/content.js
- `getContentTree`, `createContent`, `getContent`, `updateContent`, `publishContent`, `deleteContent`, `getVersions` 7개 API 함수

### frontend/src/stores/content.js
- Pinia Composition API 스토어
- `currentContent`, `versions`, `isEditing` 상태
- `fetchContent`, `saveContent`, `publishContent`, `createContent`, `fetchVersions` 액션

### frontend/src/components/content/TipTapEditor.vue
- `useEditor`로 StarterKit + Image + Link + Table(Row/Cell/Header) 확장 구성
- `readonly` prop: `true` → editable:false + 툴바 숨김, `false` → 풀 편집 모드
- modelValue: TipTap JSON string (JSON.parse → setContent, getJSON → JSON.stringify → emit)
- 툴바: Bold, Italic, Underline, H1/H2, BulletList, OrderedList, Code, 코드블록, 링크, 이미지
- `onBeforeUnmount`: `editor.destroy()` 호출

### frontend/src/views/content/ContentView.vue
- AppHeader + SpaceSidebar + 메인 콘텐츠 Flexbox 레이아웃
- `TipTapEditor :readonly="true"` 읽기 모드 렌더링
- 편집 버튼 → `/spaces/:key/contents/:id/edit` 라우팅
- 버전 기록 버튼 (stub — Plan 07에서 슬라이드 패널 구현 예정)
- DxLoadIndicator 로딩 상태

### frontend/src/views/content/ContentEditorView.vue
- 신규(`/contents/new`) vs 편집(`/:contentId/edit`) 모드 자동 감지
- DxTextBox 제목 입력, TipTapEditor `v-model="body"` 편집
- "임시저장" 버튼: 신규 시 createContent → URL 변경, 수정 시 saveContent
- "게시" 버튼: createContent(신규) or publishContent → 완료 후 ContentView 이동
- DxToast 성공/오류 알림

### frontend/src/router/index.js
- `/spaces/:spaceKey/contents/new` 라우트 추가 (ContentEditorView 사용)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] TableRow/TableCell/TableHeader import 경로 수정**
- **Found during:** Task 2
- **Issue:** Plan에서 `@tiptap/extension-table-row`, `@tiptap/extension-table-cell`, `@tiptap/extension-table-header` 별도 패키지로 import 지시했으나, 실제 설치된 패키지는 `@tiptap/extension-table` 하나에 모두 포함됨 (node_modules 확인)
- **Fix:** `import { Table, TableRow, TableCell, TableHeader } from '@tiptap/extension-table'` 단일 import로 변경
- **Files modified:** TipTapEditor.vue
- **Commit:** 82e42f6

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| 버전 기록 버튼 (클릭 시 showVersionHistory=true만 설정, 패널 없음) | ContentView.vue:L37 | Plan 07에서 VersionHistoryPanel 슬라이드 패널 구현 예정 |

## Verification Results

- [x] `npm run build` 성공 (에러 없음, 경고: chunk size — 기존과 동일)
- [x] TipTapEditor.vue에 `readonly` prop 처리 로직 존재 (grep 결과: 4건)
- [x] ContentEditorView에 임시저장/게시 버튼 존재 (grep 결과: 4건)
- [x] router에 `/contents/new` 라우트 존재 확인

## Self-Check: PASSED

- frontend/src/api/content.js: FOUND
- frontend/src/stores/content.js: FOUND
- frontend/src/components/content/TipTapEditor.vue: FOUND
- frontend/src/views/content/ContentView.vue: FOUND (min_lines 60 이상 — 168줄)
- frontend/src/views/content/ContentEditorView.vue: FOUND (min_lines 80 이상 — 172줄)
- Commits 11e4b43, 82e42f6, c82a995, c5091c5: FOUND
