package kr.zoomnear.infra.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 도메인 레이어가 알림을 발송할 때 사용하는 단일 진입점.
 *
 * <p>동작:
 * <ol>
 *   <li>notifications 테이블에 행을 INSERT 한다.</li>
 *   <li>FcmClient 를 통해 푸시 알림을 전송한다 (실패해도 예외를 전파하지 않는다).</li>
 * </ol>
 *
 * <p>Day 2: NotificationRepository(JPA) 로 교체 예정.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final FcmClient fcmClient;

    public NotificationDispatcher(JdbcTemplate jdbc, FcmClient fcmClient) {
        this.jdbc = jdbc;
        this.fcmClient = fcmClient;
    }

    /**
     * 알림을 저장하고 FCM 푸시를 발송한다.
     *
     * @param userId  수신자 UUID
     * @param type    알림 종류
     * @param title   알림 제목
     * @param body    알림 본문
     * @param payload 추가 데이터 (nullable)
     */
    public void notify(UUID userId, NotificationType type, String title, String body, Map<String, Object> payload) {
        persistNotification(userId, type, title, body, payload);
        dispatchPush(userId, title, body, payload);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void persistNotification(UUID userId, NotificationType type, String title, String body, Map<String, Object> payload) {
        try {
            // PG 드라이버가 Instant 의 SQL 타입을 추론하지 못하므로 명시적으로 Timestamp 로 변환.
            jdbc.update(
                    """
                    INSERT INTO notifications (id, user_id, type, title, body, payload, read_at, created_at)
                    VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, NULL, ?)
                    """,
                    userId,
                    type.name(),
                    title,
                    body,
                    toJsonbObject(payload),
                    Timestamp.from(Instant.now())
            );
        } catch (Exception e) {
            // DB 저장 실패는 로그만 남기고 계속 진행 (알림 누락보다 서비스 중단이 더 큰 피해)
            log.error("[Notification] DB persist failed. userId={} type={} error={}", userId, type, e.getMessage(), e);
        }
    }

    private PGobject toJsonbObject(Map<String, Object> payload) {
        PGobject obj = new PGobject();
        try {
            obj.setType("jsonb");
            obj.setValue(payload == null ? "{}" : MAPPER.writeValueAsString(payload));
        } catch (SQLException | JsonProcessingException ex) {
            try {
                obj.setType("jsonb");
                obj.setValue("{}");
            } catch (SQLException ignored) {
                // 빈 jsonb 으로 폴백 — setType 도 실패할 일은 없음
            }
        }
        return obj;
    }

    private void dispatchPush(UUID userId, String title, String body, Map<String, Object> payload) {
        try {
            fcmClient.sendToUser(userId, title, body, payload);
        } catch (Exception e) {
            // FCM 실패는 조용히 무시 — push 는 best-effort
            log.warn("[Notification] FCM dispatch failed (ignored). userId={} error={}", userId, e.getMessage());
        }
    }
}
