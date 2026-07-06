---
phase: 03-search
plan: 03
subsystem: comment
tags: [comment, soft-delete, tree, rbac, tdd]
dependency_graph:
  requires: [03-01]
  provides: [comment-api]
  affects: [content-api]
tech_stack:
  added: []
  patterns: [soft-delete, tree-assembly, author-permission-check, tdd-red-green]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/comment/entity/Comment.java
    - backend/src/main/java/com/company/wiki/comment/repository/CommentRepository.java
    - backend/src/main/java/com/company/wiki/comment/dto/CommentDto.java
    - backend/src/main/java/com/company/wiki/comment/service/CommentService.java
    - backend/src/main/java/com/company/wiki/comment/controller/CommentController.java
    - backend/src/test/java/com/company/wiki/comment/controller/CommentControllerTest.java
  modified: []
decisions:
  - "Comment soft delete: deletedAt 필드 설정 (DB row 보존), findByContentIdAndDeletedAtIsNull 필터"
  - "트리 조립: Map<Long,List<Comment>> byParent + null 키=루트 패턴 (ContentService와 동일)"
  - "작성자 권한: authorId.equals(userId) || SITE_ADMIN — checkAuthorOrAdmin 헬퍼"
metrics:
  duration: 7분
  completed: 2026-07-07
  tasks: 3
  files: 6
---

# Phase 3 Plan 3: Comment 백엔드 Summary

**One-liner:** 계층형 댓글 2레벨 트리 + soft delete + 작성자/SITE_ADMIN 권한 체크 (COMMENT-01, COMMENT-02 완료)

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Comment 엔티티 + Repository + DTO | 3cb607f | Comment.java, CommentRepository.java, CommentDto.java |
| 2 | CommentService + CommentController | b590ff0 | CommentService.java, CommentController.java |
| 3 (TDD RED) | CommentControllerTest 작성 | 7deca6b | CommentControllerTest.java |
| 3 (TDD GREEN) | 테스트 통과 확인 | (포함) | 3/3 통과 |

## Endpoints Implemented

| Method | Path | 설명 | 요구사항 |
|--------|------|------|----------|
| GET | /contents/{contentId}/comments | 댓글 트리 조회 (2레벨) | — |
| POST | /contents/{contentId}/comments | 댓글 작성 (WRITE 권한) | COMMENT-01 |
| PUT | /comments/{commentId} | 댓글 수정 (본인/SITE_ADMIN) | — |
| DELETE | /comments/{commentId} | 댓글 soft delete (본인/SITE_ADMIN) | COMMENT-02 |

## Test Results

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
- createComment_success: POST 200 + $.data.id 존재
- deleteOwnComment_success: 204 후 GET 목록 미노출 확인
- getComments_returnsTree: $.data[0].children[0].body = "대댓글"
```

## Deviations from Plan

None — plan executed exactly as written.

## TDD Gate Compliance

- RED commit: 7deca6b (test(03-03): add failing CommentControllerTest)
- GREEN: 3/3 테스트 통과 (BUILD SUCCESS)

## Threat Model Coverage

| Threat | Mitigation | Status |
|--------|-----------|--------|
| T-03-05: Tampering (update/delete) | checkAuthorOrAdmin — 본인 또는 SITE_ADMIN | MITIGATED |
| T-03-06: EoP (createComment) | canWrite 체크 — WRITE 권한 없으면 403 | MITIGATED |

## Self-Check: PASSED

- Comment.java: FOUND
- CommentRepository.java: FOUND
- CommentDto.java: FOUND
- CommentService.java: FOUND
- CommentController.java: FOUND
- CommentControllerTest.java: FOUND
- Commits 3cb607f, b590ff0, 7deca6b: FOUND
