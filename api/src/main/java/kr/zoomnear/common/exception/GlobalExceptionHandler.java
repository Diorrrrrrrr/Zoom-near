package kr.zoomnear.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/// 전역 예외 매핑. 응답 포맷: { code, message, details }.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(
            BusinessException ex, HttpServletRequest req) {
        ErrorCode code = ex.getCode();
        log.warn("BusinessException [{}] {} ({} {})", code, ex.getMessage(),
                req.getMethod(), req.getRequestURI());
        return ResponseEntity.status(code.getStatus())
                .body(body(code.name(), ex.getMessage(), Map.of("args", ex.getArgs())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldErrorToMap)
                .collect(Collectors.toList());
        log.warn("Validation failed at {} {}: {}", req.getMethod(), req.getRequestURI(), fields);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(body(ErrorCode.VALIDATION_FAILED.name(),
                        ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                        Map.of("fields", fields)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        log.warn("AccessDenied at {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus())
                .body(body(ErrorCode.FORBIDDEN.name(),
                        ErrorCode.FORBIDDEN.getDefaultMessage(), Map.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(
            AuthenticationException ex, HttpServletRequest req) {
        log.warn("AuthenticationException at {} {}: {}",
                req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getStatus())
                .body(body(ErrorCode.UNAUTHORIZED.name(),
                        ErrorCode.UNAUTHORIZED.getDefaultMessage(), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {} {}",
                req.getMethod(), req.getRequestURI(), ex);
        // TEMP DEBUG: expose exception class+message+rootCause to response details
        // TODO: revert after 500 diagnosis on POST /api/v1/events
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exception", ex.getClass().getName());
        details.put("message", ex.getMessage());
        details.put("rootException", root.getClass().getName());
        details.put("rootMessage", root.getMessage());
        return ResponseEntity.status(ErrorCode.INTERNAL.getStatus())
                .body(body(ErrorCode.INTERNAL.name(),
                        ErrorCode.INTERNAL.getDefaultMessage(), details));
    }

    private Map<String, Object> body(String code, String message, Map<String, ?> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("details", details);
        return body;
    }

    private Map<String, String> fieldErrorToMap(FieldError fe) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field", fe.getField());
        m.put("message", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage());
        return m;
    }
}
