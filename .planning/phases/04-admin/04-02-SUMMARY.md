---
phase: 04-admin
plan: "02"
subsystem: notification-backend
tags: [notification, comment, service, jpa, tdd, idor-prevention]
dependency_graph:
  requires: []
  provides: [NotificationService, NotificationRepository, NotificationEntity, NotificationDto]
  affects: [CommentService]
tech_stack:
  added: []
  patterns: [SpringBoot-Service, JPA-Repository, @Modifying-Query, TDD-SpringBootTest]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/notification/entity/Notification.java
    - backend/src/main/java/com/company/wiki/notification/repository/NotificationRepository.java
    - backend/src/main/java/com/company/wiki/notification/dto/NotificationDto.java
    - backend/src/main/java/com/company/wiki/notification/service/NotificationService.java
    - backend/src/test/java/com/company/wiki/notification/service/NotificationServiceTest.java
  modified:
    - backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java
    - backend/src/main/java/com/company/wiki/comment/service/CommentService.java
decisions:
  - "Notification.isRead는 @Builder.Default false로 설정 — Lombok @Builder와 boolean 기본값 충돌 방지"
  - "NotificationRepository에 findByUserId(Long userId) List 오버로드 추가 — 테스트에서 단건 조회에 필요"
  - "content.getCreatedBy().getId() 사용 — Content.createdBy가 User 엔티티 ManyToOne 참조임을 Content.java에서 확인"
  - "NOTIFICATION_NOT_FOUND는 이 플랜에서 ErrorCode에 추가 (04-01 의존 없이 방어적으로 포함)"
metrics:
  duration: "3분 20초"
  completed_date: "2026-07-07"
  tasks: 2
  files: 7
---

# Phase 4 Plan 02: Notification Backend Summary

**One-liner:** 알림 도메인 전체(Notification 엔티티/리포지토리/DTO/서비스) 신규 구현 및 CommentService에 알림 생성 통합 (IDOR 방지 포함)

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Notification 엔티티·리포지토리·DTO·서비스 생성 | 1020a51 (RED), 96485b6 (GREEN) | entity, repository, dto, service, test, ErrorCode |
| 2 | CommentService에 NotificationService 통합 | 3075cd4 | CommentService.java |

## Implementation Details

### Task 1: Notification 도메인 (TDD)

**RED commit:** `1020a51` — NotificationServiceTest 4개 테스트 작성 (컴파일 실패 확인)

**GREEN commit:** `96485b6` — 다음 파일 생성:

- `Notification.java` — `@Entity @Table(name="notifications")`, `@Builder.Default isRead=false`, `@PrePersist createdAt`
- `NotificationRepository.java` — `countByUserIdAndIsReadFalse`, `findByIdAndUserId` (IDOR 방지), `markAllAsReadByUserId` (@Modifying @Query)
- `NotificationDto.java` — `Response record` + `PagedResponse<T> record` (from(Notification n) 팩터리 포함)
- `NotificationService.java` — `create/getNotifications/getUnreadCount/markAsRead/markAllAsRead`
- `ErrorCode.java` — `NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.")` 추가

테스트 결과: **4/4 PASS** (BUILD SUCCESS)

### Task 2: CommentService 통합

`createComment()` 내 댓글 저장 직후 알림 생성 블록 삽입:
- `content.getCreatedBy().getId()` — Content.createdBy가 `@ManyToOne User` 참조임을 확인 후 사용
- `!contentAuthorId.equals(userId)` 조건 — 자기 글 자기 댓글 알림 제외
- try-catch 래핑 — 알림 생성 실패가 댓글 트랜잭션 롤백 방지 (T-04-02-03 mitigate)
- `@Slf4j` + `log.warn` 추가

## Deviations from Plan

**1. [Rule 2 - Missing Functionality] NotificationRepository에 findByUserId(List) 오버로드 추가**
- Found during: Task 1 (테스트 작성 시)
- Issue: NotificationServiceTest.markAsRead_wrongUser_throwsNotFound에서 생성된 알림의 ID를 조회하기 위해 `findByUserId(Long userId) → List<Notification>` 필요
- Fix: NotificationRepository에 `List<Notification> findByUserId(Long userId)` 추가 (Page 오버로드와 공존)
- Files modified: NotificationRepository.java

## TDD Gate Compliance

- RED gate commit: `1020a51` (test(04-02): add failing tests...)
- GREEN gate commit: `96485b6` (feat(04-02): implement...)
- REFACTOR: 불필요 — 코드 정리 없이 GREEN 달성

## Threat Model Coverage

| Threat ID | Mitigation | Status |
|-----------|-----------|--------|
| T-04-02-01 | findByIdAndUserId — userId 검증 필수 | DONE |
| T-04-02-03 | CommentService try-catch — 알림 실패 비중단 | DONE |

## Known Stubs

None.

## Self-Check: PASSED

- Notification.java: EXISTS
- NotificationRepository.java: EXISTS
- NotificationDto.java: EXISTS
- NotificationService.java: EXISTS
- NotificationServiceTest.java: EXISTS
- Commits 1020a51, 96485b6, 3075cd4: FOUND in git log
- NotificationServiceTest BUILD SUCCESS: 4/4 tests PASS
- CommentService compile BUILD SUCCESS: CONFIRMED
