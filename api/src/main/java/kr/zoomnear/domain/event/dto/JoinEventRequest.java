package kr.zoomnear.domain.event.dto;

import java.util.UUID;

/// 이벤트 참여 요청 DTO. proxiedTuntunId가 채워지면 둔둔이 튼튼을 대리 신청.
public record JoinEventRequest(
        UUID proxiedTuntunId
) {
}
