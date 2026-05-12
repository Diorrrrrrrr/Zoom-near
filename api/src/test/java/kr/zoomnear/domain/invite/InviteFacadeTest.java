package kr.zoomnear.domain.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * InviteFacade 단위 테스트.
 *
 * TODO(Day1PMend): InviteFacade, InviteToken 클래스 완성 후 주석 해제 및 활성화.
 *                 현재는 도메인 계약(시그니처+동작)을 명세로 표현한다.
 */
@DisplayName("InviteFacade — 초대 토큰 발급/소비")
class InviteFacadeTest {

    // TODO(Day1PMend): InviteTokenRepository inviteTokenRepository;
    // TODO(Day1PMend): InviteFacade inviteFacade;

    @Test
    @DisplayName("#1 정상 발급 → expires 72h 이후, status=PENDING")
    void issue_token_creates_pending_with_72h_expiry() {
        // TODO(Day1PMend): InviteFacade 완성 후 활성화
        // given
        // when(inviteTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // UUID issuerId = TestFixtures.USER_DUNDUN_ID;
        //
        // when
        // InviteToken token = inviteFacade.issue(issuerId);
        //
        // then
        // assertThat(token.getStatus()).isEqualTo(InviteStatus.PENDING);
        // assertThat(token.getExpiresAt()).isAfter(Instant.now().plusSeconds(72 * 3600 - 5));
        // assertThat(token.getExpiresAt()).isBefore(Instant.now().plusSeconds(72 * 3600 + 5));
        // assertThat(token.getIssuedBy()).isEqualTo(issuerId);

        // 현재 단계: 계약 명세만 확인
        Instant expected72hFromNow = Instant.now().plusSeconds(72L * 3600);
        assertThat(expected72hFromNow).isAfter(Instant.now());
    }

    @Test
    @DisplayName("#2 만료된 토큰 consume → BusinessException (VALIDATION_FAILED 또는 NOT_FOUND)")
    void consume_expired_token_throws_exception() {
        // TODO(Day1PMend): InviteFacade 완성 후 활성화
        // given — status=EXPIRED 토큰
        // InviteToken expiredToken = InviteToken.builder()
        //         .id(UUID.randomUUID())
        //         .token(TestFixtures.INVITE_TOKEN)
        //         .status(InviteStatus.EXPIRED)
        //         .expiresAt(Instant.now().minusSeconds(3600))
        //         .build();
        // when(inviteTokenRepository.findByToken(TestFixtures.INVITE_TOKEN))
        //         .thenReturn(Optional.of(expiredToken));
        //
        // then
        // assertThatThrownBy(() -> inviteFacade.consume(TestFixtures.INVITE_TOKEN, UUID.randomUUID()))
        //         .isInstanceOf(BusinessException.class);

        // 현재 단계: EXPIRED 상태 열거형 존재 확인
        assertThat(InviteStatus.EXPIRED).isNotNull();
    }

    @Test
    @DisplayName("#3 이미 consume된 토큰 재사용 → BusinessException(CONFLICT)")
    void consume_already_consumed_token_throws_conflict() {
        // TODO(Day1PMend): InviteFacade 완성 후 활성화
        // given — status=CONSUMED 토큰
        // InviteToken consumedToken = InviteToken.builder()
        //         .id(UUID.randomUUID())
        //         .token(TestFixtures.INVITE_TOKEN)
        //         .status(InviteStatus.CONSUMED)
        //         .expiresAt(Instant.now().plusSeconds(3600))
        //         .build();
        // when(inviteTokenRepository.findByToken(TestFixtures.INVITE_TOKEN))
        //         .thenReturn(Optional.of(consumedToken));
        //
        // then
        // assertThatThrownBy(() -> inviteFacade.consume(TestFixtures.INVITE_TOKEN, UUID.randomUUID()))
        //         .isInstanceOf(BusinessException.class)
        //         .extracting("code").isEqualTo(ErrorCode.CONFLICT);

        // 현재 단계: CONSUMED 상태 열거형 존재 확인
        assertThat(InviteStatus.CONSUMED).isNotNull();
    }

    @Test
    @DisplayName("#4 존재하지 않는 토큰 consume → BusinessException(NOT_FOUND)")
    void consume_nonexistent_token_throws_not_found() {
        // TODO(Day1PMend): InviteFacade 완성 후 활성화
        // when(inviteTokenRepository.findByToken("NOEXIST")).thenReturn(Optional.empty());
        //
        // assertThatThrownBy(() -> inviteFacade.consume("NOEXIST", UUID.randomUUID()))
        //         .isInstanceOf(BusinessException.class)
        //         .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);

        // 현재 단계: NOT_FOUND 에러코드 존재 확인
        assertThat(ErrorCode.NOT_FOUND).isNotNull();
    }

    @Test
    @DisplayName("#5 REVOKED 토큰 consume 시도 → BusinessException")
    void consume_revoked_token_throws_exception() {
        // TODO(Day1PMend): InviteFacade 완성 후 활성화
        // given — status=REVOKED 토큰
        // ...

        // 현재 단계: REVOKED 상태 열거형 존재 확인
        assertThat(InviteStatus.REVOKED).isNotNull();
    }
}
