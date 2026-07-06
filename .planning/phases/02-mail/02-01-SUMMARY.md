---
phase: 02-mail
plan: 01
subsystem: backend
tags: [mail, aes, encryption, crud, spring-boot]
dependency_graph:
  requires: [Phase 01 완료 (Space/Permission API)]
  provides: [MailAccount CRUD API, AES-256 자격증명 암호화]
  affects: [mail polling (02-02 예정)]
tech_stack:
  added: [AES/CBC/PKCS5Padding]
  patterns: [JPA Entity + Repository, Spring MVC Controller, Service 계층 권한 검사]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/common/util/AesEncryptUtil.java
    - backend/src/main/java/com/company/wiki/mail/entity/MailAccount.java
    - backend/src/main/java/com/company/wiki/mail/repository/MailAccountRepository.java
    - backend/src/main/java/com/company/wiki/mail/dto/MailAccountDto.java
    - backend/src/main/java/com/company/wiki/mail/service/MailAccountService.java
    - backend/src/main/java/com/company/wiki/mail/controller/MailAccountController.java
    - backend/src/test/java/com/company/wiki/mail/controller/MailAccountControllerTest.java
  modified:
    - backend/src/main/resources/application.yml
decisions:
  - "AES 기본 키 32바이트 / IV 16바이트로 정확히 설정 (기본값 오타 수정)"
  - "MailAccountController의 getGroupIds()는 List.of() 반환 — 그룹 권한 통합은 추후 계획"
metrics:
  duration: 7분
  completed: 2026-07-06
---

# Phase 2 Plan 01: 메일 계정 CRUD + AES-256 자격증명 암호화 Summary

**한 줄 요약:** AES-256/CBC/PKCS5Padding 암호화로 자격증명을 안전하게 저장하는 IMAP 메일 계정 등록/조회/삭제 REST API 구현 완료

## What Was Built

IMAP 메일 동기화의 기반이 되는 메일 계정 관리 API와 AES-256 자격증명 암호화를 구현했다.

- **AesEncryptUtil**: `encrypt(plaintext)` / `decrypt(ciphertext)` — AES/CBC/PKCS5Padding, Base64 인코딩 반환
- **MailAccount 엔티티**: mail_accounts 테이블 매핑, @PrePersist로 기본값 설정
- **MailAccountRepository**: findBySpaceId, findBySyncStatus, findByIdAndSpaceId
- **MailAccountService**: create(AES 암호화), findBySpace, delete, updateSyncStatus — isSpaceAdmin 권한 검사
- **MailAccountController**: GET/POST/DELETE 엔드포인트, /spaces/{key}/mail-accounts
- **통합 테스트 4개**: 생성 성공, 목록 조회, 삭제, 자격증명 암호화 검증 — 모두 통과

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | AesEncryptUtil | 9476a7f | AesEncryptUtil.java, application.yml |
| 2 | MailAccount 엔티티 + 레포지토리 | 95e84e1 | MailAccount.java, MailAccountRepository.java |
| 3 | MailAccountDto + MailAccountService | f78badd | MailAccountDto.java, MailAccountService.java |
| 4 | MailAccountController + 통합 테스트 | 576bf26 | MailAccountController.java, MailAccountControllerTest.java |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] AES 기본 키/IV 길이 오류 수정**
- **Found during:** Task 4 테스트 실행 중
- **Issue:** 플랜에서 지정한 기본 secretKey `WikiAesKey256BitSecretKey12345678`는 33바이트(AES-256은 32바이트 필요), `WikiAesIV16Bytes!`는 17바이트(CBC는 16바이트 필요)였음. `RuntimeException: AES 암호화 실패` 발생
- **Fix:** `WikiAesKey256BitSecretKey1234567` (32바이트), `WikiAesIV16Bytes` (16바이트)로 수정
- **Files modified:** backend/src/main/resources/application.yml
- **Commit:** 576bf26 (Task 4와 함께 커밋)

## Known Stubs

- `getGroupIds()` in MailAccountController: 항상 `List.of()` 반환. 그룹 기반 권한 검사는 향후 통합 예정 (플랜 명시 사항)

## Verification Results

- `MailAccountControllerTest`: Tests run: 4, Failures: 0, Errors: 0 — BUILD SUCCESS
- credential 필드: DB에 저장 시 Base64 인코딩된 AES 암호문 저장 확인 (평문 "pass"와 다름)

## Self-Check: PASSED
