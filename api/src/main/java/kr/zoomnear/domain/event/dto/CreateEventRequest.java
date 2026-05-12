package kr.zoomnear.domain.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import kr.zoomnear.domain.event.EventCategory;
import kr.zoomnear.domain.event.EventVisibility;

/// 이벤트 생성 요청 DTO. 매니저 프로그램은 MANAGER 역할만 사용 가능.
public record CreateEventRequest(

        @Size(max = 100)
        String regionText,

        @NotNull
        EventCategory category,

        @NotBlank
        @Size(max = 120)
        String title,

        @Size(max = 4000)
        String description,

        @NotNull
        Instant startsAt,

        @NotNull
        Instant endsAt,

        @Min(1)
        int capacity,

        @NotNull
        @PositiveOrZero
        BigDecimal pointCost,

        EventVisibility visibility,

        boolean managerProgram
) {
}
