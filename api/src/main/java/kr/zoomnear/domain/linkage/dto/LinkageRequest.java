package kr.zoomnear.domain.linkage.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/// 둔둔→튼튼 연결 생성 요청.
public record LinkageRequest(

        @NotNull
        UUID tuntunId,

        boolean isPrimary
) {
}
