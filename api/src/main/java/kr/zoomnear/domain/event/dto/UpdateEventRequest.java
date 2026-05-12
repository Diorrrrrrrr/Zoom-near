package kr.zoomnear.domain.event.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import kr.zoomnear.domain.event.EventCategory;

/// 이벤트 부분 수정 요청 DTO. 모든 필드는 nullable; null이 아닌 필드만 패치된다.
public record UpdateEventRequest(

        @Size(max = 120)
        String title,

        @Size(max = 4000)
        String description,

        @Size(max = 100)
        String regionText,

        EventCategory category,

        Instant startsAt,

        Instant endsAt,

        @PositiveOrZero
        Integer capacity,

        @PositiveOrZero
        BigDecimal pointCost
) {
}
