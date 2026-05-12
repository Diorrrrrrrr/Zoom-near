package kr.zoomnear.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E 시나리오 #4 — 이벤트 정원 초과 race (50명 동시 참여, capacity=10).
 *
 * 단계:
 *   1. 50명 TUNTUN 가입 + 각 충분한 잔액 부여
 *   2. capacity=10 이벤트 생성 (MANAGER 계정)
 *   3. 50 스레드 동시 POST /api/v1/events/{id}/join
 *   4. 201 응답 정확히 10건, 409(EVENT_FULL) 40건 검증
 *   5. DB event_participations CONFIRMED 10건 검증
 *
 * TODO(Day2-LaneQ):
 *   - @SpringBootTest(webEnvironment=RANDOM_PORT) + Testcontainers Postgres 활성화
 *   - EventParticipationFacade 비관 락 구현 완성 필요
 *   - Flyway V002 완성 필요
 */
@DisplayName("E2E Flow #4 — 이벤트 정원 초과 동시성 race")
class E2EFlow4Test {

    @Test
    @Disabled("TODO(Day2-LaneQ): Testcontainers + 동시성 구현 완성 후 활성화")
    @DisplayName("50명 동시 join, capacity=10 → 정확히 10명 CONFIRMED")
    void fifty_concurrent_joins_capacity_10_exactly_10_succeed() throws InterruptedException {
        // TODO(Day2-LaneQ): ExecutorService 50 스레드 동시 호출 구현
        // assertThat(successCount.get()).isEqualTo(10);
        // assertThat(eventFullCount.get()).isEqualTo(40);
    }
}
