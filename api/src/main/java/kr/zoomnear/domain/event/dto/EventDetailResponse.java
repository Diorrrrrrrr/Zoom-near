package kr.zoomnear.domain.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.domain.event.EventCategory;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.EventVisibility;
import kr.zoomnear.domain.event.SocialEvent;

/// 이벤트 상세 응답 DTO. 상세 화면용 — 현재 참여 수 + 호출자 참여 상태 포함.
/// myParticipationStatus: "JOINED" (PENDING_APPROVAL/CONFIRMED) | "CANCELLED" (CANCELED) | null
public record EventDetailResponse(
        UUID id,
        UUID creatorId,
        String regionText,
        EventCategory category,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        long currentJoinedCount,
        BigDecimal pointCost,
        EventStatus status,
        EventVisibility visibility,
        boolean managerProgram,
        String myParticipationStatus,
        Instant createdAt,
        Instant updatedAt
) {

    public static EventDetailResponse from(SocialEvent e, long currentJoinedCount, String myParticipationStatus) {
        return new EventDetailResponse(
                e.getId(),
                e.getCreatorId(),
                e.getRegionText(),
                e.getCategory(),
                e.getTitle(),
                e.getDescription(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getCapacity(),
                currentJoinedCount,
                e.getPointCost(),
                e.getStatus(),
                e.getVisibility(),
                e.isManagerProgram(),
                myParticipationStatus,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
