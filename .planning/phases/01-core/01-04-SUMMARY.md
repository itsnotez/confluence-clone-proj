---
phase: 01-core
plan: "04"
subsystem: content
tags: [content, versioning, tree, crud, permission]
dependency_graph:
  requires: ["01-02", "01-03"]
  provides: ["content-crud", "content-versioning", "content-tree"]
  affects: ["01-05", "01-06"]
tech_stack:
  added: []
  patterns:
    - "JPA @ManyToOne with Long FK (순환 참조 방지)"
    - "메모리 Map<parentId, List<Content>> 트리 구성"
    - "soft delete (deletedAt + status=ARCHIVED)"
    - "Deferred FK constraint (current_version_id)"
key_files:
  created:
    - backend/src/main/java/com/company/wiki/content/entity/Content.java
    - backend/src/main/java/com/company/wiki/content/entity/ContentVersion.java
    - backend/src/main/java/com/company/wiki/content/repository/ContentRepository.java
    - backend/src/main/java/com/company/wiki/content/repository/ContentVersionRepository.java
    - backend/src/main/java/com/company/wiki/content/dto/ContentDto.java
    - backend/src/main/java/com/company/wiki/content/service/ContentService.java
    - backend/src/main/java/com/company/wiki/content/controller/ContentController.java
    - backend/src/test/java/com/company/wiki/content/controller/ContentControllerTest.java
  modified:
    - backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java
decisions:
  - "Content.currentVersionId를 Long으로 선언하여 순환 FK 방지 (JPA @ManyToOne 미사용)"
  - "트리 조회는 DB 재귀 쿼리 대신 메모리 Map으로 구성 (단순성 우선)"
  - "PermissionService.canRead/canWrite에 List.of() 빈 그룹 ID 전달 (그룹 권한은 Wave 1 후 통합)"
metrics:
  duration: "약 20분"
  completed: "2026-07-02"
  tasks_completed: 5
  files_created: 8
  files_modified: 1
---

# Phase 01 Plan 04: 콘텐츠 CRUD + 계층 트리 + 버전 관리 Summary

**One-liner:** Content/ContentVersion JPA 엔티티, 메모리 트리 조회, 버전 이력 관리 REST API (7 엔드포인트, 통합 테스트 5개 통과)

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Content/ContentVersion 엔티티 및 레포지토리 | 3622f54 | Content.java, ContentVersion.java, ContentRepository.java, ContentVersionRepository.java |
| 2 | ContentDto + ErrorCode 보완 | d04a72e | ContentDto.java, ErrorCode.java |
| 3 | ContentService 구현 | 85d8b87 | ContentService.java |
| 4 | ContentController REST 엔드포인트 | 2effd28 | ContentController.java |
| 5 | 통합 테스트 작성 및 통과 | 5c04bbf | ContentControllerTest.java |

## API Endpoints

| Method | Path | 설명 |
|--------|------|------|
| GET | /spaces/{spaceKey}/contents | Space 콘텐츠 트리 조회 |
| POST | /spaces/{spaceKey}/contents | 콘텐츠 생성 (DRAFT) |
| GET | /contents/{id} | 단건 조회 |
| PUT | /contents/{id} | 수정 (Draft 덮어쓰기) |
| POST | /contents/{id}/publish | 게시 (새 버전 생성) |
| DELETE | /contents/{id} | soft delete (204) |
| GET | /contents/{id}/versions | 버전 목록 |

## Test Results

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- createContent_success: DRAFT 상태로 생성 확인
- getContentTree_returnsTree: 트리 배열 반환 확인
- publishContent_incrementsVersion: PUBLISHED 상태 전환 확인
- getVersions_afterPublish: 버전 목록 및 versionNo 확인
- deleteContent_softDelete: soft delete 후 404 확인

## Deviations from Plan

None - 플랜대로 정확히 실행됨.

## Known Stubs

없음.

## Self-Check: PASSED

- Content.java: 존재
- ContentVersion.java: 존재
- ContentService.java: 존재 (298줄)
- ContentController.java: 존재 (95줄)
- ContentControllerTest.java: 존재, 5개 테스트 통과
- ErrorCode.PERMISSION_DENIED: 추가됨
- 커밋 해시: 3622f54, d04a72e, 85d8b87, 2effd28, 5c04bbf
