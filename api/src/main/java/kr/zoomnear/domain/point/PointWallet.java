package kr.zoomnear.domain.point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// 사용자 포인트 지갑. user_id를 PK로 1:1 매핑. V001 스키마: point_wallets(NUMERIC(12,0)).
@Entity
@Table(name = "point_wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PointWallet {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static PointWallet emptyFor(UUID userId, Instant now) {
        return PointWallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .updatedAt(now)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PointWallet other)) return false;
        return userId != null && userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
