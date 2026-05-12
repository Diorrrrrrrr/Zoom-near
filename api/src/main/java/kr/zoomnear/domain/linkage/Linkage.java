package kr.zoomnear.domain.linkage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// linkages 테이블 ORM 매핑. V001 스키마의 컬럼명 tunchun_id를 도메인의 tuntunId로 별칭한다.
/// 둔둔 1인당 ACTIVE 4명 제한과 역할 검증은 DB 트리거가 보장한다.
@Entity
@Table(name = "linkages")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Linkage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "dundun_id", nullable = false)
    private UUID dundunId;

    @Column(name = "tunchun_id", nullable = false)
    private UUID tuntunId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private LinkageStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Linkage other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
