package kr.zoomnear.domain.point;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/// PointWallet 영속성 어댑터.
public interface PointWalletRepository extends JpaRepository<PointWallet, UUID> {
}
