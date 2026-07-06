---
phase: 03-search
plan: "06"
subsystem: frontend
tags: [comment, attachment, vue, api]
dependency_graph:
  requires: ["03-03", "03-04", "03-05"]
  provides: ["COMMENT-01", "COMMENT-02", "ATTACH-01", "ATTACH-02"]
  affects: ["frontend/src/views/content/ContentView.vue"]
tech_stack:
  added: []
  patterns:
    - "named export API module (mail.js 패턴)"
    - "inline panel component with watch(contentId) + onMounted"
    - "native file input ref + FormData upload"
    - "window.open for presigned URL download"
key_files:
  created:
    - frontend/src/api/comment.js
    - frontend/src/api/attachment.js
    - frontend/src/components/content/CommentPanel.vue
    - frontend/src/components/content/AttachmentPanel.vue
  modified:
    - frontend/src/views/content/ContentView.vue
decisions:
  - "download()는 axios 호출 없이 URL 문자열만 반환 — window.open으로 새 탭 오픈"
  - "CommentPanel isOwn(): authStore.user?.id === comment.createdBy?.id (UI 게이팅만, 서버가 최종 판단)"
  - "AttachmentPanel upload: 컴포넌트에서 FormData 생성, API 모듈에서 multipart/form-data 헤더 설정"
metrics:
  duration: "~12분"
  completed: "2026-07-07"
  tasks_completed: 3
  files_changed: 5
---

# Phase 3 Plan 06: 댓글·첨부파일 프론트엔드 Summary

**One-liner:** commentApi/attachmentApi named export 모듈 + CommentPanel(트리형 댓글)/AttachmentPanel(파일 업로드·window.open 다운로드) Vue 컴포넌트를 구현하고 ContentView에 연결

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | comment.js + attachment.js API 모듈 생성 | da16b78 | frontend/src/api/comment.js, attachment.js |
| 2 | CommentPanel.vue + AttachmentPanel.vue 생성 | 9715640 | frontend/src/components/content/CommentPanel.vue, AttachmentPanel.vue |
| 3 | ContentView.vue에 두 패널 연결 | 084b9cd | frontend/src/views/content/ContentView.vue |

## Verification

- comment.js: `export const commentApi` + list/create/remove — node 검증 통과
- attachment.js: `export const attachmentApi` + list/upload(FormData)/download(URL) — node 검증 통과
- CommentPanel.vue: commentApi, submitComment, removeComment, replyTargetId — node 검증 통과
- AttachmentPanel.vue: attachmentApi, onFileChange, openDownload, window.open — node 검증 통과
- ContentView.vue: CommentPanel/AttachmentPanel import + content-id 바인딩 — 검증 통과
- `npx vite build` — exit code 0, built in 3.73s

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None — CommentPanel과 AttachmentPanel 모두 실제 API 엔드포인트와 연결됨.

## Threat Flags

없음 — 새로운 네트워크 엔드포인트 없음. 기존 백엔드 API(03-03, 03-04)를 호출만 함.

## Self-Check: PASSED

- frontend/src/api/comment.js — FOUND
- frontend/src/api/attachment.js — FOUND
- frontend/src/components/content/CommentPanel.vue — FOUND
- frontend/src/components/content/AttachmentPanel.vue — FOUND
- frontend/src/views/content/ContentView.vue — modified, FOUND
- Commits da16b78, 9715640, 084b9cd — all present in git log
