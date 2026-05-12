package kr.zoomnear.domain.point.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/// 모의 충전 응답 DTO. 잔액과 충전 메타데이터를 함께 반환한다.
public record MockTopupResponse(
        BigDecimal newBalance,
        Long mockTopupId,
        BigDecimal amount,
        UUID beneficiaryId,
        Instant createdAt
) {
}
