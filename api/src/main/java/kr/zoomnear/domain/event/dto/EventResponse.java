package kr.zoomnear.domain.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.domain.event.EventCategory;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.EventVisibility;
import kr.zoomnear.domain.event.SocialEvent;

/// 이벤트 응답 DTO (전체 필드).
public record EventResponse(
        UUID id,
        UUID creatorId,
        String regionText,
        EventCategory category,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        BigDecimal pointCost,
        EventStatus status,
        EventVisibility visibility,
        boolean managerProgram,
        Instant createdAt,
        Instant updatedAt
) {

    public static EventResponse from(SocialEvent e) {
        return new EventResponse(
                e.getId(),
                e.getCreatorId(),
                e.getRegionText(),
                e.getCategory(),
                e.getTitle(),
                e.getDescription(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getCapacity(),
                e.getPointCost(),
                e.getStatus(),
                e.getVisibility(),
                e.isManagerProgram(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
