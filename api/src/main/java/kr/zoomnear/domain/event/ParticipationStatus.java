package kr.zoomnear.domain.event;

/// event_participations.status. PENDING_APPROVAL은 대리참여 승인 대기 상태.
public enum ParticipationStatus {
    PENDING_APPROVAL,
    CONFIRMED,
    CANCELED,
    COMPLETED,
    NO_SHOW
}
