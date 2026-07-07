package com.company.wiki.auditlog.dto;

import com.company.wiki.auditlog.entity.AuditLog;

import java.time.LocalDateTime;

public class AuditLogDto {

    public record Response(
            Long id,
            Long actorId,
            String actionType,
            String targetType,
            Long targetId,
            String detail,
            boolean isAdminAccess,
            LocalDateTime createdAt
    ) {
        public static Response from(AuditLog auditLog) {
            return new Response(
                    auditLog.getId(),
                    auditLog.getActorId(),
                    auditLog.getActionType(),
                    auditLog.getTargetType(),
                    auditLog.getTargetId(),
                    auditLog.getDetail(),
                    auditLog.isAdminAccess(),
                    auditLog.getCreatedAt()
            );
        }
    }
}
