package kr.zoomnear.domain.approval;

/// approvals.type. 사용자 간 워크플로우의 사유 종류.
public enum ApprovalType {
    EVENT_JOIN,
    EVENT_CANCEL,
    EVENT_CREATE,
    /// 튼튼 ↔ 든든 양방향 연동 요청. 요청자=actor, 승인자=상대방.
    LINKAGE_CREATE
}
