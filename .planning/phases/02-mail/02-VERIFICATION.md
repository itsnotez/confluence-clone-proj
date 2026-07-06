---
phase: 02-mail
verified: 2026-07-06T23:00:00Z
status: human_needed
score: 8/9 must-haves verified
overrides_applied: 0
human_verification:
  - test: "메일함 UI에서 발신자 컬럼이 정상 표시되는지 확인"
    expected: "DxDataGrid 발신자 컬럼에 이메일 주소가 표시됨 (현재 fromAddress로 바인딩되나 백엔드는 sender 필드 반환)"
    why_human: "백엔드 MailMessageDto.Response는 sender 필드를 반환하지만 MailBoxView.vue는 fromAddress 필드를 참조 — 런타임에서 컬럼이 공백으로 표시될 수 있으나 빌드 오류 없이 통과하므로 시각적 확인 필요"
---

# Phase 02-mail: IMAP Polling 기반 메일 동기화 및 메일함 UI 검증 보고서

**Phase Goal:** IMAP Polling 기반 메일 동기화 및 메일함 UI
**Verified:** 2026-07-06T23:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | POST /spaces/{key}/mail-accounts 로 메일 계정을 등록할 수 있다 | ✓ VERIFIED | `MailAccountController.java:41` — `@PostMapping` + `mailAccountService.create(...)` 실제 구현 |
| 2 | credential 필드는 AES-256으로 암호화되어 DB에 저장된다 | ✓ VERIFIED | `AesEncryptUtil.java:38` — AES/CBC/PKCS5Padding 실제 구현, `MailAccountService.java:52` — `aesEncryptUtil.encrypt(req.getPassword())` 호출 후 저장 |
| 3 | ImapService.fetchNewMessages()가 JavaMail로 IMAP 메시지를 fetch한다 | ✓ VERIFIED | `ImapService.java:33~113` — 139줄 실제 구현, `jakarta.mail.*` import, `UIDFolder.getUID()` 사용 |
| 4 | MailPollingScheduler가 5분 주기로 syncAll()을 실행한다 | ✓ VERIFIED | `MailPollingScheduler.java:20` — `@Scheduled(fixedDelay = 300000)`, `WikiApplication.java:8` — `@EnableScheduling` |
| 5 | 동기화 3회 연속 실패 시 syncStatus가 DISABLED로 변경된다 | ✓ VERIFIED | `MailSyncService.java:39~65` — while(retries < 3) 루프, retries >= 3시 DISABLED 설정, `MailSyncServiceTest.java:114` — 테스트 통과 확인 |
| 6 | GET /spaces/{key}/mail-accounts/{accountId}/messages 로 메시지 목록 조회 가능 | ✓ VERIFIED | `MailMessageController.java:33` — `@GetMapping`, `MailMessageService.java:44` — `findByAccount()` 실제 구현 |
| 7 | POST .../messages/{msgId}/convert 로 Page 변환 가능하고 linkedContentId가 업데이트된다 | ✓ VERIFIED | `MailMessageController.java:46` — `@PostMapping("/{msgId}/convert")`, `MailMessageService.java:68~143` — Content + ContentVersion 생성 후 `msg.setLinkedContentId(content.getId())` |
| 8 | MailBoxView가 DevExtreme DxDataGrid로 메일 목록을 렌더링한다 | ✓ VERIFIED | `MailBoxView.vue:28~48` — `DxDataGrid`, `DxColumn`, `DxSelection`, `DxPaging` 컴포넌트 사용, `mailStore.messages` 바인딩 |
| 9 | /spaces/:spaceKey/mail 경로로 접근 가능하다 | ✓ VERIFIED | `router/index.js:13` — `{ path: '/spaces/:spaceKey/mail', name: 'MailBox', component: () => import('@/views/mail/MailBoxView.vue'), meta: { requiresAuth: true } }` |

**Score:** 8/9 truths technically verified (Truth #8 보류 — 발신자 필드 런타임 불일치)

---

### Required Artifacts

| Artifact | 최소 줄 | 실제 줄 | Status | 비고 |
|----------|---------|---------|--------|------|
| `backend/.../common/util/AesEncryptUtil.java` | 30 | 71 | ✓ VERIFIED | encrypt/decrypt 완전 구현 |
| `backend/.../mail/entity/MailAccount.java` | 40 | 69 | ✓ VERIFIED | 모든 필드 + @PrePersist |
| `backend/.../mail/service/ImapService.java` | 60 | 139 | ✓ VERIFIED | JavaMail IMAP fetch 완전 구현 |
| `backend/.../mail/service/MailSyncService.java` | 70 | 80 | ✓ VERIFIED | syncAccount + syncAll + 재시도 로직 |
| `backend/.../mail/service/MailMessageService.java` | 60 | 145 | ✓ VERIFIED | findByAccount + convertToPage 완전 구현 |
| `frontend/src/views/mail/MailBoxView.vue` | 80 | 235 | ✓ VERIFIED | DxDataGrid 실제 렌더링 로직 포함 |
| `frontend/src/api/mail.js` | - | 12 | ✓ VERIFIED | mailAccountApi + mailMessageApi export |
| `frontend/src/stores/mail.js` | - | 62 | ✓ VERIFIED | Pinia defineStore, 4개 액션 |

---

### Key Link Verification

| From | To | Via | Status | 비고 |
|------|----|-----|--------|------|
| `MailAccountController` | `MailAccountService` | `mailAccountService.create()` | ✓ WIRED | 컨트롤러 → 서비스 연결 확인 |
| `MailAccountService` | `AesEncryptUtil` | `aesEncryptUtil.encrypt()` | ✓ WIRED | `MailAccountService.java:52` |
| `MailAccountService` | `MailAccountRepository` | `mailAccountRepository.save()` | ✓ WIRED | 암호화 후 저장 |
| `MailPollingScheduler` | `MailSyncService` | `mailSyncService.syncAll()` | ✓ WIRED | `MailPollingScheduler.java:22` |
| `MailSyncService` | `ImapService` | `imapService.fetchNewMessages()` | ✓ WIRED | `MailSyncService.java:41` |
| `MailSyncService` | `MailMessageRepository` | `existsByMailAccountIdAndMessageUid()` | ✓ WIRED | 중복 방지 체크 |
| `MailMessageController` | `MailMessageService` | `mailMessageService.convertToPage()` | ✓ WIRED | `MailMessageController.java:51` |
| `MailMessageService` | `ContentRepository` | `contentRepository.save(content)` | ✓ WIRED | `MailMessageService.java:121` |
| `MailMessageService` | `MailMessage.linkedContentId` | `msg.setLinkedContentId(content.getId())` | ✓ WIRED | `MailMessageService.java:137` |
| `MailBoxView.vue` | `useMailStore` | `mailStore.fetchAccounts()` | ✓ WIRED | onMounted에서 호출 |
| `useMailStore` | `mailMessageApi` | `mailMessageApi.getMessages()` | ✓ WIRED | `mail.js store:30` |
| `MailBoxView.vue` | `DxDataGrid` | `:data-source="mailStore.messages"` | ✓ WIRED | `MailBoxView.vue:31` |
| `MailBoxView.vue(fromAddress)` | `MailMessageDto.Response(sender)` | `data-field="fromAddress"` | ✗ MISMATCH | 백엔드는 `sender` 반환, 프론트엔드는 `fromAddress` 참조 |

---

### Data-Flow Trace (Level 4)

| Artifact | 데이터 변수 | 소스 | 실제 데이터 | Status |
|----------|-----------|------|-----------|--------|
| `MailBoxView.vue` | `mailStore.messages` | `GET /spaces/.../messages` → DB | `findByMailAccountIdOrderByReceivedAtDesc()` → DB 조회 | ✓ FLOWING |
| `MailBoxView.vue` | `mailStore.accounts` | `GET /spaces/.../mail-accounts` → DB | `findBySpaceId()` → DB 조회 | ✓ FLOWING |
| `MailBoxView.vue` DxColumn `fromAddress` | `mailStore.selectedMessage.fromAddress` | `MailMessageDto.Response.sender` | DB `sender` 컬럼 → DTO `sender` 필드 반환 | ✗ HOLLOW_PROP — `fromAddress` 속성 없음 |

---

### Behavioral Spot-Checks

| 동작 | 확인 방법 | 결과 | Status |
|------|----------|------|--------|
| AES 키 길이 검증 | `echo -n "WikiAesKey256BitSecretKey1234567" \| wc -c` | 32바이트 | ✓ PASS |
| AES IV 길이 검증 | `echo -n "WikiAesIV16Bytes" \| wc -c` | 16바이트 | ✓ PASS |
| `@EnableScheduling` 어노테이션 | `WikiApplication.java:8` | 존재 확인 | ✓ PASS |
| `@Scheduled(fixedDelay=300000)` | `MailPollingScheduler.java:20` | 존재 확인 | ✓ PASS |
| ErrorCode.ALREADY_CONVERTED | `ErrorCode.java:17` | `HttpStatus.CONFLICT` | ✓ PASS |
| `MailBox` 라우트 | `router/index.js:13` | `/spaces/:spaceKey/mail` 존재 | ✓ PASS |
| SpaceSidebar 메일함 링크 | `SpaceSidebar.vue:9,34` | `goMailBox()` → `MailBox` 라우트 | ✓ PASS |

---

### Requirements Coverage

| Requirement | 담당 플랜 | 구현 상태 | 근거 |
|-------------|---------|----------|------|
| REQ-MAIL-ACCOUNT | 02-01 | ✓ SATISFIED | CRUD API + AES-256 암호화 완전 구현 |
| REQ-MAIL-SYNC | 02-02 | ✓ SATISFIED | ImapService + MailSyncService + MailPollingScheduler 완전 구현 |
| REQ-MAIL-MESSAGE | 02-03 | ✓ SATISFIED | 메시지 조회 + 페이지 변환 API 완전 구현 |
| REQ-MAIL-FRONTEND | 02-04 | ⚠ PARTIAL | MailBoxView DxDataGrid 구현됨, 단 발신자 컬럼 필드명 불일치 |

---

### Anti-Patterns Found

| 파일 | 라인 | 패턴 | 심각도 | 영향 |
|------|-----|------|-------|------|
| `MailBoxView.vue` | 39, 58 | `data-field="fromAddress"` — DTO는 `sender` 반환 | ⚠ Warning | 발신자 컬럼이 런타임에서 공백으로 표시됨 (빌드 오류 없음) |
| `MailAccountController.java` | 30 | `getGroupIds()` 항상 `List.of()` 반환 | ℹ Info | 플랜 명시 사항 — 그룹 권한은 추후 통합 예정 |

---

### Human Verification Required

#### 1. 메일함 발신자 컬럼 표시 확인

**Test:** `/spaces/{spaceKey}/mail` 경로로 접근 후 메일 계정을 선택하고, 메시지 목록에서 "발신자" 컬럼이 실제로 데이터를 표시하는지 확인

**Expected:** 발신자 이메일 주소가 표시되어야 함

**Why human:** 백엔드 `MailMessageDto.Response`는 `sender` 필드를 반환하지만, `MailBoxView.vue`는 `data-field="fromAddress"`로 바인딩함. Vue/DevExtreme은 존재하지 않는 필드 참조 시 빌드 오류 없이 공백 렌더링. 실제 IMAP 메일 데이터가 없는 환경에서는 빈 그리드로 보일 수 있어 런타임 확인 필요.

**수정이 필요한 경우:** `MailBoxView.vue` 39번 줄 `data-field="fromAddress"` → `data-field="sender"`, 58번 줄 `mailStore.selectedMessage.fromAddress` → `mailStore.selectedMessage.sender`로 변경

---

### Gaps Summary

**필드명 불일치 (sender vs fromAddress):**

`MailMessageDto.Response`(백엔드)는 `sender` 필드를 반환하지만 `MailBoxView.vue`(프론트엔드)는 `fromAddress`로 참조합니다. 이는 SUMMARY.md에서 "fromAddress 필드명 사용 (백엔드 02-03 응답 필드명과 일치 — sender 아님)"으로 명시된 의도적 결정으로 보이나, 실제 백엔드 DTO 확인 결과 `sender` 필드임이 확인되었습니다. 빌드는 통과하지만 런타임에서 발신자 컬럼이 비어있게 됩니다.

이 문제는 2줄 수정으로 해결 가능합니다:
- `MailBoxView.vue:39` `fromAddress` → `sender`
- `MailBoxView.vue:58` `fromAddress` → `sender`

---

_Verified: 2026-07-06T23:00:00Z_
_Verifier: Claude (gsd-verifier)_
