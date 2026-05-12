package kr.zoomnear.domain.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.notification.dto.NotificationItemResponse;
import kr.zoomnear.domain.notification.dto.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 본인 알림 조회·읽음 처리 도메인 파사드. INSERT는 NotificationDispatcher가 별도로 수행한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;

    /// 본인 알림 목록 + 카운트.
    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID userId, Boolean unread, int limit, int offset) {
        int safeLimit = Math.min(MAX_LIMIT, Math.max(1, limit));
        int safeOffset = Math.max(0, offset);
        Pageable pageable = PageRequest.of(safeOffset / safeLimit, safeLimit);

        List<Notification> page = (Boolean.TRUE.equals(unread))
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationItemResponse> items = page.stream()
                .map(NotificationItemResponse::from)
                .toList();

        long total = notificationRepository.countByUserId(userId);
        long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationListResponse(items, total, unreadCount);
    }

    /// 알림 읽음 처리. 본인 소유가 아니면 FORBIDDEN.
    @Transactional
    public void markRead(UUID notificationId, UUID currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!notification.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 알림만 읽음 처리할 수 있습니다.");
        }
        if (notification.getReadAt() != null) {
            return;
        }
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }
}
