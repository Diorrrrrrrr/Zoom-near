package kr.zoomnear.domain.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/// 둔둔이 튼튼에게 대리 충전하는 요청 DTO.
public record ProxyTopupRequest(

        @NotNull
        UUID tuntunId,

        @NotNull
        @Positive
        BigDecimal amount,

        @Size(max = 200)
        String reasonText
) {
}
