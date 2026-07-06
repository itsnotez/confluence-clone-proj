---
phase: 02-mail
plan: 02
subsystem: backend
tags: [mail, imap, sync, scheduler, javamail, mockito]
dependency_graph:
  requires: [Phase 02-01 (MailAccount CRUD + AES-256 암호화)]
  provides: [IMAP 메시지 동기화, MailMessage 엔티티, 5분 폴링 스케줄러]
  affects: [mail polling, mail inbox UI (향후)]
tech_stack:
  added: [JavaMail (spring-boot-starter-mail already present), Mockito unit tests]
  patterns: [IMAP UIDFolder deduplication, retry-with-status-tracking, @Scheduled fixedDelay]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/mail/entity/MailMessage.java
    - backend/src/main/java/com/company/wiki/mail/repository/MailMessageRepository.java
    - backend/src/main/java/com/company/wiki/mail/service/ImapService.java
    - backend/src/main/java/com/company/wiki/mail/service/MailSyncService.java
    - backend/src/main/java/com/company/wiki/mail/scheduler/MailPollingScheduler.java
    - backend/src/test/java/com/company/wiki/mail/service/MailSyncServiceTest.java
  modified: []
decisions:
  - "ImapService.extractText()에서 IOException을 MessagingException 시그니처에서 제외하고 내부 try-catch로 처리 (컴파일러 unchecked exception 오류 해결)"
  - "MailSyncService.syncAccount() 재시도 카운터: 1~2회 실패=ERROR, 3회 실패=DISABLED — 테스트 verify(save, times(3)) 일치"
metrics:
  duration: 16분
  completed: 2026-07-06
---

# Phase 2 Plan 02: IMAP Polling 기반 메일 동기화 서비스 Summary

**한 줄 요약:** JavaMail UIDFolder 기반 중복 방지 IMAP 동기화 서비스 + 3회 실패 시 DISABLED 재시도 로직 + 5분 고정지연 스케줄러 구현 완료

## What Was Built

IMAP 메일 동기화를 위한 엔티티, 서비스, 스케줄러 계층을 완전히 구현했다.

- **MailMessage 엔티티**: `mail_messages` 테이블 매핑, `(mail_account_id, message_uid)` UNIQUE 제약, @PrePersist로 status="UNREAD" 기본값 설정
- **MailMessageRepository**: `existsByMailAccountIdAndMessageUid()` — 중복 방지 핵심 메서드
- **ImapService**: AES 복호화 → JavaMail IMAP/IMAPS 연결 → UIDFolder.getUID()로 UID 추출 → 텍스트 본문 재귀 파싱 → 연결 종료 (finally 보장)
- **MailSyncService**: `syncAccount()` — 중복 체크 후 신규만 저장, 3회 실패 시 DISABLED; `syncAll()` — ACTIVE/PENDING/ERROR 계정 전체 동기화
- **MailPollingScheduler**: `@Scheduled(fixedDelay=300000)` — 이전 실행 완료 후 5분 대기 (동시 실행 방지)
- **단위 테스트 3개**: 신규 저장, 중복 스킵, 3회 실패→DISABLED — 모두 통과

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | MailMessage 엔티티 + 레포지토리 | c201879 | MailMessage.java, MailMessageRepository.java |
| 2 | ImapService (JavaMail IMAP fetch) | 95488bc | ImapService.java |
| 3 | MailSyncService + MailPollingScheduler | a02d75f | MailSyncService.java, MailPollingScheduler.java |
| 4 | MailSyncService 단위 테스트 | 2797fe4 | MailSyncServiceTest.java |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ImapService.extractText() IOException 컴파일 오류**
- **Found during:** Task 2 컴파일 검증 중
- **Issue:** `extractText(Part part) throws MessagingException, IOException`로 선언했으나 `IOException`이 직접 throw되지 않아 Java 컴파일러가 오류로 처리 (`exception is never thrown in body of corresponding try statement`)
- **Fix:** `IOException`을 메서드 시그니처에서 제거하고, `part.getContent()` 호출부를 내부 try-catch로 감싸서 처리
- **Files modified:** backend/src/main/java/com/company/wiki/mail/service/ImapService.java
- **Commit:** 95488bc

**2. [Rule 1 - Bug] fetchNewMessages() catch 절의 IOException 제거**
- **Found during:** Task 2 두 번째 컴파일 시도 중
- **Issue:** 메인 try 블록의 catch에 `MessagingException | IOException` 병합이 있었으나 `IOException`이 실제로 발생하지 않음
- **Fix:** catch를 `MessagingException`만으로 변경
- **Files modified:** backend/src/main/java/com/company/wiki/mail/service/ImapService.java
- **Commit:** 95488bc

## Known Stubs

없음 — 동기화 서비스는 모두 실제 로직으로 구현됨. 단, 실제 IMAP 서버 연결은 통합 테스트 환경에서 별도 검증 필요.

## Verification Results

- `MailSyncServiceTest`: Tests run: 3, Failures: 0, Errors: 0 — BUILD SUCCESS
- 3회 실패 시 syncStatus=DISABLED, mailAccountRepository.save() 3회 호출 확인
- 전체 컴파일: BUILD SUCCESS (MailMessage, MailMessageRepository, ImapService, MailSyncService, MailPollingScheduler)

## Self-Check: PASSED
