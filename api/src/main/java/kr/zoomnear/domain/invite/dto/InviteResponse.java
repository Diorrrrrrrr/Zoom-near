package kr.zoomnear.domain.invite.dto;

import java.time.Instant;
import java.util.UUID;

/// 초대 토큰 응답 DTO. 토큰 발급/조회 시 inviter 이름과 사용 가능한 url 까지 노출한다.
public record InviteResponse(
        UUID id,
        UUID token,
        UUID inviterDundunId,
        String inviterName,
        Instant expiresAt,
        String status,
        String url
) {
}
