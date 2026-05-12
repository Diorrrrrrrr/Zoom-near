package kr.zoomnear.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E 시나리오 #5 — 초대 링크 가입 → 자동 연동 생성.
 *
 * 단계:
 *   1. 기존 DUNDUN POST /api/v1/invite/token → invite_token 발급
 *   2. ANON POST /api/v1/auth/signup-via-invite (token + 신규 계정 정보)
 *   3. 가입 즉시 DUNDUN-TUNTUN linkage ACTIVE 생성 확인
 *   4. GET /api/v1/linkages/me (신규 TUNTUN 토큰) → linkage 존재 확인
 *   5. 만료 토큰 재사용 시도 → 409
 *
 * TODO(Day2-LaneQ):
 *   - @SpringBootTest(webEnvironment=RANDOM_PORT) + Testcontainers Postgres 활성화
 *   - InviteController, InviteFacade 완성 필요 (Lane B PM)
 *   - AuthFacade.signup inviteToken 소비 로직 연결 필요
 */
@DisplayName("E2E Flow #5 — 초대 링크 가입 → 자동 연동")
class E2EFlow5Test {

    @Test
    @Disabled("TODO(Day2-LaneQ): Testcontainers + InviteFacade 완성 후 활성화")
    @DisplayName("초대 링크로 가입 시 DUNDUN-TUNTUN 연동이 자동 생성된다")
    void signup_via_invite_creates_linkage_automatically() {
        // TODO(Day2-LaneQ): 단계별 구현
    }

    @Test
    @Disabled("TODO(Day2-LaneQ): InviteFacade 완성 후 활성화")
    @DisplayName("만료된 초대 토큰으로 가입 시도 → 400/409")
    void signup_via_expired_invite_token_rejected() {
        // TODO(Day2-LaneQ): 단계별 구현
    }
}
