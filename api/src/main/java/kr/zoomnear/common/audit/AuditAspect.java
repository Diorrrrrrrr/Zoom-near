package kr.zoomnear.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/// {@link AuditLog} 어노테이션이 붙은 메서드 호출을 가로채 audit_logs INSERT.
/// TransactionTemplate(PROPAGATION_REQUIRES_NEW)로 별도 트랜잭션에서 기록 — 비즈니스 트랜잭션 롤백에 영향받지 않음.
@Slf4j
@Aspect
@Component
public class AuditAspect {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate auditTxTemplate;

    public AuditAspect(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PlatformTransactionManager txManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.auditTxTemplate = new TransactionTemplate(txManager);
        this.auditTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @AfterReturning(value = "@annotation(audit)", returning = "result")
    public void logSuccess(JoinPoint jp, AuditLog audit, Object result) {
        record(audit, jp, "SUCCESS", null);
    }

    @AfterThrowing(value = "@annotation(audit)", throwing = "ex")
    public void logFailure(JoinPoint jp, AuditLog audit, Throwable ex) {
        record(audit, jp, "FAILED", ex);
    }

    private void record(AuditLog audit, JoinPoint jp, String status, Throwable ex) {
        try {
            auditTxTemplate.executeWithoutResult(tx -> doInsert(audit, jp, status, ex));
        } catch (Exception persistErr) {
            log.warn("AuditAspect.record persist failed action={} status={} err={}",
                    audit.value(), status, persistErr.getMessage());
        }
    }

    private void doInsert(AuditLog audit, JoinPoint jp, String status, Throwable ex) {
        String action = resolveAction(audit, jp);
        String targetType = audit.targetType().isBlank() ? null : audit.targetType();
        String targetId = extractTargetId(jp);
        UUID actorId = currentActorId();
        String payload = buildPayload(jp, ex);
        String ip = currentIp();
        String ua = currentUserAgent();

        jdbcTemplate.update(
                "INSERT INTO audit_logs(actor_id, action, target_type, target_id, payload, ip, user_agent, status) "
                        + "VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)",
                actorId, action, targetType, targetId, payload, ip, ua, status);
    }

    private String resolveAction(AuditLog audit, JoinPoint jp) {
        if (!audit.action().isBlank()) {
            return audit.action();
        }
        if (!audit.value().isBlank()) {
            return audit.value();
        }
        return jp.getSignature().toShortString();
    }

    private String extractTargetId(JoinPoint jp) {
        Object[] args = jp.getArgs();
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid.toString();
            }
        }
        return null;
    }

    private UUID currentActorId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppPrincipal p) {
                return p.userId();
            }
        } catch (Exception ignored) {
            // SecurityContext 미주입 (배치/스케줄러)인 경우 NULL
        }
        return null;
    }

    private String currentIp() {
        return getRequest().map(HttpServletRequest::getRemoteAddr).orElse(null);
    }

    private String currentUserAgent() {
        return getRequest().map(r -> r.getHeader("User-Agent")).orElse(null);
    }

    private java.util.Optional<HttpServletRequest> getRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return java.util.Optional.ofNullable(attrs).map(ServletRequestAttributes::getRequest);
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private String buildPayload(JoinPoint jp, Throwable ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("method", jp.getSignature().toShortString());
        body.put("at", Instant.now().toString());
        if (ex != null) {
            body.put("error", ex.getClass().getSimpleName());
            body.put("message", ex.getMessage());
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception serErr) {
            return "{}";
        }
    }
}
