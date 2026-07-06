---
phase: 03-search
plan: "01"
subsystem: search-backend
tags: [search, tsvector, postgresql, spring-boot, tdd]
dependency_graph:
  requires: []
  provides: [search-api, content-search-body-sync]
  affects: [ContentService, ErrorCode]
tech_stack:
  added: []
  patterns: [native-query, tsvector-gin, permission-filter, tdd]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/search/dto/SearchDto.java
    - backend/src/main/java/com/company/wiki/search/repository/SearchRepository.java
    - backend/src/main/java/com/company/wiki/search/service/SearchService.java
    - backend/src/main/java/com/company/wiki/search/controller/SearchController.java
    - backend/src/main/java/com/company/wiki/content/repository/ContentSearchBodyRepository.java
    - backend/src/test/java/com/company/wiki/search/controller/SearchControllerTest.java
  modified:
    - backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java
    - backend/src/main/java/com/company/wiki/content/service/ContentService.java
decisions:
  - "SearchRepository는 UNION ALL 네이티브 쿼리로 제목(search_vector STORED) + 본문(content_search_bodies) 동시 검색"
  - "SearchService에서 UNION ALL 중복 id를 LinkedHashMap으로 제거 (rank 높은 순 첫 항목 유지)"
  - "content_search_bodies upsert는 ContentService.create/update/publish 세 곳에 직접 호출 (EventListener 대신)"
metrics:
  duration: "~15분"
  completed: "2026-07-07"
  tasks_completed: 3
  files_changed: 8
---

# Phase 3 Plan 1: Search Backend Summary

**One-liner:** PostgreSQL tsvector('simple') + GIN 인덱스 기반 콘텐츠 전문검색 API — 제목/본문 UNION ALL 쿼리 + PermissionService RBAC 필터링

## What Was Built

- `GET /api/search?q=키워드` 엔드포인트 — 인증 필요, 200 + JSON 배열 반환
- `SearchRepository`: `websearch_to_tsquery('simple', :q)`로 `contents.search_vector`(제목 STORED) + `content_search_bodies.search_vector`(본문) UNION ALL
- `SearchService`: `PermissionService.canRead` 필터로 READ 권한 없는 Space 결과 제외, LinkedHashMap으로 중복 id 제거
- `ContentSearchBodyRepository`: `to_tsvector('simple')` upsert 네이티브 쿼리
- `ContentService` 수정: createContent/updateContent/publishContent 3곳에 `upsertSearchBody` 호출 추가
- `ErrorCode` 추가: `LABEL_NOT_FOUND`, `COMMENT_NOT_FOUND`, `ATTACHMENT_NOT_FOUND`, `FILE_UPLOAD_FAILED` (03-02~04 플랜용)

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | ErrorCode + ContentSearchBodyRepository + ContentService | c1905cd | ErrorCode.java, ContentSearchBodyRepository.java, ContentService.java |
| 2 | SearchDto + SearchRepository + SearchService + SearchController | fc9020c | 4개 신규 파일 |
| 3 (RED) | SearchControllerTest 작성 | 8a43ec4 | SearchControllerTest.java |

## Test Results

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
- search_authenticated_returns200  PASS
- search_findsCreatedContent       PASS
- search_noAuth_returns401         PASS
```

## Deviations from Plan

None - 플랜 그대로 실행됨.

## TDD Gate Compliance

- RED 커밋: `8a43ec4` (test(03-01): add failing tests for SearchController)
- GREEN: 테스트 3개 통과 (별도 커밋 불필요 — 구현이 Task 2에서 이미 완료됨)

## Threat Mitigations Applied

| Threat | Mitigation |
|--------|-----------|
| T-03-01 Info Disclosure | SearchService에서 PermissionService.canRead 필터 적용 |
| T-03-02 SQL Injection | @Param 바인딩 사용, 문자열 concat 없음 |

## Self-Check: PASSED

- SearchController.java: FOUND
- SearchRepository.java: FOUND (websearch_to_tsquery('simple') 포함)
- SearchService.java: FOUND (permissionService.canRead 포함)
- ContentSearchBodyRepository.java: FOUND (to_tsvector('simple') 포함)
- Commits c1905cd, fc9020c, 8a43ec4: FOUND
