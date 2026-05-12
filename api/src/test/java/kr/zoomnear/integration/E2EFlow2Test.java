package kr.zoomnear.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E 시나리오 #2 — 든든이 가입 → 튼튼이와 연동 → 대리 이벤트 참여 → 승인 흐름.
 *
 * 단계:
 *   1. POST /api/v1/auth/signup (role=DUNDUN)
 *   2. POST /api/v1/auth/signup (role=TUNTUN)
 *   3. POST /api/v1/linkage (dundun → tuntun 연동 요청)
 *   4. POST /api/v1/proxy/events/{id}/join (든든이 대리 참여 → PENDING_APPROVAL)
 *   5. POST /api/v1/approvals/{id}/approve (튼튼이 승인 → CONFIRMED)
 *   6. GET /api/v1/points/me/balance 튼튼이 잔액 차감 확인
 *
 * TODO(Day2-LaneQ):
 *   - @SpringBootTest(webEnvironment=RANDOM_PORT) + Testcontainers Postgres 활성화
 *   - LinkageController, ProxyController, ApprovalController 완성 필요
 */
@DisplayName("E2E Flow #2 — DUNDUN 연동 → 대리 참여 → 승인")
class E2EFlow2Test {

    @Test
    @Disabled("TODO(Day2-LaneQ): Testcontainers + 컨트롤러 완성 후 활성화")
    @DisplayName("든든이가 연동 후 대리 참여하고 튼튼이가 승인하면 잔액이 차감된다")
    void dundun_proxy_join_tuntun_approves_balance_decreases() {
        // TODO(Day2-LaneQ): 단계별 구현
    }
}
