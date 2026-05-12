package kr.zoomnear.infra.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * FCM 전송 인터페이스.
 * Day 2에 Firebase Admin SDK 실 구현체로 교체 예정.
 */
public interface FcmClient {

    /**
     * 단일 사용자에게 푸시 알림을 전송한다.
     *
     * @param userId 수신자 UUID
     * @param title  알림 제목
     * @param body   알림 본문
     * @param data   추가 데이터 페이로드 (nullable)
     */
    void sendToUser(UUID userId, String title, String body, Map<String, Object> data);

    // ─────────────────────────────────────────────────────────────────────────
    // Stub 구현체: FCM 비활성화 시 기본 빈
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FCM 비활성화 환경(local / test)에서 사용되는 No-op Stub.
     * {@code zoomnear.fcm.enabled=false} 또는 프로퍼티 미설정 시 로드된다.
     */
    @Component
    @ConditionalOnProperty(name = "zoomnear.fcm.enabled", havingValue = "false", matchIfMissing = true)
    class NoopFcmClient implements FcmClient {

        private static final Logger log = LoggerFactory.getLogger(NoopFcmClient.class);

        @Override
        public void sendToUser(UUID userId, String title, String body, Map<String, Object> data) {
            log.info("[FCM-NOOP] userId={} title='{}' body='{}' data={}", userId, title, body, data);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stub 구현체: FCM 활성화 placeholder (Day 2 실 구현 전 임시)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FCM 활성화 환경에서 로드되는 Placeholder Stub.
     * Day 2에 Firebase Admin SDK 호출 코드로 교체한다.
     *
     * @see <a href="https://firebase.google.com/docs/admin/setup">Firebase Admin SDK</a>
     */
    @Component
    @ConditionalOnProperty(name = "zoomnear.fcm.enabled", havingValue = "true")
    class PlaceholderFcmClient implements FcmClient {

        private static final Logger log = LoggerFactory.getLogger(PlaceholderFcmClient.class);

        @Override
        public void sendToUser(UUID userId, String title, String body, Map<String, Object> data) {
            // TODO(Day 2): FirebaseMessaging.getInstance().send(...) 로 교체
            log.info("[FCM-PLACEHOLDER] FCM enabled but SDK not yet wired. userId={} title='{}' body='{}' data={}",
                    userId, title, body, data);
        }
    }
}
