# Phase 4: 관리자 대시보드·알림·감사로그 - Research

**Researched:** 2026-07-07
**Domain:** Spring Boot AOP / Admin API / In-app Notification / Audit Log / Vue3 DevExtreme Dashboard
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**관리자 대시보드**
- Site Admin 전용 (`ROLE_SITE_ADMIN`), Spring Security `@PreAuthorize("hasAuthority('ROLE_SITE_ADMIN')")`
- 통계 API: Space 수, 활성 사용자 수, 콘텐츠 수, 스토리지 사용량(MinIO) 집계
- 메일서버 연동 상태 포함 (MailAccount sync_status 집계)
- 프론트엔드: `AdminDashboardView.vue` stub → DevExtreme Chart + DataGrid 구현

**In-app 알림**
- `notifications` 테이블 기반 (V7 스키마 이미 존재)
- 알림 발생 시점: 댓글 생성, 콘텐츠 게시(관련 Space 구독자), 멘션(@userId), 메일 수신
- 알림 조회: `GET /api/notifications` (페이징, is_read 필터)
- 읽음 처리: `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`
- 폴링 방식 (30초 간격) — SSE는 Claude 재량
- 프론트엔드: AppHeader에 알림 벨 아이콘 + 드롭다운 패널

**감사로그**
- AOP (`@Aspect`) 또는 서비스 메서드에서 직접 `AuditLogService.record()` 호출
- 기록 대상: 권한 변경(PERMISSION_CHANGE), 콘텐츠 삭제(CONTENT_DELETE), 관리자 접근(ADMIN_ACCESS), Space 삭제(SPACE_DELETE), 메일계정 등록/삭제(MAIL_ACCOUNT_CREATE/DELETE)
- `is_admin_access=true` 는 Site Admin이 수행한 모든 행위에 설정
- 조회 API: `GET /api/admin/audit-logs` (기간·actor·action_type 필터, 페이징)
- 프론트엔드: 관리자 대시보드 내 탭 또는 별도 AuditLogView

### Claude's Discretion
- 알림 생성 시 AOP vs 직접 호출 방식 선택
- 알림 드롭다운 UI 세부 레이아웃 (DevExtreme Popover 또는 커스텀)
- 관리자 대시보드 탭 구조 (대시보드 / 감사로그 / 사용자관리 등)
- 스토리지 사용량 집계 방법 (MinIO Admin API vs attachments 테이블 합산)

### Deferred Ideas (OUT OF SCOPE)
- WebSocket(SockJS/STOMP) 기반 실시간 Push 알림 — Phase 5 이후 고려
- 메일 발송(SMTP) 알림 — 1차 범위 외
- Guest 계정 관리 — Open Question (Phase 5 이후)
- Elasticsearch 검색엔진 연동 — 이미 Out of Scope (PRD 명시)
- 첨부파일 바이러스 스캔 — PRD 선택사항, 이 Phase에서 제외
- i18n 다국어 — Phase 5 QA 단계에서 검토

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ADMIN-01 | 관리자 대시보드 — Site Admin 전용 운영 현황 뷰 (사용자/Space 통계, 스토리지 사용량, 메일서버 상태) | SecurityConfig의 `/admin/**` ROLE_SITE_ADMIN 제한 이미 존재; AdminStatsDto + AdminController 패턴 확인 |
| ADMIN-02 | 통계 API — Space 수, 활성 사용자 수, 콘텐츠 수, 스토리지 사용량(attachments SUM), 메일 상태 | 기존 Repository에 `countBy*` / `@Query` JPQL 추가로 구현; attachments.size_bytes SUM으로 스토리지 집계 가능 |
| NOTIF-01 | 알림 적재 — 댓글/콘텐츠 게시/멘션 이벤트 발생 시 notifications 테이블에 INSERT | Notification 엔티티 + NotificationRepository + NotificationService.create() 패턴; 직접 호출 방식 권장 |
| NOTIF-02 | 알림 조회/읽음처리 — GET /api/notifications, GET /api/notifications/unread-count, PATCH read/read-all | 기존 컨트롤러 패턴(SpaceController) 그대로 적용; 페이징은 Pageable 파라미터 사용 |
| AUDIT-01 | 감사로그 기록 — 권한변경/콘텐츠삭제/관리자접근/Space삭제/메일계정 등록·삭제 이벤트 기록 | spring-boot-starter-aop 이미 pom.xml 존재; AuditLogService.record() 직접 호출 방식 권장 |
| AUDIT-02 | 감사로그 조회 — GET /api/admin/audit-logs (기간·actor·action_type 필터, 페이징) | JpaSpecificationExecutor 또는 @Query + Pageable 패턴; JSONB detail 컬럼 검색은 네이티브 쿼리 필요 |

</phase_requirements>

---

## Summary

Phase 4는 순수 서비스/API/프론트엔드 구현 Phase다. DB 스키마(V7)는 이미 마이그레이션 완료되어 있으므로 새 Flyway 스크립트는 불필요하다. 세 도메인(대시보드 통계, In-app 알림, 감사로그)은 각각 독립적이나 공통 패턴을 공유한다 — 백엔드는 `@Service + JpaRepository + @RestController + ApiResponse<T>` 패턴, 프론트엔드는 Composition API + Pinia 스토어 + 기본 export API 모듈 패턴이다.

핵심 판단 사항은 두 가지다. (1) 감사로그·알림 적재는 AOP보다 **서비스 메서드 직접 호출** 방식이 낫다 — 기존 코드베이스에 AOP 인프라가 있으나(pom.xml에 `spring-boot-starter-aop`) 도입 시 JoinPoint 파악 복잡도가 증가하고 트랜잭션 동기화 문제가 생긴다. (2) 스토리지 사용량은 **attachments 테이블 SUM** 방식으로 구현한다 — MinIO Admin API는 별도 자격증명과 SDK 호출이 필요하나 attachments 테이블에는 이미 `size_bytes` 컬럼이 있어 단순 `SELECT SUM(size_bytes) FROM attachments` 쿼리로 충분하다.

SecurityConfig에 `/admin/**` 경로가 `ROLE_SITE_ADMIN`으로 이미 보호되어 있어 별도 설정 변경 없이 `/admin/stats`, `/admin/audit-logs` 엔드포인트가 자동으로 Site Admin 전용이 된다. 프론트엔드에서도 `/admin` 라우트가 이미 `AdminDashboardView.vue` stub으로 등록되어 있으며 라우터 가드에 `ROLE_SITE_ADMIN` 체크만 추가하면 된다.

**Primary recommendation:** 알림·감사로그 적재는 직접 호출(`notificationService.create()` / `auditLogService.record()`)로 구현하고, 스토리지 집계는 attachments SUM을 사용하며, DevExtreme `DxTabPanel` + `DxChart` + `DxDataGrid` 조합으로 관리자 UI를 구성한다.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| 통계 집계 (Space/User/Content COUNT) | API / Backend | Database | COUNT 쿼리는 DB에서 직접 집계, API가 DTO로 조립 |
| 스토리지 사용량 집계 | API / Backend | Database | attachments.size_bytes SUM — DB 집계 후 API 노출 |
| 메일서버 상태 집계 | API / Backend | Database | mail_accounts.sync_status COUNT — DB 직접 집계 |
| 알림 생성 | API / Backend | — | 댓글/콘텐츠 서비스에서 NotificationService 직접 호출 |
| 알림 조회/읽음처리 | API / Backend | Database | notifications 테이블 읽기/업데이트 |
| 알림 폴링 | Browser / Client | — | setInterval 30초 간격, unread-count 배지 업데이트 |
| 감사로그 기록 | API / Backend | — | 서비스 레이어에서 AuditLogService.record() 직접 호출 |
| 감사로그 조회 | API / Backend | Database | 기간/actor/action_type 필터 + 페이징 |
| 관리자 대시보드 UI | Browser / Client | — | AdminDashboardView.vue — DxChart + DxDataGrid |
| Site Admin 접근 제어 | API / Backend | Browser / Client | SecurityConfig `/admin/**` + 라우터 가드 |

---

## Standard Stack

### Core (기존 스택 — 변경 없음)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.3.1 | REST API 프레임워크 | 프로젝트 확정 스택 [VERIFIED: pom.xml] |
| Spring Data JPA | 3.3.1 (managed) | Repository + 쿼리 | 기존 패턴 일관성 [VERIFIED: pom.xml] |
| Spring Security | 6.x (managed) | 인증/권한 | SecurityConfig 이미 `/admin/**` 보호 [VERIFIED: SecurityConfig.java] |
| spring-boot-starter-aop | 3.3.1 (managed) | AOP 인프라 | pom.xml에 이미 존재 [VERIFIED: pom.xml] |
| Vue 3 | ^3.5.39 | 프론트엔드 프레임워크 | 프로젝트 확정 스택 [VERIFIED: package.json] |
| DevExtreme Vue | ^26.1.3 | UI 컴포넌트 | 이미 설치됨 [VERIFIED: package.json] |
| Pinia | ^3.0.4 | 상태 관리 | 기존 스토어 패턴 일관성 [VERIFIED: package.json] |
| Axios | ^1.18.1 | HTTP 클라이언트 | 기존 api/axios.js 인스턴스 재사용 [VERIFIED: package.json] |

### 새로 추가 불필요

이 Phase는 기존 의존성만으로 구현 가능하다. 신규 패키지 설치 없음.

---

## Package Legitimacy Audit

> 이 Phase는 신규 외부 패키지를 설치하지 않는다. 기존 설치된 패키지만 사용.

**신규 설치 패키지 없음** — 감사 항목 해당 없음.

---

## Architecture Patterns

### System Architecture Diagram

```
[Browser]
  │  30초 폴링
  │  GET /api/notifications/unread-count ─────────────────────┐
  │  GET /api/admin/stats ──────────────────────────────────┐  │
  │  GET /api/admin/audit-logs ─────────────────────────┐   │  │
  │                                                      │   │  │
[Spring Boot API]                                        │   │  │
  ├── /admin/** ──→ SecurityConfig hasRole(SITE_ADMIN)   │   │  │
  │    ├── AdminController                               │   │  │
  │    │    ├── GET /admin/stats ──→ AdminStatsService ──┘   │  │
  │    │    └── GET /admin/audit-logs → AuditLogService ─────┘  │
  │    └── (라우팅: /admin/** 자동 보호)                         │
  ├── /notifications/** ──→ NotificationController             │
  │    ├── GET /notifications ──────────────────────────────────┘
  │    ├── GET /notifications/unread-count
  │    ├── PATCH /notifications/{id}/read
  │    └── PATCH /notifications/read-all
  │
  ├── [기존 서비스에서 직접 호출]
  │    ├── CommentService.createComment() ──→ NotificationService.create()
  │    ├── ContentService.publish() ───────→ NotificationService.create()
  │    ├── PermissionService.grant*() ─────→ AuditLogService.record(PERMISSION_CHANGE)
  │    ├── SpaceService.delete() ──────────→ AuditLogService.record(SPACE_DELETE)
  │    └── MailAccountService.create/delete → AuditLogService.record(MAIL_ACCOUNT_*)
  │
[PostgreSQL]
  ├── notifications 테이블 (V7 완료)
  ├── audit_logs 테이블 (V7 완료)
  ├── users COUNT/status 집계
  ├── spaces COUNT/deletedAt 필터
  ├── contents COUNT/deletedAt 필터
  ├── attachments SUM(size_bytes) — 스토리지 집계
  └── mail_accounts sync_status 집계
```

### Recommended Project Structure

```
backend/src/main/java/com/company/wiki/
├── admin/
│   ├── controller/AdminController.java    # GET /admin/stats, /admin/audit-logs
│   ├── dto/AdminStatsDto.java             # 통계 응답 DTO (record 또는 @Builder)
│   └── service/AdminStatsService.java     # 집계 쿼리 조합
├── notification/
│   ├── entity/Notification.java          # notifications 테이블 매핑
│   ├── repository/NotificationRepository.java
│   ├── dto/NotificationDto.java
│   ├── service/NotificationService.java   # create(), getByUser(), markRead(), markAllRead()
│   └── controller/NotificationController.java
└── auditlog/
    ├── entity/AuditLog.java               # audit_logs 테이블 매핑
    ├── repository/AuditLogRepository.java
    ├── dto/AuditLogDto.java
    ├── service/AuditLogService.java        # record(), findByFilter()
    └── (controller는 AdminController에서 위임)

frontend/src/
├── api/
│   ├── admin.js       # adminApi — GET /admin/stats, /admin/audit-logs
│   └── notification.js # notificationApi — GET/PATCH /notifications
├── stores/
│   └── notification.js # Pinia 스토어 — unreadCount, notifications[]
└── views/admin/
    └── AdminDashboardView.vue   # DxTabPanel(대시보드/감사로그) + DxChart + DxDataGrid
```

### Pattern 1: 감사로그 엔티티 및 서비스

**What:** V7 `audit_logs` 테이블을 JPA Entity로 매핑하고 `AuditLogService.record()` 단일 진입점 제공

**When to use:** 기존 서비스(PermissionService, SpaceService, MailAccountService 등)에서 주요 행위 직후 호출

```java
// AuditLog 엔티티
@Entity
@Table(name = "audit_logs")
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;   // "PERMISSION_CHANGE", "CONTENT_DELETE", ...

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;   // "CONTENT", "SPACE", "PERMISSION", "MAIL_ACCOUNT"

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "jsonb")
    private String detail;       // JSON 문자열 (ObjectMapper.writeValueAsString)

    @Column(name = "is_admin_access", nullable = false)
    private boolean isAdminAccess;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// AuditLogService.record() — 기존 서비스에서 직접 호출
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(Long actorId, String actionType, String targetType,
                       Long targetId, Object detail, boolean isAdminAccess) {
        try {
            String detailJson = detail != null ? objectMapper.writeValueAsString(detail) : null;
            auditLogRepository.save(AuditLog.builder()
                .actorId(actorId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detailJson)
                .isAdminAccess(isAdminAccess)
                .build());
        } catch (Exception e) {
            // 감사로그 실패가 본 트랜잭션을 중단시키면 안 됨 — 로그만 남기고 진행
            log.error("AuditLog record failed: {}", e.getMessage());
        }
    }
}
```

**중요 pitfall:** `AuditLogService.record()`는 반드시 **try-catch로 예외를 삼켜야 한다**. 감사로그 저장 실패가 본 비즈니스 트랜잭션(Space 삭제 등)을 롤백시키면 안 된다.

### Pattern 2: 알림 엔티티 및 서비스

**What:** V7 `notifications` 테이블을 JPA Entity로 매핑하고 `NotificationService`를 기존 서비스에서 직접 호출

```java
// Notification 엔티티
@Entity
@Table(name = "notifications")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String type;         // "COMMENT", "CONTENT_PUBLISHED", "MENTION", "MAIL_RECEIVED"

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// NotificationService — 핵심 메서드
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void create(Long userId, String type, String title, String message, String linkUrl) {
        notificationRepository.save(Notification.builder()
            .userId(userId).type(type).title(title)
            .message(message).linkUrl(linkUrl).isRead(false).build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto.Response> getNotifications(Long userId, Boolean isRead, Pageable pageable) {
        // isRead null → 전체, true/false → 필터
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        n.setRead(true);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
```

### Pattern 3: 통계 API (AdminStatsService)

**What:** 여러 Repository에서 COUNT/SUM 쿼리를 호출하여 하나의 DTO로 조립

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final ContentRepository contentRepository;
    private final AttachmentRepository attachmentRepository;
    private final MailAccountRepository mailAccountRepository;

    public AdminStatsDto getStats() {
        long activeUsers = userRepository.countByStatus("ACTIVE");
        long totalSpaces = spaceRepository.countByDeletedAtIsNull();
        long totalContents = contentRepository.countByDeletedAtIsNull();
        long storageBytesUsed = attachmentRepository.sumSizeBytes();  // @Query JPQL
        long mailAccountsOk = mailAccountRepository.countBySyncStatus("ACTIVE");
        long mailAccountsFailed = mailAccountRepository.countBySyncStatus("DISABLED");

        return AdminStatsDto.builder()
            .activeUsers(activeUsers)
            .totalSpaces(totalSpaces)
            .totalContents(totalContents)
            .storageUsedBytes(storageBytesUsed)
            .mailAccountsOk(mailAccountsOk)
            .mailAccountsFailed(mailAccountsFailed)
            .build();
    }
}

// AttachmentRepository에 SUM 쿼리 추가
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByContentId(Long contentId);

    @Query("SELECT COALESCE(SUM(a.sizeBytes), 0) FROM Attachment a")
    long sumSizeBytes();
}
```

### Pattern 4: 감사로그 필터 조회

**What:** `created_at` 기간, `actor_id`, `action_type` 필터를 동적으로 조합한 페이징 쿼리

```java
// AuditLogRepository — @Query + Pageable
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:actorId IS NULL OR a.actorId = :actorId)
          AND (:actionType IS NULL OR a.actionType = :actionType)
          AND (:from IS NULL OR a.createdAt >= :from)
          AND (:to IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findByFilter(
        @Param("actorId") Long actorId,
        @Param("actionType") String actionType,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
}
```

### Pattern 5: AdminController — /admin/** 라우팅

**What:** SecurityConfig에 의해 자동으로 Site Admin 전용이 되는 `/admin/**` 컨트롤러

```java
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminStatsService adminStatsService;
    private final AuditLogService auditLogService;

    @GetMapping("/stats")
    public ApiResponse<AdminStatsDto> getStats() {
        return ApiResponse.ok(adminStatsService.getStats());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditLogDto.Response>> getAuditLogs(
        @RequestParam(required = false) Long actorId,
        @RequestParam(required = false) String actionType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        Pageable pageable
    ) {
        return ApiResponse.ok(auditLogService.findByFilter(actorId, actionType, from, to, pageable));
    }
}
```

**중요:** `/admin/**`은 `SecurityConfig`에서 이미 `hasRole("SITE_ADMIN")`으로 보호되어 있다. `@PreAuthorize` 추가 없이도 동작한다. [VERIFIED: SecurityConfig.java]

### Pattern 6: 알림 폴링 — 프론트엔드 Pinia 스토어

**What:** 30초 간격 폴링으로 unread count 업데이트, 드롭다운 패널에서 알림 목록 로드

```javascript
// stores/notification.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  let pollTimer = null

  async function fetchUnreadCount() {
    const { data } = await notificationApi.getUnreadCount()
    unreadCount.value = data.data
  }

  async function fetchNotifications(isRead = null) {
    const { data } = await notificationApi.getNotifications({ isRead, page: 0, size: 20 })
    notifications.value = data.data.content
  }

  async function markRead(id) {
    await notificationApi.markRead(id)
    await fetchUnreadCount()
  }

  async function markAllRead() {
    await notificationApi.markAllRead()
    unreadCount.value = 0
    notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
  }

  function startPolling() {
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30000)
  }

  function stopPolling() {
    if (pollTimer) clearInterval(pollTimer)
  }

  return { unreadCount, notifications, fetchUnreadCount, fetchNotifications,
           markRead, markAllRead, startPolling, stopPolling }
})
```

### Pattern 7: AdminDashboardView.vue — DevExtreme 탭 구조

**What:** `DxTabPanel`로 대시보드/감사로그 탭 전환, `DxPieChart`로 통계 시각화

```vue
<!-- AdminDashboardView.vue -->
<template>
  <div class="admin-dashboard">
    <AppHeader />
    <div class="admin-body">
      <h1>관리자 대시보드</h1>
      <DxTabPanel>
        <DxItem title="대시보드">
          <!-- 통계 카드 + DxPieChart -->
          <div class="stats-grid">
            <StatCard label="활성 사용자" :value="stats.activeUsers" />
            <StatCard label="Space 수" :value="stats.totalSpaces" />
            <StatCard label="콘텐츠 수" :value="stats.totalContents" />
            <StatCard label="스토리지 사용량" :value="formatBytes(stats.storageUsedBytes)" />
          </div>
          <DxPieChart :data-source="mailStatusData" title="메일 서버 상태">
            <DxSeries argument-field="status" value-field="count" />
          </DxPieChart>
        </DxItem>
        <DxItem title="감사로그">
          <!-- 필터 폼 + DxDataGrid -->
          <DxDataGrid :data-source="auditLogs" ...>
            <DxColumn data-field="actorId" caption="수행자" />
            <DxColumn data-field="actionType" caption="액션" />
            <DxColumn data-field="targetType" caption="대상 타입" />
            <DxColumn data-field="createdAt" caption="시각" data-type="datetime" />
          </DxDataGrid>
        </DxItem>
      </DxTabPanel>
    </div>
  </div>
</template>
```

### Pattern 8: 기존 서비스에서 알림/감사로그 연동

**What:** CommentService, ContentService, PermissionService 등에서 NotificationService·AuditLogService 직접 호출

```java
// CommentService.createComment() 수정 예시
@Transactional
public CommentDto.CommentNode createComment(Long contentId, CommentDto.CreateRequest req,
                                           Long userId, String role) {
    // ... 기존 로직 ...
    Comment comment = commentRepository.save(...);

    // 알림 생성 — 콘텐츠 작성자에게 댓글 알림
    try {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId).orElseThrow();
        Long contentAuthorId = content.getCreatedBy().getId();
        if (!contentAuthorId.equals(userId)) {  // 자기 글에 자기 댓글은 알림 제외
            notificationService.create(
                contentAuthorId,
                "COMMENT",
                "새 댓글이 달렸습니다",
                content.getTitle() + "에 댓글이 달렸습니다.",
                "/spaces/" + content.getSpaceId() + "/contents/" + contentId
            );
        }
    } catch (Exception e) {
        log.warn("알림 생성 실패 (비중단): {}", e.getMessage());
    }
    return buildCommentNode(comment, new HashMap<>());
}
```

### Anti-Patterns to Avoid

- **AOP @Around 로 알림/감사로그 weaving:** 기존 코드베이스에서 JoinPoint 메서드 시그니처와 파라미터 추출 복잡도가 높고, 트랜잭션 바운더리와 충돌 가능. 직접 호출 방식이 명확성·디버그 용이성·유지보수성 모두 우월하다. [ASSUMED — 직접 호출 선호 이유는 코드베이스 분석 기반]
- **감사로그 실패 시 예외 전파:** `AuditLogService.record()`에서 예외가 발생하면 호출자의 트랜잭션도 롤백된다. try-catch로 예외를 삼키고 로그만 남겨야 한다.
- **알림 폴링 중복 등록:** `startPolling()`을 여러 컴포넌트에서 중복 호출하면 인터벌이 쌓인다. Pinia 스토어의 `$onAction` 또는 `App.vue`의 `onMounted` 단일 지점에서만 호출해야 한다.
- **notifications 테이블 무제한 성장:** 현재 Phase에서는 스코프 아웃이지만, 오래된 알림 청소 정책(e.g., 90일 지나면 삭제)을 나중에 고려해야 한다.
- **라우터 가드 Site Admin 체크 누락:** `/admin` 라우트에 `meta: { requiresAdmin: true }` 설정을 빠뜨리면 프론트엔드에서 일반 사용자가 URL 직접 입력으로 접근 가능. 백엔드가 차단하더라도 UX가 나빠진다.
- **Page<T>를 ApiResponse<Page<T>>로 래핑:** Jackson 직렬화 시 `Page` 메타 필드(`totalPages`, `totalElements` 등)가 정상 직렬화되려면 `PageImpl` 기본 Jackson 설정이 필요. 필요 시 `PagedResponse<T>` 커스텀 DTO로 감싸는 것이 안전하다.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 통계 집계 쿼리 | 별도 View/Materialized View | Spring Data JPA `@Query` JPQL | 단순 COUNT/SUM으로 충분; 뷰 생성은 Flyway 스크립트 필요 |
| 스토리지 사용량 | MinIO Admin API 연동 | `@Query("SELECT COALESCE(SUM(a.sizeBytes),0) FROM Attachment a")` | attachments.size_bytes 이미 존재; MinIO Admin SDK 추가 불필요 |
| 알림 폴링 | Long-polling/WebSocket | `setInterval(30000)` + REST | 1차 범위는 폴링으로 충분; WebSocket은 Deferred |
| 감사로그 필터 동적 쿼리 | QueryDSL/Criteria API | JPQL `@Query` + `@Param` nullable 조건 | 필터 3개(actorId, actionType, 기간)로 단순; JpaSpecificationExecutor 오버엔지니어링 |
| JSONB 직렬화 | 직접 JSON 문자열 조작 | Jackson `ObjectMapper.writeValueAsString(detail)` | 이미 ObjectMapper가 Spring Context에 등록되어 있음 |
| Site Admin 체크 로직 | 커스텀 인터셉터 | SecurityConfig `/admin/**` + router guard | 이미 `/admin/**`가 보호됨; 중복 구현 불필요 |

**Key insight:** 이 Phase는 통계 VIEW, 리얼타임 푸시, 외부 서비스 연동 없이 기존 테이블 집계 + 폴링으로 충분히 구현 가능하다.

---

## Common Pitfalls

### Pitfall 1: AuditLog 저장 실패로 비즈니스 트랜잭션 롤백

**What goes wrong:** `AuditLogService.record()` 내부에서 예외(e.g., JSON 직렬화 실패, DB constraint)가 발생하면 호출자(`SpaceService.delete()` 등)의 트랜잭션도 함께 롤백된다.

**Why it happens:** 같은 트랜잭션 내에서 예외가 던져지면 Spring `@Transactional`이 전체 롤백 처리한다.

**How to avoid:** `AuditLogService.record()`에서 try-catch로 예외를 삼키고 `log.error()`만 남긴다. 또는 `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 사용하여 독립 트랜잭션으로 분리한다 (단, 이 경우 DB 연결 수 증가 주의).

**Warning signs:** 콘텐츠 삭제 API 테스트 시 `500 Internal Server Error` 또는 삭제가 되지 않는 현상.

### Pitfall 2: 알림 폴링 인터벌 중복 등록

**What goes wrong:** `startPolling()`이 여러 컴포넌트(AppHeader, App.vue 등)에서 중복 호출되어 30초마다 N번씩 요청이 날아간다.

**Why it happens:** Vue 컴포넌트 `onMounted`에서 각각 `startPolling()` 호출 시 `setInterval` 핸들러가 누적된다.

**How to avoid:** `App.vue` 단일 `onMounted`에서만 `startPolling()` 호출, 로그인 후 `onMounted`에서 한 번만 실행. `stopPolling()`을 `onUnmounted`에 등록하거나 로그아웃 시 명시적으로 호출.

**Warning signs:** 브라우저 네트워크 탭에서 `/api/notifications/unread-count` 요청이 30초마다 1번이 아닌 여러 번 발생.

### Pitfall 3: Page<T> Jackson 직렬화 오류

**What goes wrong:** `ApiResponse<Page<AuditLogDto.Response>>`를 반환할 때 JSON에서 `content` 배열은 있지만 `totalPages`, `totalElements` 등 페이징 메타 정보가 빠지거나, Jackson이 `PageImpl` 역직렬화에 실패한다.

**Why it happens:** Spring Data `Page`는 인터페이스이고 `PageImpl`은 Jackson 기본 설정으로 역직렬화가 안 된다.

**How to avoid:** 컨트롤러에서 `Page<T>`를 직접 반환하지 말고, 커스텀 `PagedResponse<T>` DTO로 변환하여 반환한다:
```java
record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }
}
```

**Warning signs:** 프론트엔드 `data.data.totalPages`가 undefined.

### Pitfall 4: JPQL에서 JSONB 컬럼 필터링 불가

**What goes wrong:** `audit_logs.detail` JSONB 컬럼을 JPQL에서 `a.detail LIKE :keyword`로 검색하려 하면 PostgreSQL 타입 오류 발생.

**Why it happens:** JPQL은 JSONB 연산자(`@>`, `->`, `?`)를 지원하지 않는다.

**How to avoid:** `detail` 컬럼에 대한 검색이 필요한 경우 `@Query(nativeQuery = true)`로 네이티브 SQL 사용. 이 Phase에서는 detail 컬럼 필터를 조회 스펙에서 제외하는 것이 단순하다.

**Warning signs:** `org.hibernate.exception.SQLGrammarException` 발생.

### Pitfall 5: Site Admin 라우터 가드 누락

**What goes wrong:** 백엔드 API는 `ROLE_SITE_ADMIN`으로 보호되지만, 프론트엔드 라우터에 Admin 체크가 없어 일반 사용자가 `/admin`에 접근하면 빈 화면 또는 403 에러 페이지가 표시된다.

**Why it happens:** 현재 `router/index.js`의 `/admin` 라우트에 `meta: { requiresAdmin: true }` 설정이 없다. `router.beforeEach` 가드도 `requiresAuth`만 체크한다.

**How to avoid:** `/admin` 라우트에 `meta: { requiresAdmin: true }` 추가 + `router.beforeEach`에서 `useAuthStore().user?.role !== 'SITE_ADMIN'`이면 `/spaces`로 리다이렉트.

### Pitfall 6: 알림 생성에서 N+1 쿼리

**What goes wrong:** 콘텐츠 게시 시 구독자 전체에 알림 생성을 하려면 구독자 목록 조회 → 각 구독자에 INSERT로 N번의 INSERT가 발생한다.

**Why it happens:** 개별 save() 반복 호출.

**How to avoid:** 구독자 수가 많을 경우 `notificationRepository.saveAll(List<Notification>)` 사용. 이 Phase에서는 멘션(@userId) 1:1 알림 및 댓글 알림(콘텐츠 작성자 1명)이 주 케이스이므로 N+1 문제가 실질적 영향은 적다.

---

## Code Examples

### 기존 서비스 주입 패턴 (참조)

```java
// Source: CommentService.java (검증된 패턴)
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final ContentRepository contentRepository;
    private final PermissionService permissionService;
    private final GroupMemberRepository groupMemberRepository;

    private Long getCurrentUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());  // JWT subject = userId
    }
}
```

### 컨트롤러 패턴 (참조)

```java
// Source: SpaceController.java (검증된 패턴)
@RestController
@RequestMapping("/spaces")
@RequiredArgsConstructor
public class SpaceController {
    private Long getCurrentUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }
    // ApiResponse<T> 반환, @AuthenticationPrincipal UserDetails 주입
}
```

### DevExtreme Vue 컴포넌트 import 패턴 (검증됨)

```javascript
// Source: MailBoxView.vue (검증된 패턴)
import {
  DxDataGrid, DxColumn, DxPaging, DxSelection,
} from 'devextreme-vue/data-grid'
import { DxSelectBox } from 'devextreme-vue/select-box'
import { DxTabPanel, DxItem } from 'devextreme-vue/tab-panel'
import { DxPieChart, DxSeries } from 'devextreme-vue/pie-chart'
```

### 테스트 패턴 (참조)

```java
// Source: SearchControllerTest.java (검증된 패턴)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class NotificationControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();  // POST /auth/login (admin/Admin1234!)
    }
    // 실제 PostgreSQL 사용 (H2 금지)
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spring AOP Weaving 방식 감사로그 | 서비스 직접 호출 | — | 트랜잭션 제어 명확, 디버그 용이 |
| WebSocket 실시간 알림 | 폴링 (1차) | Deferred | Phase 1 구현 단순화 |
| MinIO Admin API 스토리지 집계 | attachments SUM | — | 외부 API 불필요, 즉시 구현 가능 |

**Deprecated/outdated:**
- WebSocket 알림: Deferred (Phase 5 이후)
- JpaSpecificationExecutor 동적 쿼리: 필터 3개 수준에서는 오버엔지니어링, JPQL @Query로 충분

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | AOP보다 직접 호출 방식이 유지보수성 우월 | Architecture Patterns (Anti-Pattern) | AOP 방식 채택 시 리팩토링 필요 |
| A2 | attachments SUM으로 스토리지 집계 충분 (MinIO 실제 사용량과 근사치) | Don't Hand-Roll | MinIO에 직접 업로드된 파일(attachments 테이블 미기록)이 있을 경우 과소 집계 |
| A3 | 콘텐츠 게시 구독자 알림의 대상은 Space 멤버가 아닌 콘텐츠 작성자 1명 (댓글 알림 케이스) | Pattern 8 | 구독자 개념이 추가되면 N:1 알림 생성 로직 필요 |

---

## Open Questions

1. **스토리지 집계 정확도**
   - What we know: attachments 테이블에 `size_bytes` 컬럼이 있고, MinIO에도 동일 파일이 저장됨
   - What's unclear: MinIO에서 직접 삭제된 파일이 있을 경우 불일치 발생 가능
   - Recommendation: Phase 4에서는 attachments SUM으로 구현, 정확도 요구 시 Phase 5에서 MinIO Admin API 연동 검토

2. **알림 대상 (NOTIF-01) — 콘텐츠 게시 구독자 정의**
   - What we know: `notifications` 테이블에 `user_id` 1:1 구조
   - What's unclear: "Space 구독자" 개념이 DB 스키마에 없음 (space_favorites는 즐겨찾기이지 구독이 아님)
   - Recommendation: Phase 4에서는 댓글 알림(콘텐츠 작성자)과 멘션(@userId)만 구현. Space 구독자 개념은 별도 스키마 없이는 구현 불가 → 범위를 댓글/멘션으로 제한

3. **`audit_logs.detail` JSONB 컬럼의 표준 구조**
   - What we know: JSONB 타입으로 유연하게 저장 가능
   - What's unclear: 각 action_type별로 어떤 필드를 detail에 넣을지 미정
   - Recommendation: action_type별 최소 공통 구조 정의: `{"before": {}, "after": {}, "ip": "...", "note": "..."}`

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 (Homebrew) | 백엔드 빌드/테스트 | ✓ | 21.0.11 | `/opt/homebrew/opt/openjdk@21/bin/java` |
| PostgreSQL 16 (Docker) | 테스트, DB 집계 | ✓ | 16-alpine | — |
| MinIO (Docker) | 스토리지 (Phase 3 이후) | ✓ | latest | — |
| Docker | 테스트 인프라 | ✓ | 29.2.0 | — |
| Node 22 | 프론트엔드 빌드 | ✓ | 22.3.0 | — |
| DevExtreme Vue | 관리자 UI | ✓ | 26.1.3 (설치됨) | — |

**Missing dependencies with no fallback:** 없음

**Missing dependencies with fallback:** 없음

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test (MockMvc) |
| Config file | `src/test/resources/application.yml` (없으면 Wave 0 생성) |
| Quick run command | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw test -pl backend -Dtest=NotificationControllerTest,AuditLogControllerTest,AdminControllerTest` |
| Full suite command | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw test -pl backend` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ADMIN-01 | GET /admin/stats — SITE_ADMIN 접근 성공 | integration | `mvnw test -Dtest=AdminControllerTest#stats_siteAdmin_returns200` | ❌ Wave 0 |
| ADMIN-01 | GET /admin/stats — MEMBER 접근 거부 (403) | integration | `mvnw test -Dtest=AdminControllerTest#stats_member_returns403` | ❌ Wave 0 |
| ADMIN-02 | GET /admin/stats — 응답에 activeUsers/totalSpaces/storageUsedBytes 포함 | integration | `mvnw test -Dtest=AdminControllerTest#stats_response_hasAllFields` | ❌ Wave 0 |
| NOTIF-01 | 댓글 생성 후 notifications 테이블에 레코드 존재 | integration | `mvnw test -Dtest=NotificationServiceTest#createComment_createsNotification` | ❌ Wave 0 |
| NOTIF-02 | GET /api/notifications — 인증 사용자의 알림 목록 반환 | integration | `mvnw test -Dtest=NotificationControllerTest#getNotifications_returns200` | ❌ Wave 0 |
| NOTIF-02 | GET /api/notifications/unread-count — 미읽음 수 반환 | integration | `mvnw test -Dtest=NotificationControllerTest#unreadCount_returns200` | ❌ Wave 0 |
| NOTIF-02 | PATCH /api/notifications/{id}/read — 읽음 처리 성공 | integration | `mvnw test -Dtest=NotificationControllerTest#markRead_returns200` | ❌ Wave 0 |
| AUDIT-01 | Space 삭제 후 audit_logs 테이블에 SPACE_DELETE 레코드 존재 | integration | `mvnw test -Dtest=AuditLogServiceTest#spaceDelete_createsAuditLog` | ❌ Wave 0 |
| AUDIT-02 | GET /api/admin/audit-logs — SITE_ADMIN 접근 성공, 페이징 응답 | integration | `mvnw test -Dtest=AdminControllerTest#auditLogs_returns200` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `mvnw test -Dtest={해당 도메인 테스트 클래스} -pl backend`
- **Per wave merge:** `mvnw test -pl backend` (전체)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `backend/src/test/java/com/company/wiki/admin/controller/AdminControllerTest.java`
- [ ] `backend/src/test/java/com/company/wiki/notification/controller/NotificationControllerTest.java`
- [ ] `backend/src/test/java/com/company/wiki/notification/service/NotificationServiceTest.java`
- [ ] `backend/src/test/java/com/company/wiki/auditlog/service/AuditLogServiceTest.java`

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT (기존 JwtAuthenticationFilter — 변경 없음) |
| V3 Session Management | no | Stateless JWT — 세션 없음 |
| V4 Access Control | yes | SecurityConfig `/admin/**` hasRole(SITE_ADMIN) + router guard |
| V5 Input Validation | yes | `@DateTimeFormat` for LocalDateTime params, `@RequestParam` validation |
| V6 Cryptography | no | 새 암호화 로직 없음 |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR (다른 사용자 알림 읽음 처리) | Tampering | NotificationService에서 `findByIdAndUserId` — userId 일치 검증 필수 |
| Admin API 미인가 접근 | Elevation of Privilege | SecurityConfig `/admin/**` hasRole(SITE_ADMIN) — 이미 적용됨 |
| audit_logs 위변조 | Tampering | audit_logs에 UPDATE/DELETE 허용 금지 — AuditLogRepository에 delete 메서드 노출 금지 |
| JSONB Injection (detail 컬럼) | Tampering | ObjectMapper.writeValueAsString()으로 직렬화 — 직접 SQL 문자열 조합 금지 |

---

## Sources

### Primary (HIGH confidence)

- [VERIFIED: SecurityConfig.java] — `/admin/**` hasRole("SITE_ADMIN") 이미 적용, `@EnableMethodSecurity` 활성화 확인
- [VERIFIED: V7__audit_notifications.sql] — audit_logs, notifications 스키마 확인
- [VERIFIED: pom.xml] — spring-boot-starter-aop 이미 존재, Spring Boot 3.3.1
- [VERIFIED: package.json] — devextreme 26.1.3, devextreme-vue 26.1.3, Vue 3.5.39, Pinia 3.0.4
- [VERIFIED: MailBoxView.vue] — DxDataGrid, DxSelectBox, DxPaging, DxSelection import 패턴
- [VERIFIED: CommentService.java] — @Service + @Transactional + @RequiredArgsConstructor + BusinessException 패턴
- [VERIFIED: SpaceController.java] — @RestController + ApiResponse<T> + @AuthenticationPrincipal UserDetails 패턴
- [VERIFIED: SearchControllerTest.java] — @SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("local") + @Transactional 테스트 패턴
- [VERIFIED: npm registry] — devextreme-vue@26.1.3, devextreme@26.1.3 (최신 버전 일치)
- [VERIFIED: AttachmentRepository.java] — 기존 attachments 테이블에 size_bytes 컬럼 존재 확인

### Secondary (MEDIUM confidence)

- [ASSUMED] — AOP 직접 호출 vs @Aspect 트레이드오프 분석 (training knowledge)
- [ASSUMED] — PagedResponse<T> 패턴으로 Page<T> 직렬화 우회 (training knowledge, Spring Data JPA 일반 관행)

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — pom.xml, package.json 직접 확인
- Architecture: HIGH — SecurityConfig, 기존 서비스 패턴 코드베이스 검증
- Pitfalls: MEDIUM — 코드베이스 분석 기반 + training knowledge
- DevExtreme 컴포넌트: HIGH — MailBoxView.vue에서 실제 사용 패턴 확인

**Research date:** 2026-07-07
**Valid until:** 2026-08-07 (stable stack)
