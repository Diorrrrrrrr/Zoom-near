package kr.zoomnear.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/// 도메인 전반에서 사용하는 표준 에러 코드 enum.
/// 각 코드는 HTTP 상태와 기본 메시지를 동시에 보유한다.
@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 검증에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
    NOT_LINKED(HttpStatus.FORBIDDEN, "둔둔-튼튼 연동이 필요합니다."),
    INSUFFICIENT_POINTS(HttpStatus.CONFLICT, "포인트가 부족합니다."),
    EVENT_FULL(HttpStatus.CONFLICT, "이벤트 정원이 가득 찼습니다."),
    APPROVAL_EXPIRED(HttpStatus.CONFLICT, "만료된 승인입니다."),
    APPROVAL_ALREADY_DECIDED(HttpStatus.CONFLICT, "이미 처리된 승인입니다."),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
