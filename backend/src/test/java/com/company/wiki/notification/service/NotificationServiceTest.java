package com.company.wiki.notification.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.notification.dto.NotificationDto;
import com.company.wiki.notification.entity.Notification;
import com.company.wiki.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final Long USER_ID = 1L;   // admin 사용자 (존재하는 user_id)
    private static final Long OTHER_USER_ID = 2L; // 다른 사용자

    /**
     * Test 1: create() 후 미읽음 카운트가 1 증가한다
     */
    @Test
    @DisplayName("create_savesNotification_unreadCountOne: 알림 생성 후 미읽음 카운트 == 1")
    void create_savesNotification_unreadCountOne() {
        // Given: 초기 미읽음 카운트
        long before = notificationRepository.countByUserIdAndIsReadFalse(USER_ID);

        // When
        notificationService.create(USER_ID, "COMMENT", "새 댓글이 달렸습니다", "콘텐츠에 댓글이 달렸습니다.", "/link/1");

        // Then
        long after = notificationRepository.countByUserIdAndIsReadFalse(USER_ID);
        assertThat(after).isEqualTo(before + 1);
    }

    /**
     * Test 2: getUnreadCount()는 is_read=false 레코드만 카운트한다
     */
    @Test
    @DisplayName("getUnreadCount_countsOnlyUnread: isRead=false 레코드만 카운트")
    void getUnreadCount_countsOnlyUnread() {
        // Given: 읽음 1개, 미읽음 2개 생성
        notificationService.create(USER_ID, "COMMENT", "알림1", "내용1", "/link/1");
        notificationService.create(USER_ID, "COMMENT", "알림2", "내용2", "/link/2");

        // 읽음 알림 1개 (직접 저장)
        Notification readNotif = Notification.builder()
                .userId(USER_ID)
                .type("COMMENT")
                .title("읽은 알림")
                .message("읽음 처리된 알림")
                .isRead(true)
                .linkUrl("/link/read")
                .build();
        notificationRepository.save(readNotif);

        // When
        long unreadCount = notificationService.getUnreadCount(USER_ID);

        // Then: 미읽음 2개만 카운트
        assertThat(unreadCount).isGreaterThanOrEqualTo(2L);
        // 읽음 처리된 것은 포함되지 않음 (전체 중 isRead=false인 것만)
    }

    /**
     * Test 3: markAsRead()에 다른 사용자 ID를 넣으면 BusinessException(NOTIFICATION_NOT_FOUND) 발생 (IDOR 방지)
     */
    @Test
    @DisplayName("markAsRead_wrongUser_throwsNotFound: 다른 userId로 읽음처리 시 NOTIFICATION_NOT_FOUND 예외")
    void markAsRead_wrongUser_throwsNotFound() {
        // Given: USER_ID로 알림 생성
        notificationService.create(USER_ID, "COMMENT", "테스트 알림", "내용", "/link/test");

        // 방금 생성된 알림의 ID 조회
        Notification saved = notificationRepository.findByUserId(USER_ID).stream()
                .filter(n -> n.getTitle().equals("테스트 알림"))
                .findFirst()
                .orElseThrow();

        // When/Then: OTHER_USER_ID로 읽음처리 시도 → BusinessException
        Long notifId = saved.getId();
        assertThatThrownBy(() -> notificationService.markAsRead(notifId, OTHER_USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    /**
     * Test 4: markAllAsRead() 후 해당 userId의 모든 알림이 isRead=true
     */
    @Test
    @DisplayName("markAllAsRead_allNotificationsRead: 전체 읽음처리 후 미읽음 카운트 == 0")
    void markAllAsRead_allNotificationsRead() {
        // Given: 미읽음 알림 3개 생성
        notificationService.create(USER_ID, "COMMENT", "알림A", "내용A", "/a");
        notificationService.create(USER_ID, "COMMENT", "알림B", "내용B", "/b");
        notificationService.create(USER_ID, "COMMENT", "알림C", "내용C", "/c");

        // When
        notificationService.markAllAsRead(USER_ID);

        // Then: 미읽음 카운트 == 0
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(USER_ID);
        assertThat(unreadCount).isEqualTo(0L);
    }
}
