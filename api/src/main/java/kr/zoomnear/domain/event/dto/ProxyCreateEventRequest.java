package kr.zoomnear.domain.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/// 둔둔의 대리 이벤트 등록 요청 DTO.
/// payload 검증은 CreateEventRequest의 jakarta validation을 그대로 활용한다.
public record ProxyCreateEventRequest(

        @NotNull
        UUID tuntunId,

        @NotNull
        @Valid
        CreateEventRequest event
) {
}
