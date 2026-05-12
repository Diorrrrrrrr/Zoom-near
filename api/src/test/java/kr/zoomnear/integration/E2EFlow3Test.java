package kr.zoomnear.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E 시나리오 #3 — 포인트 충전 멱등성 (동일 referenceId 100회 → 1회만 적립).
 *
 * 단계:
 *   1. POST /api/v1/auth/signup (role=TUNTUN)
 *   2. 동일 referenceId로 POST /api/v1/points/mock-topup 100회
 *   3. GET /api/v1/points/me/balance → amount 1회분만 증가 확인
 *   4. GET /api/v1/points/me/ledger → 1행만 존재 확인
 *
 * TODO(Day2-LaneQ):
 *   - @SpringBootTest(webEnvironment=RANDOM_PORT) + Testcontainers Postgres 활성화
 *   - PointController, ledger UNIQUE 제약 마이그레이션 완성 필요
 */
@DisplayName("E2E Flow #3 — 포인트 충전 멱등성")
class E2EFlow3Test {

    @Test
    @Disabled("TODO(Day2-LaneQ): Testcontainers + PointController 완성 후 활성화")
    @DisplayName("동일 referenceId로 100회 충전 시도 → 잔액은 1회분만 증가한다")
    void topup_same_reference_100_times_balance_increments_once() {
        // TODO(Day2-LaneQ): 단계별 구현
        // for (int i = 0; i < 100; i++) { POST /points/mock-topup with same referenceId }
        // assertThat(balance).isEqualTo(initialBalance + amount);
        // assertThat(ledgerRows).isEqualTo(1);
    }
}
