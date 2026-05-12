package kr.zoomnear.domain.rank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// ranks 테이블 ORM 매핑. (PPOJJAK/GWIYOMI/KKAMJJIK/HWALHWAL)
@Entity
@Table(name = "ranks")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rank {

    @Id
    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Column(name = "min_count", nullable = false)
    private int minCount;

    @Column(name = "max_count")
    private Integer maxCount;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rank other)) return false;
        return code != null && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
