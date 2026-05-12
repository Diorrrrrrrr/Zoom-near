package kr.zoomnear.domain.linkage.dto;

import java.math.BigDecimal;
import java.util.UUID;

/// 연동 목록 단건. 호출자 시점에서 본 상대방 사용자(loginId/uniqueCode/잔액 포함)와 본인 역할을 함께 반환한다.
/// otherUserBalance 는 보안 정책: 호출자가 든든이일 때(=상대가 튼튼이)만 채워지며, 그 외에는 null.
public record LinkageListItem(
        UUID id,
        UUID otherUserId,
        String otherUserName,
        String otherUserLoginId,
        String otherUserUniqueCode,
        BigDecimal otherUserBalance,
        boolean isPrimary,
        String role
) {
}
