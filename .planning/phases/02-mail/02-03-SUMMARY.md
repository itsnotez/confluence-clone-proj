---
phase: 02-mail
plan: 03
subsystem: backend
tags: [mail, message, convert, page, rest-api, integration-test]
dependency_graph:
  requires: [Phase 02-01 (MailAccount CRUD), Phase 02-02 (MailMessage 엔티티 + IMAP 동기화)]
  provides: [메일 메시지 조회 API, 메일→페이지 변환 API]
  affects: [mail inbox UI (향후), content tree]
tech_stack:
  added: []
  patterns: [Response.from() 정적 팩토리, @Transactional readOnly 최적화, JSON 본문 직접 생성]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/mail/dto/MailMessageDto.java
    - backend/src/main/java/com/company/wiki/mail/service/MailMessageService.java
    - backend/src/main/java/com/company/wiki/mail/controller/MailMessageController.java
    - backend/src/test/java/com/company/wiki/mail/controller/MailMessageControllerTest.java
  modified:
    - backend/src/main/java/com/company/wiki/common/exception/ErrorCode.java
decisions:
  - "Content.createdBy는 User 엔티티 참조 — MailMessageService에 UserRepository 주입하여 User 객체 조회 후 Content.builder().createdBy(author) 설정"
  - "ContentVersion.author도 User 엔티티 참조 — 동일 author User 객체 재사용"
  - "Content.position은 nullable false이므로 0으로 고정 (메일 변환 페이지는 루트 레벨)"
  - "findByAccount는 @Transactional(readOnly=true) — 조회 성능 최적화"
  - "bodyText JSON 이스케이프: \\, \", \\n, \\r 처리로 JSON 파싱 오류 방지"
metrics:
  duration: 18분
  completed: 2026-07-06
---

# Phase 2 Plan 03: 메일 메시지 조회 + 페이지 변환 API Summary

**한 줄 요약:** 수신 메일 목록 조회 REST API + 메일을 Wiki 페이지(Content + ContentVersion)로 원클릭 변환하는 서비스 구현 완료, 중복 변환 409 방지 포함

## What Was Built

메일 메시지 조회 및 Wiki 페이지 변환을 위한 DTO, 서비스, 컨트롤러 계층을 완전히 구현했다.

- **MailMessageDto.Response**: MailMessage 엔티티의 REST 응답 DTO, `bodyPreview`(앞 500자), `static from(MailMessage)` 팩토리 포함
- **MailMessageDto.ConvertRequest**: 빈 body (제목/본문은 메시지에서 자동 추출)
- **MailMessageDto.ConvertResponse**: contentId, contentTitle, message 반환
- **ErrorCode 확장**: `MAIL_MESSAGE_NOT_FOUND` (404), `ALREADY_CONVERTED` (409) 추가
- **MailMessageService.findByAccount()**: Space 권한(READ 이상) 검증 후 메시지 목록 최신순 반환
- **MailMessageService.convertToPage()**: SPACE_ADMIN 권한 검증 → Content + ContentVersion 트랜잭션 생성 → linkedContentId + status="CONVERTED" 업데이트
- **MailMessageController**: GET `/messages`, POST `/messages/{msgId}/convert` 엔드포인트
- **MailMessageControllerTest**: 4개 통합 테스트 모두 통과 (빈목록, 메시지 조회, 변환 성공, 중복 변환 409)

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | MailMessageDto | 4bbbce3 | MailMessageDto.java |
| 2 | MailMessageService + ErrorCode | f3ab44e | MailMessageService.java, ErrorCode.java |
| 3 | MailMessageController | 2406f7e | MailMessageController.java |
| 4 | MailMessageController 통합 테스트 | 47b3b3a | MailMessageControllerTest.java |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ConvertRequest 중복 생성자 컴파일 오류**
- **Found during:** Task 1 컴파일 검증 중
- **Issue:** 빈 클래스에 `@NoArgsConstructor` + `@AllArgsConstructor` 동시 사용 시 매개변수가 없는 생성자가 중복 정의되어 컴파일 오류 발생
- **Fix:** `@AllArgsConstructor` 제거, `@NoArgsConstructor`만 유지
- **Files modified:** MailMessageDto.java
- **Commit:** 4bbbce3

**2. [Rule 2 - 누락 기능] Content.createdBy User 참조 반영**
- **Found during:** Task 2 구현 중 Content 엔티티 구조 확인
- **Issue:** Plan에서는 `authorId(userId)` 직접 설정을 가정했으나, Content 엔티티의 `createdBy`는 `@ManyToOne` User 엔티티 참조로 `nullable = false`
- **Fix:** MailMessageService에 `UserRepository` 주입 추가, `userId`로 User 조회 후 `Content.builder().createdBy(author)` 설정; ContentVersion의 `author`도 동일 처리
- **Files modified:** MailMessageService.java
- **Commit:** f3ab44e

## Known Stubs

없음 — 모든 기능이 실제 DB + Spring Context 기반으로 동작함.

## Verification Results

- `MailMessageControllerTest`: Tests run: 4, Failures: 0, Errors: 0 — BUILD SUCCESS
- 409 CONFLICT 중복 변환 방지 확인
- 전체 컴파일: BUILD SUCCESS

## Self-Check: PASSED
- MailMessageDto.java: 존재 확인
- MailMessageService.java: 존재 확인
- MailMessageController.java: 존재 확인
- MailMessageControllerTest.java: 존재 확인
- 커밋 4bbbce3, f3ab44e, 2406f7e, 47b3b3a: 확인 완료
