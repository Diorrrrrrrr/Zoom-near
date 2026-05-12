package kr.zoomnear.domain.invite;

/// invite_tokens.status. 발급(PENDING)→소비(CONSUMED) 또는 만료/철회.
public enum InviteStatus {
    PENDING,
    CONSUMED,
    EXPIRED,
    REVOKED
}
