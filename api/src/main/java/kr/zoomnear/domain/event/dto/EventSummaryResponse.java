package kr.zoomnear.domain.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.domain.event.EventCategory;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.SocialEvent;

/// 이벤트 목록 요약 응답 DTO. 목록 화면 노출용 핵심 필드만.
/// joinedCount: PENDING_APPROVAL + CONFIRMED 합산. 잔여 자리 계산에 사용.
public record EventSummaryResponse(
        UUID id,
        String title,
        String regionText,
        EventCategory category,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        long joinedCount,
        BigDecimal pointCost,
        EventStatus status,
        boolean managerProgram
) {

    public static EventSummaryResponse from(SocialEvent e, long joinedCount) {
        return new EventSummaryResponse(
                e.getId(),
                e.getTitle(),
                e.getRegionText(),
                e.getCategory(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getCapacity(),
                joinedCount,
                e.getPointCost(),
                e.getStatus(),
                e.isManagerProgram());
    }
}
