package kr.zoomnear.domain.approval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// Approval 영속성 어댑터.
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByApproverIdAndStatusOrderByExpiresAtAsc(UUID approverId, ApprovalStatus status);

    List<Approval> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, ApprovalStatus status);

    List<Approval> findByApproverIdAndStatusAndTypeOrderByExpiresAtAsc(
            UUID approverId, ApprovalStatus status, ApprovalType type);

    List<Approval> findByStatusAndExpiresAtBefore(ApprovalStatus status, Instant before);

    @Query("SELECT COUNT(a) FROM Approval a WHERE a.status = :status "
            + "AND (a.requesterId = :userId OR a.approverId = :userId)")
    long countActiveByRequesterOrApprover(@Param("userId") UUID userId,
                                          @Param("status") ApprovalStatus status);
}
