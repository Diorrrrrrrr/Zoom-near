package kr.zoomnear.domain.manager.dto;

import jakarta.validation.constraints.Size;

/// 매니저 신청 요청 DTO. 사유 텍스트만 받는다.
public record ManagerApplyRequest(

        @Size(max = 4000)
        String reason
) {
}
