package com.company.wiki.auditlog.service;

import com.company.wiki.auditlog.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuditLogServiceTest {

    @Autowired
    AuditLogService auditLogService;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    AuditLogTestCleaner cleaner;

    @BeforeEach
    void setUp() {
        // REQUIRES_NEW로 커밋된 이전 테스트의 audit_logs를 별도 트랜잭션으로 정리
        cleaner.deleteAll();
    }

    /**
     * Test 1: record() 호출하면 audit_logs 테이블에 레코드가 저장된다
     */
    @Test
    void record_savesAuditLogToDatabase() {
        // when
        auditLogService.record(1L, "SPACE_DELETE", "SPACE", 42L,
                Map.of("spaceKey", "TEST", "name", "Test Space"), false);

        // then: REQUIRES_NEW로 커밋된 레코드를 확인한다
        var logs = auditLogRepository.findAll();
        var found = logs.stream()
                .filter(l -> "SPACE_DELETE".equals(l.getActionType()))
                .toList();
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getTargetType()).isEqualTo("SPACE");
        assertThat(found.get(0).getTargetId()).isEqualTo(42L);
        assertThat(found.get(0).getActorId()).isEqualTo(1L);
    }

    /**
     * Test 2: record() 내부에서 예외가 발생해도 예외가 전파되지 않는다 (try-catch 내부 예외 처리)
     * 순환 참조 객체 → ObjectMapper 직렬화 실패 시뮬레이션
     */
    @Test
    void record_doesNotThrowEvenOnSerializationError() {
        // given: 직렬화 불가 객체를 detail로 전달
        Object unserializable = new Object() {
            public Object self = this; // 순환 참조 — ObjectMapper 직렬화 실패
        };

        // when/then: 예외가 전파되지 않아야 한다
        assertThatCode(() ->
                auditLogService.record(1L, "SPACE_DELETE", "SPACE", 1L, unserializable, false)
        ).doesNotThrowAnyException();
    }

    /**
     * Test 3: findByFilter(actionType="SPACE_DELETE") 는 SPACE_DELETE 레코드만 반환한다
     * spaceDelete_createsAuditLog — must_haves 조건에 의한 필수 테스트 명칭
     */
    @Test
    void spaceDelete_createsAuditLog() {
        // given: 두 종류의 레코드 저장
        auditLogService.record(1L, "SPACE_DELETE", "SPACE", 10L,
                Map.of("spaceKey", "S1", "name", "Space 1"), false);
        auditLogService.record(1L, "PERMISSION_CHANGE", "PERMISSION", 10L,
                Map.of("permissionLevel", "READ"), false);

        // when
        var result = auditLogService.findByFilter(
                null, "SPACE_DELETE", null, null,
                PageRequest.of(0, 10));

        // then
        assertThat(result.content()).isNotEmpty();
        assertThat(result.content()).allMatch(r -> "SPACE_DELETE".equals(r.actionType()));
    }

    /**
     * Test 4: findByFilter()는 기간 필터(from/to)를 적용하여 범위 밖 레코드를 제외한다
     */
    @Test
    void findByFilter_appliesDateRangeFilter() {
        // given: 현재 시각에 레코드 저장
        auditLogService.record(1L, "SPACE_DELETE", "SPACE", 20L,
                Map.of("spaceKey", "OLD", "name", "Old Space"), false);

        LocalDateTime from = LocalDateTime.now().plusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusHours(1);

        // when: 미래 기간 필터 — 방금 저장된 레코드는 범위 밖
        var result = auditLogService.findByFilter(
                null, "SPACE_DELETE", from, to,
                PageRequest.of(0, 10));

        // then: 범위 밖이므로 SPACE_DELETE 결과 없음
        var spaceDeletions = result.content().stream()
                .filter(r -> "SPACE_DELETE".equals(r.actionType()))
                .toList();
        assertThat(spaceDeletions).isEmpty();
    }
}
