package kr.zoomnear.domain.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/// 본인 충전 요청 DTO.
public record TopupRequest(

        @NotNull
        @Positive
        BigDecimal amount,

        @Size(max = 200)
        String reasonText
) {
}
