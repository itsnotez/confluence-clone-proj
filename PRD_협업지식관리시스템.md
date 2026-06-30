# PRD: 사내 협업 지식관리 시스템 (Confluence-like Platform)

## 0. 문서 정보
| 항목 | 내용 |
|---|---|
| 문서명 | 협업 지식관리 시스템 PRD |
| 작성일 | 2026-06-30 |
| 버전 | v1.0 (Draft) |
| 대상 독자 | 기획/개발/디자인/QA |

---

## 1. 개요 (Overview)

### 1.1 배경
조직 내 지식과 문서가 메신저, 메일, 개인 PC 등에 분산되어 검색·관리·권한통제가 어려운 상황을 해결하기 위해, Confluence와 유사한 사내 협업 지식관리 시스템을 자체 구축한다. Space(공간) 단위로 지식을 그룹화하고, Space별 접근권한과 전용 공용메일함을 연계하여 "하나의 Space = 하나의 팀/프로젝트 작업공간"이 되도록 한다.

### 1.2 목표 (Goals)
- Space 기반의 콘텐츠 그룹화 및 위계적 문서 관리
- Space 단위의 세밀한 사용자/그룹 접근 권한 관리
- Space별 공용 메일계정 연계(수신 메일 → 콘텐츠/스레드 변환)
- Vue3 + DevExtreme 기반의 직관적이고 일관된 UI/UX
- Java21 + Spring Boot + PostgreSQL 기반의 확장 가능한 백엔드 아키텍처

### 1.3 비목표 (Non-Goals, 1차 릴리즈 제외)
- 외부 사용자(고객) 대상 공개 위키 기능
- 실시간 동시편집(OT/CRDT) — 2차 고도화 과제로 분리
- 모바일 네이티브 앱 (반응형 웹으로 대응)
- 외부 SaaS 메일(Gmail/Outlook) OAuth 연동 — 1차는 자체/사내 메일서버(IMAP/SMTP) 연동 우선

---

## 2. 사용자 및 페르소나

| 페르소나 | 설명 | 주요 니즈 |
|---|---|---|
| Site Admin | 시스템 전체 관리자 | 전체 Space/사용자/메일서버 설정 관리 |
| Space Admin | Space 소유자/관리자 | Space 멤버·권한·메일계정 관리 |
| Member | Space 일반 참여자 | 문서 작성/조회/협업 |
| Viewer | 읽기 전용 사용자 | 콘텐츠 조회만 |
| Guest(선택) | 제한적 외부 협력자 | 특정 콘텐츠만 조회 |

---

## 3. 핵심 기능 요구사항 (Functional Requirements)

### 3.1 Space 관리
- Space 생성/수정/삭제(소프트 삭제 + 휴지통), Space Key(고유 식별자), 아이콘/설명/공개여부(Public/Private) 설정
- Space 템플릿(빈 Space, 프로젝트형, 팀 위키형 등) 선택 생성
- Space 계층 없음(평면) 단, Space 내부 콘텐츠는 트리 구조
- Space 즐겨찾기, 최근 방문 Space, Space 검색/필터(DevExtreme DataGrid)
- Space 보관(Archive) 처리: 보관된 Space는 읽기 전용

### 3.2 콘텐츠(문서) 관리
- 콘텐츠 타입: Page(일반 문서), Blog Post(공지/소식), Attachment(첨부파일), Folder(선택)
- 콘텐츠는 Space 하위에 트리(부모-자식) 구조로 무한 depth 등록
- Rich Text 에디터(WYSIWYG, 이미지/표/코드블록/링크/멘션/이모지 지원) — 별도 에디터 라이브러리 도입(TipTap 등) 검토
- 버전 관리: 모든 수정은 버전으로 저장, 버전 비교(Diff), 특정 버전으로 복원
- 콘텐츠 상태: Draft(임시저장) / Published(게시) / Archived
- 댓글(인라인/하단), @멘션, 좋아요/리액션
- 라벨(Label/Tag) 및 라벨 기반 필터링
- 첨부파일 업로드/버전관리/미리보기(이미지, PDF)
- 전체/Space 내 검색(제목, 본문 Full-Text, 작성자, 라벨, 기간 필터) — PostgreSQL `tsvector` 기반 검색(1차), 추후 Elasticsearch 확장 고려
- 콘텐츠 이동(다른 Space/부모로 이동), 복제, 내보내기(PDF/HTML)
- 콘텐츠 변경 알림(워치/구독)

### 3.3 권한 관리 (Permission)
- 권한 레벨(예시): `SPACE_ADMIN`, `WRITE`(작성/편집), `READ`(조회), `NONE`
- 권한 부여 대상: 개인 사용자, 사용자 그룹(Group), 전체(Anonymous/All Members) — 3단계 우선순위 적용
- Space 단위 기본 권한 + 콘텐츠(Page) 단위 권한 오버라이드(상속 vs 제한)
- 권한 변경 이력(Audit Log) 기록
- Site Admin은 모든 Space에 대해 최고 권한 보유(긴급 접근, 단 로그 기록)
- 사용자 그룹 관리(그룹 생성, 그룹원 추가/제거) — Site Admin/Space Admin 권한

### 3.4 Space별 공용 메일 연계
- Space 단위로 1개 이상의 공용 메일계정(예: project-a@company.com) 등록
- 메일서버 연동 방식: IMAP(수신, IDLE/Polling 방식 동기화)
- 수신 메일 처리:
  - 수신함을 Space 내 "Mail" 콘텐츠 타입(또는 별도 메일 탭)으로 동기화
  - 메일 스레드 단위 그룹핑, 첨부파일 자동 추출 및 콘텐츠 첨부파일로 저장
  - 수신 메일 → Page/Task로 전환(Convert to Page) 기능
  - 메일 → 담당자 배정, 라벨링, 읽음/처리상태 관리
  - 수신 메일 검색: 제목, 본문, 발신자, 수신일자, 첨부파일명, 처리상태, 라벨 기준 검색/필터 (Space 내 메일함 및 통합검색 연계)
- 메일계정 자격증명(ID/PW, OAuth 등)은 암호화 저장(예: AES-256, Vault 연동 고려)
- 메일 동기화 스케줄러(Spring Batch/Scheduler) + 장애 시 재시도/알림
- Space 삭제/보관 시 메일 연동 자동 해지 정책

### 3.5 알림/협업
- In-app 알림(멘션, 댓글, 콘텐츠 변경, 메일 수신)
- 활동 피드(Space 별 최근 활동 타임라인)

### 3.6 관리자 기능
- 사용자/그룹 CRUD, 전체 Space 목록/통계
- 메일서버 설정(서버 등록, 연동 상태 모니터링)
- 시스템 사용량 대시보드(Space 수, 콘텐츠 수, 스토리지 사용량) — DevExtreme Chart
- 감사 로그(Audit Log) 조회

---

## 4. 비기능 요구사항 (Non-Functional Requirements)

| 구분 | 요구사항 |
|---|---|
| 성능 | 주요 목록/검색 API 응답 < 500ms (P95), 대용량 첨부 비동기 업로드 |
| 가용성 | 운영 환경 99.5% 이상, 메일 동기화 실패 시 자동 재시도 3회 |
| 보안 | 비밀번호/메일 자격증명 암호화, JWT 기반 인증, RBAC 권한 체크 전 API 적용, XSS/SQL Injection 방어, 첨부파일 바이러스 스캔(선택) |
| 확장성 | Space/콘텐츠 수 증가에 대응하는 인덱스 설계, 추후 검색엔진 분리 가능한 구조 |
| 감사 | 권한변경/콘텐츠삭제/메일연동 등 주요 행위 Audit Log 적재 |
| 다국어 | 1차: 한국어/영어 i18n 지원 |
| 브라우저 | Chrome, Edge 최신 버전 기준 |

---

## 5. 기술 스택 및 아키텍처

### 5.1 기술 스택
- **Frontend**: Vue 3 (Composition API), DevExtreme(DataGrid, TreeList, Scheduler, Chart 등 활용), Pinia(상태관리), Vue Router, Axios
- **Backend**: Java 21, Spring Boot 3.x, Spring Security(JWT/RBAC), Spring Data JPA, Spring Batch/Scheduler(메일 동기화), Spring Mail(JavaMail, IMAP 수신 / 알림 발송용 SMTP)
- **DB**: PostgreSQL 16 (Full-Text Search `tsvector`, JSONB 활용)
- **Storage**: 첨부파일은 Object Storage(S3 호환) 또는 파일서버, 메타데이터는 DB
- **Infra(예시)**: Docker, CI/CD(Jenkins/GitHub Actions), 운영환경은 추후 결정

### 5.2 시스템 구성도(논리)
```
[Vue3 + DevExtreme SPA]
        │ REST API (JWT)
        ▼
[Spring Boot API Server]
   ├─ Auth/Permission Module
   ├─ Space/Content Module
   ├─ Mail Integration Module ── IMAP ──> [사내 메일서버]
   ├─ Search Module (PostgreSQL FTS)
   └─ Audit/Notification Module
        │
        ▼
   [PostgreSQL]      [Object Storage(첨부파일)]
```

### 5.3 인증/인가
- 로그인: 사내 계정(LDAP/AD 연동 또는 자체 계정) → JWT 발급
- 인가: API Method 단위 + Space/Content 단위 권한 체크(AOP 또는 Interceptor)

---

## 6. 데이터 모델 (핵심 엔티티, 초안)

| 엔티티 | 주요 속성 |
|---|---|
| `users` | id, login_id, name, email, status |
| `groups` | id, name, description |
| `group_members` | group_id, user_id |
| `spaces` | id, space_key, name, description, type(public/private), status(active/archived), created_by |
| `space_permissions` | id, space_id, subject_type(user/group/all), subject_id, permission_level |
| `contents` | id, space_id, parent_id, type(page/blog/folder), title, status, current_version_id |
| `content_versions` | id, content_id, version_no, body(richtext/json), author_id, created_at |
| `content_permissions` | id, content_id, subject_type, subject_id, permission_level |
| `attachments` | id, content_id, file_name, storage_path, version, size |
| `labels` / `content_labels` | 라벨 마스터 및 매핑 |
| `comments` | id, content_id, parent_comment_id, body, author_id |
| `mail_accounts` | id, space_id, email_address, imap_host, imap_port, smtp_host, smtp_port, credential(암호화), sync_status |
| `mail_messages` | id, mail_account_id, message_uid, thread_id, subject, sender, received_at, status, linked_content_id |
| `audit_logs` | id, actor_id, action_type, target_type, target_id, detail, created_at |

> 위 모델은 초안이며, 상세 설계 단계에서 정규화/인덱스(특히 `tsvector` 검색 인덱스, 권한 조회용 복합 인덱스) 확정 필요.

---

## 7. 화면 정의 (주요 화면 목록)

| 화면 | 설명 | 주요 DevExtreme 컴포넌트 |
|---|---|---|
| Space 목록 | 전체/내 Space 목록, 검색, 즐겨찾기 | DataGrid |
| Space 홈 | Space 소개, 최근 콘텐츠, 활동 피드 | TileView, List |
| 콘텐츠 트리/뷰어 | 좌측 트리 + 우측 콘텐츠 뷰/에디터 | TreeList + Editor(별도) |
| 권한 관리 | Space/콘텐츠별 권한 매트릭스 관리 | DataGrid(편집모드) |
| 메일 연동 설정 | 메일계정 등록/상태 확인 | Form, DataGrid |
| 메일함(Space 내) | 수신메일 목록, 스레드뷰, 메일→문서 전환 | DataGrid, Popup |
| 관리자 대시보드 | 사용량 통계, Space/사용자 현황 | Chart, PivotGrid |
| 검색 결과 | 통합검색 결과 리스트 | DataGrid/List |

---

## 8. 권한 모델 상세 규칙
1. 콘텐츠 권한이 별도로 설정되지 않으면 Space 기본 권한을 상속한다.
2. 동일 사용자에 대해 "개인 권한"이 "그룹 권한"보다 우선한다.
3. 거부(Deny) 권한은 1차 범위에서는 미지원(허용 누적 방식만 지원), 필요 시 2차 검토.
4. Space Admin은 해당 Space 내 권한/메일계정/멤버를 관리할 수 있으나 시스템 설정은 불가.
5. Site Admin의 모든 접근은 Audit Log에 별도 표시(관리자 접근 로그).

---

## 9. 마일스톤 (제안)

| 단계 | 범위 | 기간(예시) |
|---|---|---|
| Phase 0 | 요구사항 확정, 데이터모델/API 설계, 화면 설계(Figma) | 2주 |
| Phase 1 | 사용자/Space/권한 기본 기능, 콘텐츠 CRUD+버전관리 | 4주 |
| Phase 2 | 메일서버 연동(수신 동기화, 메일→문서 전환) | 4주 |
| Phase 3 | 검색, 라벨, 댓글, 첨부파일 | 3주 |
| Phase 4 | 관리자 대시보드, 알림, 감사로그, 통합 QA | 3주 |
| Phase 5 | 성능/보안 검증, UAT, 운영 이전 | 2주 |

> 실제 기간은 팀 규모/리소스에 따라 조정 필요.

---

## 10. 리스크 및 검토 필요 사항
- **에디터 선택**: Rich Text 편집기(자체 구현 vs TipTap/Quill 등 도입) 결정 필요 — 콘텐츠 본문 저장 포맷(HTML vs JSON Document)에 영향
- **메일 동기화 방식**: IMAP IDLE(실시간) vs 주기적 Polling — 메일서버 사양에 따라 결정
- **검색 성능**: 콘텐츠량 증가 시 PostgreSQL FTS의 한계 → Elasticsearch/OpenSearch 전환 시점 사전 검토
- **메일 계정 보안**: 자격증명 저장 방식(자체 암호화 vs Vault/KMS) 결정 필요
- **동시편집**: 1차는 잠금(Lock) 기반 편집충돌 방지로 시작, 실시간 공동편집은 2차 과제
- **외부 협력자(Guest) 접근**: 1차 범위 포함 여부 재확인 필요

---

## 11. Open Questions (확인 필요)
- 사내 메일서버는 어떤 종류인가요? (Exchange, Postfix/Dovecot, 기타 IMAP/SMTP 표준 서버 여부)
- 사용자 인증은 사내 LDAP/AD와 연동하나요, 자체 계정체계인가요?
- 첨부파일 저장소는 자체 파일서버/NAS인지, S3 호환 Object Storage를 사용할 수 있는지?
- 동시 사용자 규모(예상 동접/총 사용자 수)는 어느 정도인가요?
- Guest(외부 협력자) 접근 기능이 1차 범위에 필요한가요?
