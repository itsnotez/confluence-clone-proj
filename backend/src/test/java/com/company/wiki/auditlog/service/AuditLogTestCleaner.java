package com.company.wiki.auditlog.service;

import com.company.wiki.auditlog.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuditLogServiceTest용 클린업 헬퍼.
 * AuditLogService.record()가 REQUIRES_NEW로 커밋하므로, 테스트 트랜잭션 롤백으로 정리되지 않는다.
 * 이 빈은 별도 트랜잭션(REQUIRES_NEW)에서 deleteAll을 실행하여 테스트 격리를 보장한다.
 */
@Component
@RequiredArgsConstructor
public class AuditLogTestCleaner {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAll() {
        auditLogRepository.deleteAll();
    }
}
