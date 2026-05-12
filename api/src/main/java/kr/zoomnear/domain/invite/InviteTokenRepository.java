package kr.zoomnear.domain.invite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/// InviteToken 영속성 어댑터.
public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {

    Optional<InviteToken> findByToken(UUID token);

    List<InviteToken> findByInviterDundunIdAndStatus(UUID inviterDundunId, InviteStatus status);
}
