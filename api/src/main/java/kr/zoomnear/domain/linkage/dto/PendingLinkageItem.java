package kr.zoomnear.domain.linkage.dto;

import java.time.Instant;
import java.util.UUID;

/// 내가 보낸 대기중 연동 요청 단건 (호출자 = requester).
/// 호출자 시점에서 본 상대방(승인 대기자) 정보 + Approval 메타.
public record PendingLinkageItem(
        UUID approvalId,
        UUID otherUserId,
        String otherUserName,
        String otherUserLoginId,
        String otherUserUniqueCode,
        Instant createdAt,
        Instant expiresAt
) {
}
