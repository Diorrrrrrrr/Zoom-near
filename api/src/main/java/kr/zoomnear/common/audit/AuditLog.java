package kr.zoomnear.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 감사 로그 대상 메서드 마커.
/// AuditAspect가 audit_logs 테이블에 INSERT (성공/실패 모두 기록).
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /// 감사 액션 코드 (예: "AUTH_SIGNUP", "AUTH_LOGIN"). value()와 동일.
    String value() default "";

    /// 별칭 — 명시 시 value보다 우선 사용된다.
    String action() default "";

    /// 감사 대상 종류 (예: "user", "event", "approval"). 비어 있으면 NULL.
    String targetType() default "";
}
