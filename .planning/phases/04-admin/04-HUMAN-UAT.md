---
status: partial
phase: 04-admin
source: [04-VERIFICATION.md]
started: 2026-07-07T09:00:00Z
updated: 2026-07-07T09:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. 관리자 대시보드 UI 렌더링 확인

expected: "'관리자 대시보드' 제목, 통계 카드 4개, DxPieChart(메일 서버 상태), DxDataGrid(감사로그 탭)가 브라우저에서 정상 표시. 일반 사용자 접근 시 /spaces로 리다이렉트"
result: [pending]

**Steps:**
1. 백엔드 실행: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw spring-boot:run -pl backend`
2. 프론트엔드 실행: `cd frontend && npm run dev`
3. `http://localhost:3000/login`에서 admin / Admin1234! 로그인
4. `http://localhost:3000/admin` 접속

**Expected:**
- "관리자 대시보드" 제목이 표시된다
- "대시보드" 탭에 통계 카드 4개 (활성 사용자/Space 수/콘텐츠 수/스토리지 사용량)가 표시된다
- 메일 서버 상태 DxPieChart가 렌더링된다
- "감사로그" 탭 클릭 시 DxDataGrid가 표시된다
- 일반 사용자로 /admin 접속 시 /spaces로 리다이렉트된다

---

### 2. 알림 벨 아이콘 + 드롭다운 + 폴링 동작 확인

expected: "벨 아이콘 클릭 시 드롭다운 열림, 배지 숫자 표시, 읽음 처리 시 배지 감소, 30초 폴링이 네트워크 탭에서 중복 없이 1회씩만 호출됨"
result: [pending]

**Steps:**
1. admin 로그인 후 AppHeader의 🔔 아이콘 확인
2. 벨 아이콘 클릭
3. 브라우저 개발자도구 네트워크 탭 관찰 (30초 대기)
4. 알림이 있는 경우 개별 클릭 / "모두 읽음" 클릭

**Expected:**
- AppHeader 우측에 🔔 벨 아이콘이 표시된다
- 미읽음 알림이 있으면 배지에 숫자가 표시된다
- 벨 클릭 시 드롭다운이 열리고 알림 목록이 표시된다
- 개별 클릭 시 읽음 처리되고 배지 카운트가 감소한다
- "모두 읽음" 클릭 시 배지가 0이 된다
- 네트워크 탭에서 `/api/notifications/unread-count`가 30초마다 1회씩만 호출된다

---

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
