package kr.zoomnear.domain.admin.dto;

import jakarta.validation.constraints.Size;

/// 관리자의 사용자 정지 요청 DTO.
public record SuspendUserRequest(
        @Size(max = 1000) String reason
) {
}
