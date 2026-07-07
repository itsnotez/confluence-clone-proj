package com.company.wiki.notification.controller;

import com.company.wiki.common.response.ApiResponse;
import com.company.wiki.notification.dto.NotificationDto;
import com.company.wiki.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 (isRead 필터 선택적, 페이징)
     * GET /notifications?isRead=false&page=0&size=20
     */
    @GetMapping
    public ApiResponse<NotificationDto.PagedResponse<NotificationDto.Response>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        return ApiResponse.ok(notificationService.getNotifications(userId, isRead, pageable));
    }

    /**
     * 미읽음 알림 수 조회
     * GET /notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    /**
     * 단건 읽음처리 — IDOR 방지: NotificationService.markAsRead()에서 userId 소유자 검증
     * PATCH /notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        notificationService.markAsRead(id, userId);
        return ApiResponse.ok(null);
    }

    /**
     * 전체 읽음처리
     * PATCH /notifications/read-all
     */
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UserDetails principal) {
        Long userId = getCurrentUserId(principal);
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok(null);
    }

    private Long getCurrentUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }
}
