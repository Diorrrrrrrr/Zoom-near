package kr.zoomnear.domain.profile.dto;

import java.math.BigDecimal;
import java.util.UUID;
import kr.zoomnear.domain.profile.Role;

/// 본인 프로필 응답. 잔액, 랭크 코드, 랭크 표시명까지 포함한다.
public record MeResponse(
        UUID id,
        String loginId,
        String name,
        Role role,
        String uniqueCode,
        BigDecimal balance,
        String rankCode,
        String rankDisplayName
) {
}
