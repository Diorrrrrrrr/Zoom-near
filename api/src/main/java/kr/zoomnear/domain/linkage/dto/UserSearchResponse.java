package kr.zoomnear.domain.linkage.dto;

import java.util.UUID;

/// 둔둔이 코드로 튼튼을 검색한 결과. 최소한의 식별정보만 노출.
public record UserSearchResponse(
        UUID id,
        String name,
        String uniqueCode
) {
}
