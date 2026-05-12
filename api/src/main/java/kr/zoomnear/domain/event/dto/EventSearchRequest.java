package kr.zoomnear.domain.event.dto;

/// 이벤트 목록 조회 검색 조건. 모든 필드 nullable.
public record EventSearchRequest(
        String regionText,
        Integer page,
        Integer size
) {
}
