package kr.zoomnear.domain.point;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/// PointLedger 영속성 어댑터.
public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    List<PointLedger> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
}
