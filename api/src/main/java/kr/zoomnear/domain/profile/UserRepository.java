package kr.zoomnear.domain.profile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/// User 영속성 어댑터. 로그인 ID·고유 코드 조회를 제공한다.
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByUniqueCode(String uniqueCode);

    boolean existsByLoginId(String loginId);

    boolean existsByUniqueCode(String uniqueCode);
}
