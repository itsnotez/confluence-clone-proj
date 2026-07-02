---
phase: 01-core
plan: 02
subsystem: space
tags: [space, crud, favorites, jpa, spring-boot, integration-test]
dependency_graph:
  requires: [01-01]
  provides: [space-crud-api, space-favorites-api]
  affects: [01-03, 01-04]
tech_stack:
  added: []
  patterns: [JPA @EmbeddedId, @ManyToOne LAZY, soft-delete, toggle-pattern]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/space/entity/Space.java
    - backend/src/main/java/com/company/wiki/space/entity/SpaceFavoriteId.java
    - backend/src/main/java/com/company/wiki/space/entity/SpaceFavorite.java
    - backend/src/main/java/com/company/wiki/space/repository/SpaceRepository.java
    - backend/src/main/java/com/company/wiki/space/repository/SpaceFavoriteRepository.java
    - backend/src/main/java/com/company/wiki/space/dto/SpaceDto.java
    - backend/src/main/java/com/company/wiki/space/service/SpaceService.java
    - backend/src/main/java/com/company/wiki/space/controller/SpaceController.java
    - backend/src/test/java/com/company/wiki/space/controller/SpaceControllerTest.java
  modified: []
decisions:
  - SpaceFavoriteId를 @Embeddable record로 구현 (Java record + JPA 복합키)
  - SpaceFavoriteRepository에서 existsByIdSpaceIdAndIdUserId 네이밍 사용 (EmbeddedId 탐색)
  - toggleFavorite은 POST/DELETE 양쪽 모두 동일 서비스 메서드 호출 (REST 의미론적 구분)
  - soft delete: deletedAt 필드 + status="DELETED" 동시 설정
metrics:
  duration: "약 20분"
  completed_date: "2026-07-02"
  tasks_completed: 5
  files_created: 9
  files_modified: 0
---

# Phase 01 Plan 02: Space CRUD API 및 즐겨찾기 구현 Summary

Space 도메인의 CRUD API와 즐겨찾기 토글 기능을 @EmbeddedId record 복합키와 soft-delete 패턴으로 구현하고 통합 테스트 5개 통과.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Space/SpaceFavorite 엔티티 및 레포지토리 | 2877002 | Space.java, SpaceFavoriteId.java, SpaceFavorite.java, SpaceRepository.java, SpaceFavoriteRepository.java |
| 2 | SpaceDto + ErrorCode 보완 | a1db616 | SpaceDto.java |
| 3 | SpaceService 구현 | ff11092 | SpaceService.java |
| 4 | SpaceController REST 엔드포인트 | e4f762a | SpaceController.java |
| 5 | TDD — Space API 통합 테스트 | d65314c | SpaceControllerTest.java |

## Implementation Details

### 엔티티 설계

**Space.java**
- `@Entity @Table(name="spaces")`
- spaceKey(VARCHAR 50, UNIQUE), name(VARCHAR 300), description(TEXT)
- type/status: String (기본값 "PRIVATE"/"ACTIVE" — @PrePersist)
- `@ManyToOne(fetch=LAZY) User createdBy`
- deletedAt(nullable) — soft delete 지원

**SpaceFavoriteId.java**
- `@Embeddable record` — Java record + JPA @Embeddable 결합
- (spaceId Long, userId Long) 복합키

**SpaceFavorite.java**
- `@EmbeddedId SpaceFavoriteId id`
- `@MapsId("spaceId") Space space`, `@MapsId("userId") User user`

### 서비스 로직

- **findAll**: ACTIVE + deletedAt IS NULL 목록, 즐겨찾기 여부 포함
- **findByKey**: SPACE_NOT_FOUND 예외 처리
- **create**: existsBySpaceKey 체크 → DUPLICATE_SPACE_KEY(409)
- **update**: ARCHIVED 상태 체크 → ARCHIVED_SPACE(400), null 필드 skip
- **delete**: deletedAt=now(), status="DELETED" (soft delete)
- **toggleFavorite**: existsById → delete, else → save

### REST API

| Method | Path | Response |
|--------|------|----------|
| GET | /spaces | 200 ApiResponse<List<Response>> |
| GET | /spaces/{spaceKey} | 200 ApiResponse<Response> |
| POST | /spaces | 200 ApiResponse<Response> |
| PUT | /spaces/{spaceKey} | 200 ApiResponse<Response> |
| DELETE | /spaces/{spaceKey} | 204 No Content |
| POST | /spaces/{spaceKey}/favorite | 200 ApiResponse<Void> |
| DELETE | /spaces/{spaceKey}/favorite | 200 ApiResponse<Void> |

## Test Results

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- createSpace_success: POST /spaces 성공
- createSpace_duplicateKey_returns409: 중복 키 409 반환
- getSpaces_returnsActivelist: 목록 조회
- deleteSpace_softDelete: DELETE 후 GET 404 확인
- toggleFavorite_addsAndRemoves: 즐겨찾기 추가/제거 토글

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Flyway 마이그레이션 체크섬 불일치**
- **Found during:** Task 5 (테스트 실행 시 ApplicationContext 로딩 실패)
- **Issue:** V1__init_users_groups.sql 파일이 변경되어 DB에 저장된 체크섬(-588307163)과 로컬 체크섬(-315227983) 불일치
- **Fix:** `./mvnw flyway:repair` 실행으로 flyway_schema_history 테이블의 체크섬 갱신
- **Files modified:** 없음 (DB 스키마 히스토리 테이블만 수정)
- **Commit:** 없음 (인프라 수정)

**2. SpaceFavoriteId를 별도 파일로 분리**
- **Found during:** Task 1 (설계 검토)
- **이유:** JPA @Embeddable record는 별도 클래스 파일로 분리해야 Spring이 올바르게 인식
- **Files:** SpaceFavoriteId.java (신규 생성)

## Known Stubs

없음 — 모든 API가 실제 데이터와 연결됨.

## Threat Flags

없음 — 모든 /spaces 엔드포인트는 .anyRequest().authenticated() 정책으로 JWT 인증 필수.

## Self-Check: PASSED

- Space.java 존재: FOUND
- SpaceFavorite.java 존재: FOUND
- SpaceService.java 존재: FOUND
- SpaceController.java 존재: FOUND
- SpaceControllerTest.java 존재: FOUND
- 커밋 2877002: FOUND
- 커밋 a1db616: FOUND
- 커밋 ff11092: FOUND
- 커밋 e4f762a: FOUND
- 커밋 d65314c: FOUND
- Tests run: 5, BUILD SUCCESS: CONFIRMED
