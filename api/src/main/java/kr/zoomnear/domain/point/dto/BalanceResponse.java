package kr.zoomnear.domain.point.dto;

import java.math.BigDecimal;
import java.util.UUID;

/// 포인트 잔액 응답 DTO.
public record BalanceResponse(
        UUID userId,
        BigDecimal balance
) {
}
