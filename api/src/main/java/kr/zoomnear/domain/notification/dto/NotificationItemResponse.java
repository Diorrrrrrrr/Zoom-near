package kr.zoomnear.domain.notification.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.domain.notification.Notification;

/// 알림 단건 응답 DTO.
public record NotificationItemResponse(
        UUID id,
        String type,
        String title,
        String body,
        Map<String, Object> payload,
        Instant readAt,
        Instant createdAt
) {

    public static NotificationItemResponse from(Notification n) {
        return new NotificationItemResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getPayload(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
