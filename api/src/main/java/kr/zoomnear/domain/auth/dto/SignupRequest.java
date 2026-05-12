package kr.zoomnear.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.zoomnear.domain.profile.Role;

/// 회원가입 요청 DTO.
/// role은 TUNTUN/DUNDUN만 허용 — ADMIN/MANAGER는 컨트롤러/Facade에서 거부한다.
public record SignupRequest(

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "loginId는 영문/숫자/_ 4~20자")
        String loginId,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        @Pattern(regexp = "^01[016789][-]?\\d{3,4}[-]?\\d{4}$", message = "phone 형식이 올바르지 않습니다")
        String phone,

        @Email
        String email,

        @NotBlank
        @Size(max = 60)
        String name,

        @NotNull
        Role role,

        String inviteToken
) {
}
