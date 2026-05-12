package kr.zoomnear.domain.point.dto;

import java.util.List;

/// 포인트 원장 목록 응답 DTO.
public record LedgerListResponse(
        List<LedgerItemResponse> items,
        long total,
        boolean hasMore
) {
}
