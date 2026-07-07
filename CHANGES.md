# 변경사항 요약

## 작업 일자: 2026-07-08

---

## 1. 새 스페이스 만들기 — SITE_ADMIN 전용 제한

### 목적
일반 사용자가 스페이스를 생성하지 못하도록 제한.

### 변경 내용
- **`frontend/src/views/space/SpaceListView.vue`**
  - `useAuthStore` import 추가
  - "새 Space 만들기" 버튼에 `v-if="auth.user?.role === 'SITE_ADMIN'"` 조건 추가
- **`backend/.../SpaceController.java`**
  - `@PostMapping` (createSpace) 메서드에 `@PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")` 추가

---

## 2. 스페이스 목록 행 선택 하이라이트

### 목적
마우스로 선택한 스페이스 행을 시각적으로 구분.

### 변경 내용
- **`frontend/src/views/space/SpaceListView.vue`**
  - `DxSelection` 컴포넌트 추가, `selectedKeys` ref 추가
  - `onRowClick` 핸들러로 클릭 시 선택 상태 업데이트
  - `:deep()` CSS 스타일: 호버(#e8f0fe), 선택(#1976d2 파란색/흰색)

---

## 3. 메일 계정 관리 탭 추가 (Admin Dashboard)

### 목적
Admin 대시보드에서 스페이스별 IMAP 메일 계정 등록/삭제/수동동기화.

### 변경 내용
- **`frontend/src/views/admin/AdminDashboardView.vue`**
  - "메일 계정 관리" 탭(4번째) 추가
  - 스페이스 선택 → 해당 스페이스의 메일 계정 목록 표시
  - 그리드 컬럼: 이메일, 호스트, 포트, SSL, 동기화 상태 배지, **오류 내용**(빨간 텍스트), 마지막 동기화 시각, 동기화/삭제 버튼
  - 계정 등록 팝업: emailAddress, password, imapHost, imapPort, imapSsl (DxCheckBox), smtpHost, smtpPort

---

## 4. 메일 동기화 오류 메시지 노출

### 목적
동기화 실패 시 어떤 오류인지 Admin에서 확인 가능.

### 변경 내용
- **`backend/src/main/resources/db/migration/V8__mail_account_error_message.sql`**
  - `mail_accounts` 테이블에 `last_error_message TEXT` 컬럼 추가
- **`backend/.../MailAccount.java`**
  - `lastErrorMessage` 필드 추가
- **`backend/.../MailAccountDto.java`**
  - `Response.lastErrorMessage` 필드 추가 및 `from()` 메서드에 매핑
- **`backend/.../MailSyncService.java`**
  - 성공 시: `lastErrorMessage = null` 초기화
  - 실패 시: `lastErrorMessage` 에 오류 메시지 저장 (getCause 우선)
- **`backend/.../MailAccountController.java`**
  - `POST /{id}/sync` — 수동 동기화 트리거 엔드포인트 추가
- **`frontend/src/api/mail.js`**
  - `syncAccount(spaceKey, id)` API 함수 추가

---

## 5. 메일 발신자 인코딩 수정

### 목적
한글/MIME 인코딩된 발신자 이름 (`=?UTF-8?B?...?=`) 이 깨져서 표시되는 문제 수정.

### 변경 내용
- **`backend/.../ImapService.java`**
  - `decodeSender()` 메서드 추가
    - `InternetAddress.getPersonal()` + `MimeUtility.decodeText()` 로 MIME 인코딩 디코딩
    - personal이 없으면 email 주소 반환

---

## 6. 메일 본문 추출 수정 (HTML 이메일 + Daum IMAP 호환)

### 목적
메일함에서 메일 클릭 시 우측 본문 영역이 비어있는 문제 수정.

### 원인 분석
1. **HTML 전용 이메일**: `text/plain` 파트만 처리하다가 `text/html` 처리 누락
2. **base64/QP 인코딩**: `getContent()` 가 `InputStream` 반환 시 `.toString()` 으로 주소 문자열 반환
3. **Daum IMAP `BODY.PEEK[]` 버그**: `READ_ONLY` 폴더에서 Daum 서버가 `BODY.PEEK[]` 명령에 빈 응답 반환

### 변경 내용
- **`backend/.../ImapService.java`**
  - `extractText()`: `text/plain` 우선, `text/html` 폴백 (stripHtml), `multipart/*` 재귀 처리
  - `readPartAsString()`: `getContent()` → String/InputStream 분기, 실패 시 `getInputStream()` 폴백
  - `stripHtml()`: `(?si)` 플래그로 멀티라인 `<style>/<script>` 블록 제거, HTML 엔티티 디코딩
  - `detectCharset()`: Content-Type 헤더에서 charset 파싱
  - `MailMessageRepository`: `findByMailAccountIdAndMessageUid()` 추가 (본문 비어있는 기존 메시지 업서트)
  - **`Folder.READ_ONLY` → `Folder.READ_WRITE`** 변경
    - Daum IMAP 서버가 READ_ONLY 폴더의 `BODY.PEEK[]` 에 빈 응답 반환하는 버그 우회
    - READ_WRITE 모드에서는 `BODY[]` 명령 사용 → 정상 수신

### 결과
- 54개 메시지 중 50개 본문 추출 성공 (나머지 4개는 텍스트 없는 이미지 전용 이메일)

---

## 기술 스택

- **Frontend**: Vue 3, Vite, DevExtreme (`DxDataGrid`, `DxTabPanel`, `DxPopup`, `DxCheckBox`)
- **Backend**: Spring Boot 3, Jakarta Mail (Angus Mail), JPA/Flyway
- **인증**: Spring Security, `@PreAuthorize`
- **IMAP**: Jakarta Mail, `imaps` (SSL 993), Daum IMAP 서버 호환
