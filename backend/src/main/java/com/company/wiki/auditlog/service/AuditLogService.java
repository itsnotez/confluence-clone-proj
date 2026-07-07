package com.company.wiki.auditlog.service;

import com.company.wiki.auditlog.dto.AuditLogDto;
import com.company.wiki.auditlog.entity.AuditLog;
import com.company.wiki.auditlog.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    /**
     * 감사로그 기록 단일 진입점.
     * REQUIRES_NEW로 독립 트랜잭션에서 실행 — 내부 예외가 호출자 트랜잭션을 오염시키지 않는다.
     * catch 블록에서 예외를 삼켜 호출자에게 전파하지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String actionType, String targetType,
                       Long targetId, Object detail, boolean isAdminAccess) {
        try {
            String detailJson = detail != null ? objectMapper.writeValueAsString(detail) : null;
            AuditLog auditLog = AuditLog.builder()
                    .actorId(actorId)
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detailJson)
                    .isAdminAccess(isAdminAccess)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("AuditLog record failed: {}", e.getMessage());
        }
    }

    /**
     * 감사로그 필터 조회.
     * 모든 파라미터는 nullable (null이면 해당 조건 무시).
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto.Response> findByFilter(
            Long actorId, String actionType,
            LocalDateTime from, LocalDateTime to,
            Pageable pageable) {

        Page<AuditLog> page = auditLogRepository.findByFilter(actorId, actionType, from, to, pageable);
        List<AuditLogDto.Response> content = page.getContent()
                .stream()
                .map(AuditLogDto.Response::from)
                .toList();

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
