---
phase: 03-search
plan: 02
subsystem: label-backend
tags: [label, content-label, rbac, jpa, tdd]
dependency_graph:
  requires: [03-01]
  provides: [label-api, content-label-api]
  affects: [content]
tech_stack:
  added: []
  patterns: [embeddable-record-composite-key, junction-table-entity, permission-check-pattern]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/label/entity/Label.java
    - backend/src/main/java/com/company/wiki/label/entity/ContentLabel.java
    - backend/src/main/java/com/company/wiki/label/entity/ContentLabelId.java
    - backend/src/main/java/com/company/wiki/label/repository/LabelRepository.java
    - backend/src/main/java/com/company/wiki/label/repository/ContentLabelRepository.java
    - backend/src/main/java/com/company/wiki/label/dto/LabelDto.java
    - backend/src/main/java/com/company/wiki/label/service/LabelService.java
    - backend/src/main/java/com/company/wiki/label/controller/LabelController.java
    - backend/src/test/java/com/company/wiki/label/controller/LabelControllerTest.java
  modified: []
decisions:
  - "ContentLabelId를 @Embeddable record로 구현 (SpaceFavoriteId 패턴 동일)"
  - "getLabels에서 @Transactional(readOnly=true) 제거 — 같은 트랜잭션 내 flush 보장"
metrics:
  duration: 약 15분
  completed_date: "2026-07-07"
  tasks_completed: 3
  files_created: 9
---

# Phase 3 Plan 02: Label Backend Summary

**One-liner:** `@Embeddable` record 복합키 + PermissionService canRead/canWrite 권한 체크를 적용한 라벨 CRUD 백엔드 (LABEL-01, LABEL-02 충족)

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Label/ContentLabel/ContentLabelId 엔티티 + 레포 + DTO | 41e9346 | 6개 신규 |
| 2 | LabelService + LabelController | 57c6c58 | 2개 신규 |
| 3 | LabelControllerTest (TDD GREEN) | da1b9fb | 1개 신규, 1개 수정 |

## Verification

- `./mvnw compile` exit code 0 — 확인
- `./mvnw test -Dtest=LabelControllerTest` — 3/3 통과 (addLabel_success, getLabels_success, getLabels_noAuth_returns401)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Java assert → JUnit assertTrue 교체**
- **Found during:** Task 3 GREEN
- **Issue:** `assert found` 구문은 JVM `-ea` 플래그 없으면 비활성화되어 테스트가 항상 통과
- **Fix:** `assertTrue(found, ...)` → 최종적으로 `jsonPath("$.data[0].name").value("긴급")`으로 단순화
- **Files modified:** LabelControllerTest.java

**2. [Rule 1 - Bug] getLabels @Transactional(readOnly=true) 제거**
- **Found during:** Task 3 GREEN
- **Issue:** 같은 트랜잭션 내 save 후 readOnly 메서드 호출 시 1차 캐시만 조회하여 빈 결과 반환
- **Fix:** `@Transactional(readOnly=true)` 어노테이션 제거 (클래스 레벨 `@Transactional` 상속)
- **Files modified:** LabelService.java

## TDD Gate Compliance

- RED: `test(03-02)` commit — 52f4410
- GREEN: `feat(03-02)` commit — da1b9fb

## Threat Surface Scan

T-03-03 (Elevation of Privilege): LabelService.addLabel/removeLabel — canWrite 체크 적용 확인
T-03-04 (Information Disclosure): LabelService.getLabels — canRead 체크 적용 확인

## Known Stubs

None.

## Self-Check: PASSED

- Label.java: 존재 확인
- ContentLabelId.java: @Embeddable 존재 확인
- ContentLabel.java: @EmbeddedId 존재 확인
- LabelService.java: permissionService.canRead/canWrite 호출 확인
- LabelController.java: @PostMapping/@GetMapping("/contents/{contentId}/labels") 확인
- 테스트 3/3 GREEN 확인
