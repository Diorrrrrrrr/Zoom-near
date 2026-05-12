package kr.zoomnear.domain.admin.dto;

import jakarta.validation.constraints.NotNull;
import kr.zoomnear.domain.profile.Role;

/// 어드민이 사용자 role 을 직접 교체할 때 사용. ADMIN→ADMIN 자기 강등은 별도 차단.
public record ChangeRoleRequest(@NotNull Role role) {}
