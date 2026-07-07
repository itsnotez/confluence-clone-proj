package com.company.wiki.auditlog.repository;

import com.company.wiki.auditlog.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = "SELECT * FROM audit_logs a " +
           "WHERE (:actorId IS NULL OR a.actor_id = :actorId) " +
           "AND (:actionType IS NULL OR a.action_type = :actionType) " +
           "AND (CAST(:from AS timestamp) IS NULL OR a.created_at >= CAST(:from AS timestamp)) " +
           "AND (CAST(:to AS timestamp) IS NULL OR a.created_at <= CAST(:to AS timestamp)) " +
           "ORDER BY a.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM audit_logs a " +
           "WHERE (:actorId IS NULL OR a.actor_id = :actorId) " +
           "AND (:actionType IS NULL OR a.action_type = :actionType) " +
           "AND (CAST(:from AS timestamp) IS NULL OR a.created_at >= CAST(:from AS timestamp)) " +
           "AND (CAST(:to AS timestamp) IS NULL OR a.created_at <= CAST(:to AS timestamp))",
           nativeQuery = true)
    Page<AuditLog> findByFilter(
            @Param("actorId") Long actorId,
            @Param("actionType") String actionType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
