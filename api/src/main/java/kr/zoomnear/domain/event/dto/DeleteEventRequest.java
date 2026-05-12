package kr.zoomnear.domain.event.dto;

/// 이벤트 삭제 요청 DTO. 사유는 필수 (facade에서 non-blank 검증).
public record DeleteEventRequest(String reason) {
}
