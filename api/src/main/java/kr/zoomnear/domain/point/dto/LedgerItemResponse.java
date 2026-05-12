package kr.zoomnear.domain.point.dto;

import java.math.BigDecimal;
import java.time.Instant;
import kr.zoomnear.domain.point.PointLedger;
import kr.zoomnear.domain.point.PointReason;

/// 포인트 원장 단건 응답 DTO.
public record LedgerItemResponse(
        Long id,
        BigDecimal delta,
        PointReason reason,
        String referenceType,
        String referenceId,
        BigDecimal balanceAfter,
        Instant createdAt
) {

    public static LedgerItemResponse from(PointLedger l) {
        return new LedgerItemResponse(
                l.getId(),
                l.getDelta(),
                l.getReason(),
                l.getReferenceType(),
                l.getReferenceId(),
                l.getBalanceAfter(),
                l.getCreatedAt());
    }
}
