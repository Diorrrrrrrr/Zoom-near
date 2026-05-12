package kr.zoomnear.domain.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// EventParticipation 영속성 어댑터.
/// 정원 카운트 시 PENDING_APPROVAL + CONFIRMED 합산을 사용한다.
public interface EventParticipationRepository extends JpaRepository<EventParticipation, UUID> {

    long countByEventIdAndStatusIn(UUID eventId, Collection<ParticipationStatus> statuses);

    Optional<EventParticipation> findByApprovalId(UUID approvalId);

    List<EventParticipation> findByParticipantIdOrderByJoinedAtDesc(UUID participantId);

    /// 현재 사용자의 특정 이벤트 참여 이력 중 가장 최근 1건 (CANCELED 포함).
    Optional<EventParticipation> findFirstByEventIdAndParticipantIdOrderByJoinedAtDesc(
            UUID eventId, UUID participantId);

    List<EventParticipation> findByEventIdAndStatusIn(UUID eventId, Collection<ParticipationStatus> statuses);

    /// 여러 이벤트의 활성 참여자 수를 한 번에 집계. 목록 화면 N+1 회피.
    @Query("SELECT p.eventId, COUNT(p) FROM EventParticipation p "
            + "WHERE p.eventId IN :ids AND p.status IN :statuses GROUP BY p.eventId")
    List<Object[]> countActiveByEventIds(@Param("ids") Collection<UUID> ids,
                                         @Param("statuses") Collection<ParticipationStatus> statuses);

    long countByParticipantIdAndStatus(UUID participantId, ParticipationStatus status);

    /// 역할 전환 검증용: 사용자가 미래 이벤트에 활성 참여 중인 건수.
    @Query("SELECT COUNT(p) FROM EventParticipation p, SocialEvent e "
            + "WHERE p.eventId = e.id AND p.participantId = :userId "
            + "AND p.status IN :statuses AND e.endsAt > :now")
    long countActiveFutureParticipations(@Param("userId") UUID userId,
                                         @Param("statuses") Collection<ParticipationStatus> statuses,
                                         @Param("now") Instant now);
}
