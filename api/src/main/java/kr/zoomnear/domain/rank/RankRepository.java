package kr.zoomnear.domain.rank;

import org.springframework.data.jpa.repository.JpaRepository;

/// Rank 영속성 어댑터. 코드(PK) 단건 조회와 매핑 메서드를 제공한다.
public interface RankRepository extends JpaRepository<Rank, String> {
}
