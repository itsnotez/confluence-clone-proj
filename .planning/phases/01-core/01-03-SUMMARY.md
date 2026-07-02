---
phase: 01-core
plan: 03
subsystem: permission
tags: [rbac, permission, space-permission, content-permission, jpa, unit-test]
dependency_graph:
  requires: []
  provides: [PermissionService, SpacePermissionAPI]
  affects: [space, content]
tech_stack:
  added: []
  patterns: [RBAC, Subject-Type-Priority, JPA-Entity, Mockito-Unit-Test]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/permission/entity/SpacePermission.java
    - backend/src/main/java/com/company/wiki/permission/entity/ContentPermission.java
    - backend/src/main/java/com/company/wiki/permission/repository/SpacePermissionRepository.java
    - backend/src/main/java/com/company/wiki/permission/repository/ContentPermissionRepository.java
    - backend/src/main/java/com/company/wiki/permission/service/PermissionService.java
    - backend/src/main/java/com/company/wiki/permission/dto/PermissionDto.java
    - backend/src/main/java/com/company/wiki/permission/controller/SpacePermissionController.java
    - backend/src/test/java/com/company/wiki/permission/service/PermissionServiceTest.java
  modified: []
decisions:
  - "SpacePermission 엔티티에 @ManyToOne 대신 Long spaceId 사용 — PermissionService가 SpaceRepository에 의존하지 않도록 순환의존 방지"
  - "SpacePermissionController만 SpaceRepository 주입 — spaceKey→spaceId 변환은 컨트롤러 레이어에서만 수행"
  - "권한 레벨 비교를 PERMISSION_ORDER List.indexOf()로 구현 — 단순하고 확장 가능"
  - "ALL 타입은 subject_id가 NULL — findBySpaceIdAndSubjectTypeAndSubjectIdIsNull() 별도 쿼리 메서드 사용"
metrics:
  duration: "3분 33초"
  completed_date: "2026-07-02"
  tasks_completed: 4
  files_created: 8
  files_modified: 0
---

# Phase 01 Plan 03: Permission RBAC 시스템 Summary

**One-liner:** SpacePermission/ContentPermission JPA 엔티티, 개인>그룹>전체 우선순위 PermissionService, SpacePermission CRUD API, Mockito 단위 테스트 6개 구현

## 완료된 Tasks

| Task | 이름 | 커밋 | 핵심 파일 |
|------|------|------|-----------|
| 1 | SpacePermission/ContentPermission 엔티티 및 레포지토리 | e1dc8e7 | SpacePermission.java, ContentPermission.java, SpacePermissionRepository.java, ContentPermissionRepository.java |
| 2 | PermissionService 구현 (개인>그룹>전체 우선순위) | 32e3edc | PermissionService.java |
| 3 | PermissionDto + SpacePermissionController | 80b1c10 | PermissionDto.java, SpacePermissionController.java |
| 4 | TDD — PermissionService 단위 테스트 | 214b004 | PermissionServiceTest.java |

## 구현 내용

### 권한 레벨 우선순위

```
높음: SPACE_ADMIN > WRITE > READ > NONE
판단: 개인(USER) > 그룹(GROUP) > 전체(ALL)
```

### PermissionService 핵심 메서드

| 메서드 | 설명 |
|--------|------|
| `resolveSpacePermission(spaceId, userId, userGroupIds)` | 우선순위 순으로 권한 레벨 문자열 반환 |
| `canRead(spaceId, userId, userRole, userGroupIds)` | READ 이상이면 true, SITE_ADMIN 항상 true |
| `canWrite(spaceId, userId, userRole, userGroupIds)` | WRITE 이상이면 true, SITE_ADMIN 항상 true |
| `isSpaceAdmin(spaceId, userId, userRole, userGroupIds)` | SPACE_ADMIN이면 true, SITE_ADMIN 항상 true |
| `resolveContentPermission(...)` | 콘텐츠 권한 없으면 Space 권한 상속 |
| `grantSpacePermission(...)` | 기존 권한 업데이트 or 신규 생성 |
| `revokeSpacePermission(...)` | 권한 삭제 |
| `findSpacePermissions(spaceId)` | Space 권한 목록 |

### API 엔드포인트

| Method | URL | 설명 | 권한 필요 |
|--------|-----|------|-----------|
| GET | `/spaces/{spaceKey}/permissions` | 권한 목록 조회 | SPACE_ADMIN 이상 |
| POST | `/spaces/{spaceKey}/permissions` | 권한 부여 | SPACE_ADMIN 이상 |
| DELETE | `/spaces/{spaceKey}/permissions?subjectType=USER&subjectId=1` | 권한 삭제 | SPACE_ADMIN 이상 |

### 단위 테스트 결과

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| 테스트 | 검증 내용 |
|--------|-----------|
| `canRead_siteAdmin_alwaysTrue` | SITE_ADMIN은 Repository 호출 없이 항상 true |
| `canRead_personalRead_true` | 개인 READ 권한 → canRead true |
| `canRead_groupWrite_true` | 그룹 WRITE 권한 → canRead true (WRITE >= READ) |
| `canRead_noPermission_false` | 권한 없으면 false |
| `canRead_personalNone_overridesGroupRead` | **핵심**: 개인 NONE이 그룹 READ를 오버라이드 |
| `canWrite_readOnly_false` | READ만 있으면 canWrite false |

## 설계 결정

### 순환 의존성 방지
- `PermissionService`는 `SpaceRepository`에 의존하지 않음 → `spaceId`를 파라미터로 직접 받음
- `SpacePermissionController`만 `SpaceRepository`를 주입받아 `spaceKey→spaceId` 변환 수행
- Wave 1 병렬 실행 환경에서도 SpaceRepository 유무와 무관하게 PermissionService 독립 동작

### ALL 타입 NULL 처리
- `subject_type = 'ALL'`인 경우 `subject_id = NULL` (DB UNIQUE 제약 고려)
- JPA 쿼리 메서드를 `findBySpaceIdAndSubjectTypeAndSubjectIdIsNull()`로 별도 처리

### 권한 레벨 비교
- `PERMISSION_ORDER = ["NONE", "READ", "WRITE", "SPACE_ADMIN"]` 리스트로 rank 비교
- `permissionRank(level) >= permissionRank("READ")` 방식으로 canRead/canWrite 판단

## Deviations from Plan

### 계획된 변경 사항 적용

**1. ALL 타입 쿼리 메서드 추가**
- 계획: `findBySpaceIdAndSubjectType(spaceId, "ALL")` 으로 정의
- 변경: `findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(spaceId, "ALL")`으로 변경
- 이유: `subject_id = NULL`인 ALL 레코드를 올바르게 조회하기 위해 IS NULL 조건 필요
- Rule 1 (버그 방지) — JPA 메서드 이름이 NULL 조건을 자동으로 처리하지 않음

**이 외 편차 없음 — 계획 그대로 실행**

## 검증 결과

- [x] PermissionServiceTest Tests run: 6, Failures: 0, BUILD SUCCESS
- [x] canRead(개인 NONE + 그룹 READ) = false 확인 (우선순위 핵심 검증)
- [x] SITE_ADMIN은 항상 true 확인
- [x] SpacePermissionController GET/POST/DELETE 컴파일 성공
- [x] 전체 mvnw compile 성공

## Self-Check: PASSED

- SpacePermission.java: FOUND
- ContentPermission.java: FOUND
- SpacePermissionRepository.java: FOUND
- ContentPermissionRepository.java: FOUND
- PermissionService.java: FOUND (207 lines > min 80)
- PermissionDto.java: FOUND
- SpacePermissionController.java: FOUND
- PermissionServiceTest.java: FOUND
- Commits e1dc8e7, 32e3edc, 80b1c10, 214b004: FOUND
