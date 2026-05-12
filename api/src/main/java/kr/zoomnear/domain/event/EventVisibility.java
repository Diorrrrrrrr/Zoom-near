package kr.zoomnear.domain.event;

/// 이벤트 노출 범위. 동일 동네만 vs. 인접 동네까지.
public enum EventVisibility {
    REGION_ONLY,
    ADJACENT
}
