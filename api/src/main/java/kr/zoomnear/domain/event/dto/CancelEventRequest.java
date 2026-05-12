package kr.zoomnear.domain.event.dto;

import java.util.UUID;

/// 이벤트 취소 요청 DTO. participationId 가 비어있으면 본인의 가장 최근 active 참여를 자동 매핑.
public record CancelEventRequest(
        UUID participationId
) {
}
