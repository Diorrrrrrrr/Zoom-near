package kr.zoomnear.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// event_participations 테이블 ORM 매핑.
/// proxiedBy가 NULL이면 본인 참여, 값이 있으면 둔둔이 튼튼을 대리해 신청한 케이스.
@Entity
@Table(name = "event_participations")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventParticipation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "proxied_by")
    private UUID proxiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ParticipationStatus status;

    @Column(name = "approval_id")
    private UUID approvalId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventParticipation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
