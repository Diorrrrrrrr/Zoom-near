package kr.zoomnear.domain.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// 비밀번호 변경 요청 DTO.
public record ChangePasswordRequest(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 64)
        String newPassword
) {
}
