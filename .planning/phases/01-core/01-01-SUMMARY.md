---
phase: 01-core
plan: "01"
subsystem: user-group-management
tags: [user, group, crud, security, jwt, spring-boot]
dependency_graph:
  requires: []
  provides: [user-crud-api, group-crud-api, group-member-management]
  affects: [space-api, permission-api, content-api]
tech_stack:
  added: []
  patterns: [Spring Data JPA, @EmbeddedId composite key, @PreAuthorize method security, soft delete]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/user/entity/Group.java
    - backend/src/main/java/com/company/wiki/user/entity/GroupMember.java
    - backend/src/main/java/com/company/wiki/user/repository/GroupRepository.java
    - backend/src/main/java/com/company/wiki/user/repository/GroupMemberRepository.java
    - backend/src/main/java/com/company/wiki/user/dto/UserDto.java
    - backend/src/main/java/com/company/wiki/user/dto/GroupDto.java
    - backend/src/main/java/com/company/wiki/user/service/UserService.java
    - backend/src/main/java/com/company/wiki/user/service/GroupService.java
    - backend/src/main/java/com/company/wiki/user/controller/GroupController.java
    - backend/src/test/java/com/company/wiki/user/controller/UserControllerTest.java
    - backend/src/test/java/com/company/wiki/user/controller/GroupControllerTest.java
  modified:
    - backend/src/main/java/com/company/wiki/user/controller/UserController.java
    - backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java
    - backend/src/main/java/com/company/wiki/common/config/SecurityConfig.java
decisions:
  - "@PreAuthorize에서 ROLE_ 접두사 명시적 사용 (hasAuthority('ROLE_SITE_ADMIN')) — UserDetailsServiceImpl이 ROLE_ 접두사를 부여하므로 hasRole()과 hasAuthority() 혼용 방지"
  - "GroupMemberId를 @Embeddable 정적 내부 클래스로 정의 — 복합키 Serializable 구현 및 @EmbeddedId 표준 패턴 준수"
  - "UserDto.CreateRequest record의 compact constructor로 role null 기본값 처리"
metrics:
  duration: "~10분"
  completed_date: "2026-07-02"
  tasks_completed: 5
  tests_passed: 9
  files_created: 11
  files_modified: 3
---

# Phase 01 Plan 01: 사용자/그룹 CRUD API 구현 Summary

## One-liner

JWT 인증 + @PreAuthorize 메서드 시큐리티로 SITE_ADMIN 권한 제어된 User/Group CRUD API 및 그룹 멤버 관리 엔드포인트 구현

## What Was Built

### Task 1: Group/GroupMember 엔티티 및 레포지토리
- `Group.java`: `@Entity @Table(name="groups")`, `@OneToMany` members 관계, `@PrePersist` createdAt 자동 설정
- `GroupMember.java`: `@EmbeddedId GroupMemberId` 복합키, `@MapsId` 관계 매핑, 정적 내부 `@Embeddable` 클래스
- `GroupRepository`: `findByName`, `existsByName` 쿼리 메서드
- `GroupMemberRepository`: `findByGroupId`, `findByUserId`, `deleteByIdGroupIdAndIdUserId`

### Task 2: DTO 및 ErrorCode 보완
- `UserDto`: `CreateRequest`(검증 어노테이션), `UpdateRequest`(@Nullable 필드), `Response`(from(User) 팩토리 메서드)
- `GroupDto`: `CreateRequest`, `Response`(from(Group) + memberCount)
- `ErrorCode`: `GROUP_NOT_FOUND`, `DUPLICATE_EMAIL` 추가

### Task 3: 서비스 레이어
- `UserService`: `findAll`, `findById`, `create`(중복 loginId/email 검증 + bcrypt 암호화), `update`, `deactivate`(soft delete status="INACTIVE"), `findMe`
- `GroupService`: `findAll`, `findById`, `create`, `update`, `delete`, `addMember`, `removeMember`, `getMembers`

### Task 4: 컨트롤러 및 SecurityConfig
- `UserController`: GET/POST/PUT/DELETE /users, GET /users/me 리팩토링 (UserService 위임)
- `GroupController`: /groups CRUD + /groups/{id}/members 멤버 관리 (POST/DELETE/GET)
- `SecurityConfig`: `@EnableMethodSecurity` 추가로 `@PreAuthorize` 활성화

### Task 5: 통합 테스트
- `UserControllerTest` 5개: 사용자 생성, 목록조회, 수정, 삭제, 미인증 401
- `GroupControllerTest` 4개: 그룹 생성, 멤버추가, 미인증 목록조회, 미인증 생성
- **Tests run: 9, Failures: 0, BUILD SUCCESS**

## Commits

| Hash | Type | Description |
|------|------|-------------|
| dbf4e6c | feat | Group/GroupMember 엔티티 및 레포지토리 추가 |
| e89348a | feat | UserDto/GroupDto DTO 및 ErrorCode 보완 |
| baba5da | feat | UserService/GroupService 서비스 레이어 구현 |
| 429e63b | feat | UserController/GroupController + SecurityConfig @EnableMethodSecurity |
| cba2266 | test | UserControllerTest/GroupControllerTest 통합 테스트 추가 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ROLE_ 접두사 authority 불일치 수정**
- **Found during:** Task 4
- **Issue:** `UserDetailsServiceImpl`이 `ROLE_SITE_ADMIN`으로 authority를 등록하는데, 컨트롤러에서 `hasAuthority('SITE_ADMIN')`을 사용하면 권한 체크가 항상 실패함
- **Fix:** `hasAuthority('ROLE_SITE_ADMIN')`으로 수정 (또는 `hasRole('SITE_ADMIN')`도 동일 효과이나 명시성을 위해 hasAuthority 유지)
- **Files modified:** `UserController.java`, `GroupController.java`
- **Commit:** 429e63b

**2. [Rule 1 - Bug] GET /users/{id} SpEL expression 수정**
- **Found during:** Task 4
- **Issue:** `#id == authentication.name.toLong()` SpEL에서 `.toLong()`은 Groovy 문법으로 Java SpEL에서 동작하지 않음
- **Fix:** `T(Long).parseLong(authentication.name)`으로 수정
- **Files modified:** `UserController.java`
- **Commit:** 429e63b

## Known Stubs

없음 — 모든 API가 실제 PostgreSQL DB와 연동되어 동작함

## Threat Flags

없음 — 모든 관리 API에 `@PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")` 적용됨

## Self-Check: PASSED

- Group.java 존재: FOUND
- GroupMember.java 존재: FOUND
- UserService.java 존재: FOUND
- GroupService.java 존재: FOUND
- UserControllerTest.java 존재: FOUND
- GroupControllerTest.java 존재: FOUND
- 커밋 dbf4e6c: FOUND
- 커밋 e89348a: FOUND
- 커밋 baba5da: FOUND
- 커밋 429e63b: FOUND
- 커밋 cba2266: FOUND
- Tests run: 9, Failures: 0: VERIFIED
