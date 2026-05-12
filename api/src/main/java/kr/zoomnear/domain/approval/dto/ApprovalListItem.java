package kr.zoomnear.domain.approval.dto;

import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.approval.ApprovalType;

/// 승인 목록 응답 항목.
/// requesterLoginId 는 화면 표시용으로 함께 노출 — 휴대폰 번호 같은 PII 는 노출하지 않는다.
public record ApprovalListItem(
        UUID id,
        ApprovalType type,
        ApprovalStatus status,
        UUID requesterId,
        String requesterName,
        String requesterLoginId,
        String payloadSummary,
        Instant expiresAt,
        Instant createdAt
) {
}
