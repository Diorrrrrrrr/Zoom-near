package kr.zoomnear.domain.event;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// SocialEvent 영속성 어댑터. 목록 조회와 정원 차감 시 잠금 조회를 제공한다.
public interface SocialEventRepository extends JpaRepository<SocialEvent, UUID> {

    Page<SocialEvent> findByStatusAndStartsAtAfter(EventStatus status, Instant after, Pageable pageable);

    Page<SocialEvent> findByStatusAndRegionTextContainingAndStartsAtAfter(
            EventStatus status, String regionText, Instant after, Pageable pageable);

    Page<SocialEvent> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);

    /// 정원 차감 시 같은 이벤트에 대한 동시 join을 직렬화하기 위한 비관 락.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SocialEvent e WHERE e.id = :id")
    Optional<SocialEvent> findWithLockById(@Param("id") UUID id);
}
