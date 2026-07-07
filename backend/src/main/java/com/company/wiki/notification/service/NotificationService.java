package com.company.wiki.notification.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.notification.dto.NotificationDto;
import com.company.wiki.notification.entity.Notification;
import com.company.wiki.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 생성 — 예외는 호출자(CommentService)가 try-catch로 처리
     */
    public void create(Long userId, String type, String title, String message, String linkUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .linkUrl(linkUrl)
                .build();
        notificationRepository.save(notification);
    }

    /**
     * 알림 목록 조회 (isRead 필터 선택적)
     */
    @Transactional(readOnly = true)
    public NotificationDto.PagedResponse<NotificationDto.Response> getNotifications(
            Long userId, Boolean isRead, Pageable pageable) {
        Page<Notification> page;
        if (isRead == null) {
            page = notificationRepository.findByUserId(userId, pageable);
        } else {
            page = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        }
        return new NotificationDto.PagedResponse<>(
                page.getContent().stream().map(NotificationDto.Response::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * 미읽음 카운트 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 단건 읽음처리 — IDOR 방지: findByIdAndUserId로 소유자 검증
     */
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * 전체 읽음처리
     */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
