package kr.zoomnear.domain.auth.dto;

import java.util.UUID;
import kr.zoomnear.domain.profile.Role;

/// 토큰 응답 DTO. 액세스/리프레시 토큰과 사용자 식별 정보를 함께 반환한다.
public record TokenResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        Role role
) {
}
