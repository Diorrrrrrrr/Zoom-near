package kr.zoomnear.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E 시나리오 #1 — 신규 튼튼이 가입 → 이벤트 참여 → 포인트 차감 확인.
 *
 * 단계:
 *   1. POST /api/v1/auth/signup (role=TUNTUN)
 *   2. POST /api/v1/points/mock-topup (잔액 충전)
 *   3. GET /api/v1/events → 이벤트 목록
 *   4. POST /api/v1/events/{id}/join
 *   5. GET /api/v1/points/me/balance → 잔액 차감 확인
 *
 * TODO(Day2-LaneQ):
 *   - @SpringBootTest(webEnvironment=RANDOM_PORT) + Testcontainers Postgres 활성화
 *   - Flyway 마이그레이션 V001/V002 완성 필요
 *   - EventController, PointController 완성 필요 (Lane B PM)
 */
@DisplayName("E2E Flow #1 — TUNTUN 가입 → 이벤트 참여 → 잔액 차감")
class E2EFlow1Test {

    @Test
    @Disabled("TODO(Day2-LaneQ): Testcontainers + 컨트롤러 완성 후 활성화")
    @DisplayName("신규 TUNTUN이 가입하고 이벤트에 참여하면 포인트가 차감된다")
    void tuntun_signup_topup_join_event_balance_decreases() {
        // TODO(Day2-LaneQ):
        // TestRestTemplate client = ...;
        //
        // // 1. 가입
        // SignupRequest signup = new SignupRequest("e2e_tuntun1", "P@ss1234!", "010-0001-0001",
        //         "e2e@test.com", "E2E튼튼", Role.TUNTUN, null);
        // ResponseEntity<TokenResponse> signupRes = client.postForEntity(
        //         "/api/v1/auth/signup", signup, TokenResponse.class);
        // assertThat(signupRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // String token = signupRes.getBody().accessToken();
        //
        // // 2. 포인트 충전
        // HttpHeaders headers = new HttpHeaders();
        // headers.setBearerAuth(token);
        // Map<String, Object> topup = Map.of("amount", 10000, "referenceId", UUID.randomUUID().toString());
        // client.exchange("/api/v1/points/mock-topup", HttpMethod.POST,
        //         new HttpEntity<>(topup, headers), Void.class);
        //
        // // 3. 이벤트 목록 → 첫 번째 이벤트 선택 (사전 시드 필요)
        // // 4. join
        // // 5. 잔액 확인
    }
}
