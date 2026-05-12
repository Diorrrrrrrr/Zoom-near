package kr.zoomnear.common.exception;

import lombok.Getter;

/// 도메인 비즈니스 규칙 위반 시 던지는 표준 예외.
/// {@link ErrorCode}와 선택적 컨텍스트 인자(args)를 함께 전달한다.
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Object[] args;

    public BusinessException(ErrorCode code) {
        this(code, code.getDefaultMessage(), new Object[0]);
    }

    public BusinessException(ErrorCode code, String message) {
        this(code, message, new Object[0]);
    }

    public BusinessException(ErrorCode code, Object... args) {
        super(code.getDefaultMessage());
        this.code = code;
        this.args = args == null ? new Object[0] : args;
    }

    public BusinessException(ErrorCode code, String message, Object... args) {
        super(message);
        this.code = code;
        this.args = args == null ? new Object[0] : args;
    }
}
