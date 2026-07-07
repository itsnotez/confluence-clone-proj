---
phase: 04-admin
plan: "04"
subsystem: notification-api
tags: [notification, rest-api, controller, spring-boot, tdd, idor-prevention]
dependency_graph:
  requires:
    - phase: 04-02
      provides: [NotificationService, NotificationRepository, NotificationEntity, NotificationDto]
  provides: [NotificationController, GET /notifications, GET /notifications/unread-count, PATCH /notifications/{id}/read, PATCH /notifications/read-all]
  affects: [04-06]
tech_stack:
  added: []
  patterns: [SpringBoot-Controller, @AuthenticationPrincipal, TDD-SpringBootTest]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/notification/controller/NotificationController.java
    - backend/src/test/java/com/company/wiki/notification/controller/NotificationControllerTest.java
  modified: []
key_decisions:
  - "markRead_otherUser_returns404 테스트에서 userId=999 FK 제약 위반 → UserRepository로 실제 사용자 생성 후 사용"
  - "IDOR 방지 로직은 NotificationController가 아닌 NotificationService.markAsRead()에 위임 (04-02 구현)"
patterns-established:
  - "Controller패턴: @RestController + @RequestMapping + @RequiredArgsConstructor + getCurrentUserId(UserDetails)"
  - "통합테스트: @SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles(local) + @Transactional"
requirements-completed: [NOTIF-02]
duration: 3min
completed: "2026-07-07"
---

# Phase 4 Plan 04: Notification Controller Summary

**NotificationController 4개 REST 엔드포인트 구현 및 IDOR 방지 통합 테스트 완성 — Wave 2 알림 HTTP API 노출 완료**

## Performance

- **Duration:** 2m 41s
- **Started:** 2026-07-07T07:56:58Z
- **Completed:** 2026-07-07T08:00:59Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- NotificationController 4개 엔드포인트 구현 (GET 목록, GET 미읽음 카운트, PATCH 단건 읽음, PATCH 전체 읽음)
- IDOR 방지 테스트 (markRead_otherUser_returns404) 포함 6개 통합 테스트 모두 GREEN
- Wave 1(04-02)의 NotificationService를 HTTP 레이어로 완전히 노출

## Task Commits

TDD cycle:
1. **RED: NotificationControllerTest 작성 (컴파일 성공, 5/6 실패)** - `aa1bbb9` (test)
2. **GREEN: NotificationController 구현 (6/6 통과)** - `2bb2526` (feat)

## Files Created/Modified
- `backend/src/main/java/com/company/wiki/notification/controller/NotificationController.java` — 4개 엔드포인트 컨트롤러
- `backend/src/test/java/com/company/wiki/notification/controller/NotificationControllerTest.java` — 6개 통합 테스트 (IDOR 방지 포함)

## Decisions Made
- **IDOR 방지 위임:** `markRead_otherUser_returns404` 테스트에서 IDOR 방지는 NotificationController가 아닌 NotificationService.markAsRead()에서 처리 (`findByIdAndUserId`). 컨트롤러는 userId만 전달.
- **테스트 사용자 생성:** 초기 계획의 `userId=999` FK 제약 위반 → UserRepository로 실제 사용자(`notif_other_user`) 생성하는 방식으로 변경.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] FK 제약으로 인한 markRead_otherUser_returns404 테스트 수정**
- **Found during:** Task 1 RED phase 실행 시
- **Issue:** `notifications.user_id`에 FK 제약이 있어 userId=999(존재하지 않는 사용자)로 Notification 저장 시 DataIntegrityViolation 발생
- **Fix:** `UserRepository` + `PasswordEncoder` @Autowired 추가, `createOtherUser()` 헬퍼로 실제 DB 사용자 생성 후 해당 userId로 알림 생성
- **Files modified:** NotificationControllerTest.java
- **Verification:** 6/6 테스트 BUILD SUCCESS
- **Committed in:** `aa1bbb9` (RED commit에서 함께 수정)

---

**Total deviations:** 1 auto-fixed (Rule 1 - Bug)
**Impact on plan:** FK 제약 인식 후 즉시 수정. 테스트 의도 (IDOR 방지 검증) 동일하게 달성.

## Issues Encountered
- RED phase에서 `userId=999` FK 제약 위반 — 즉시 Rule 1로 수정 (위 참고)

## TDD Gate Compliance

- RED gate commit: `aa1bbb9` (test(04-04): add failing tests...)
- GREEN gate commit: `2bb2526` (feat(04-04): implement NotificationController...)
- REFACTOR: 불필요 — 코드 구조 정리 없이 GREEN 달성

## Threat Model Coverage

| Threat ID | Mitigation | Status |
|-----------|-----------|--------|
| T-04-04-01 | JWT userId 기반 쿼리 — 다른 사용자 알림 조회 불가 | DONE (NotificationService에서) |
| T-04-04-02 | NotificationService.markAsRead()에서 findByIdAndUserId 소유자 검증 | DONE (04-02 구현 + 테스트 검증) |
| T-04-04-03 | Spring Security 전역 인증 필터 — 401 반환 확인 | DONE (getNotifications_unauthenticated_returns401) |

## Known Stubs

None.

## Next Phase Readiness
- Wave 2 알림 HTTP API 완성 — 04-06(알림 벨 프론트엔드)이 소비하는 4개 엔드포인트 준비 완료
- `/notifications`, `/notifications/unread-count`, `/notifications/{id}/read`, `/notifications/read-all` 모두 사용 가능
