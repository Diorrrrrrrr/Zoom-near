package kr.zoomnear.domain.point;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/// MockTopup 영속성 어댑터.
public interface MockTopupRepository extends JpaRepository<MockTopup, Long> {

    List<MockTopup> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<MockTopup> findByChargedByOrderByCreatedAtDesc(UUID chargedBy, Pageable pageable);
}
