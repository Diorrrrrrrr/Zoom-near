package kr.zoomnear.domain.approval;

/// approvals.status. EXPIRED는 expires_at 경과 후 배치/요청에서 마킹.
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
}
