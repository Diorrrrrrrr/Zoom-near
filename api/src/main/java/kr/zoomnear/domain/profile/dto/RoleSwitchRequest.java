package kr.zoomnear.domain.profile.dto;

import jakarta.validation.constraints.NotNull;
import kr.zoomnear.domain.profile.Role;

/// 역할 전환 요청 DTO. DUNDUN ↔ TUNTUN만 허용.
public record RoleSwitchRequest(
        @NotNull Role newRole
) {
}
