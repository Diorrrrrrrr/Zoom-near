package kr.zoomnear.domain.manager;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/// ManagerApplication 영속성 어댑터.
public interface ManagerApplicationRepository extends JpaRepository<ManagerApplication, UUID> {

    Page<ManagerApplication> findByStatus(ManagerApplicationStatus status, Pageable pageable);

    List<ManagerApplication> findByApplicantIdAndStatus(UUID applicantId, ManagerApplicationStatus status);
}
