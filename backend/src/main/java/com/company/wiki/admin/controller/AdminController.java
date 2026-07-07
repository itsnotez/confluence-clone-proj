package com.company.wiki.admin.controller;

import com.company.wiki.admin.dto.AdminStatsDto;
import com.company.wiki.admin.service.AdminStatsService;
import com.company.wiki.auditlog.dto.AuditLogDto;
import com.company.wiki.auditlog.service.AuditLogService;
import com.company.wiki.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final AuditLogService auditLogService;

    private Long getCurrentUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStatsDto> getStats(@AuthenticationPrincipal UserDetails principal) {
        Long actorId = getCurrentUserId(principal);
        try {
            auditLogService.record(actorId, "ADMIN_ACCESS", "ADMIN", null,
                    Map.of("endpoint", "GET /admin/stats"), true);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }
        return ApiResponse.ok(adminStatsService.getStats());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<AuditLogService.PagedResponse<AuditLogDto.Response>> getAuditLogs(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        Long userId = getCurrentUserId(principal);
        try {
            auditLogService.record(userId, "ADMIN_ACCESS", "ADMIN", null,
                    Map.of("endpoint", "GET /admin/audit-logs"), true);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }
        return ApiResponse.ok(auditLogService.findByFilter(actorId, actionType, from, to, pageable));
    }
}
