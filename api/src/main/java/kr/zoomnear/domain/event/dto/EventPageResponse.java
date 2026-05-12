package kr.zoomnear.domain.event.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/// 이벤트 페이지 응답 어댑터. Spring Page → 평탄화된 객체.
public record EventPageResponse(
        List<EventSummaryResponse> content,
        long totalElements,
        int page,
        int size
) {

    public static EventPageResponse from(Page<EventSummaryResponse> p) {
        return new EventPageResponse(
                p.getContent(),
                p.getTotalElements(),
                p.getNumber(),
                p.getSize());
    }
}
