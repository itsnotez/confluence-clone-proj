package com.company.wiki.notification.dto;

import com.company.wiki.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    public record Response(
            Long id,
            Long userId,
            String type,
            String title,
            String message,
            boolean isRead,
            String linkUrl,
            LocalDateTime createdAt
    ) {
        public static Response from(Notification n) {
            return new Response(
                    n.getId(),
                    n.getUserId(),
                    n.getType(),
                    n.getTitle(),
                    n.getMessage(),
                    n.isRead(),
                    n.getLinkUrl(),
                    n.getCreatedAt()
            );
        }
    }

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
