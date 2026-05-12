package kr.zoomnear.domain.linkage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// Linkage 영속성 어댑터. 둔둔/튼튼 양방향 조회와 활성 여부 검사를 제공한다.
public interface LinkageRepository extends JpaRepository<Linkage, UUID> {

    boolean existsByDundunIdAndTuntunIdAndStatus(UUID dundunId, UUID tuntunId, LinkageStatus status);

    long countByDundunIdAndStatus(UUID dundunId, LinkageStatus status);

    List<Linkage> findByDundunIdAndStatus(UUID dundunId, LinkageStatus status);

    List<Linkage> findByTuntunIdAndStatus(UUID tuntunId, LinkageStatus status);

    Optional<Linkage> findByDundunIdAndTuntunId(UUID dundunId, UUID tuntunId);

    /// 사용자가 둔둔이든 튼튼이든 활성 연결을 가진 수.
    @Query("SELECT COUNT(l) FROM Linkage l WHERE l.status = :status "
            + "AND (l.dundunId = :userId OR l.tuntunId = :userId)")
    long countActiveAsAnyParty(@Param("userId") UUID userId,
                               @Param("status") LinkageStatus status);
}
