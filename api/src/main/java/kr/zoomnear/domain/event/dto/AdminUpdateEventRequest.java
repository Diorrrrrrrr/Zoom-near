package kr.zoomnear.domain.event.dto;

/// 관리자 이벤트 수정 요청 DTO. reason은 필수 (facade에서 non-blank 검증).
public record AdminUpdateEventRequest(UpdateEventRequest event, String reason) {
}
