package kr.zoomnear.domain.notification.dto;

import java.util.List;

/// 알림 목록 응답 DTO. unreadCount는 항상 사용자 전체의 미열람 수.
public record NotificationListResponse(
        List<NotificationItemResponse> items,
        long total,
        long unreadCount
) {
}
