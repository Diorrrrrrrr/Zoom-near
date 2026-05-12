package kr.zoomnear.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/// 리프레시 토큰 갱신 요청 DTO.
public record RefreshRequest(

        @NotBlank
        String refreshToken
) {
}
