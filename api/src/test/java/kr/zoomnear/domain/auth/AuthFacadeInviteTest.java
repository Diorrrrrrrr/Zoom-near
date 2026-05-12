package kr.zoomnear.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.JwtTokenProvider;
import kr.zoomnear.domain.auth.dto.SignupRequest;
import kr.zoomnear.domain.auth.dto.TokenResponse;
import kr.zoomnear.domain.point.PointWallet;
import kr.zoomnear.domain.point.PointWalletRepository;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthFacade 추가 단위 테스트 — invite token 토픽 5케이스.
 * Lane B AM의 기존 AuthFacadeTest(sanity) 외에 inviteToken 흐름을 검증한다.
 *
 * NOTE: InviteFacade 미완 상태이므로 현재 AuthFacade의 inviteToken 처리가
 *       log.info 수준임을 확인하고 Day1PMend 시점에 실제 소비 로직 연결을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthFacade — invite token 시나리오")
class AuthFacadeInviteTest {

    @Mock UserRepository userRepository;
    @Mock PointWalletRepository pointWalletRepository;
    @Mock JwtTokenProvider tokenProvider;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    AuthFacade authFacade;

    @BeforeEach
    void setUp() {
        authFacade = new AuthFacade(userRepository, pointWalletRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("inviteToken null 이어도 정상 가입 성공")
    void signup_without_invite_token_succeeds() {
        when(userRepository.existsByLoginId(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.issueAccess(any(UUID.class), any(Role.class))).thenReturn("acc");
        when(tokenProvider.issueRefresh(any(UUID.class))).thenReturn("ref");

        SignupRequest req = new SignupRequest(
                "newuser1", "Password1!", "010-1234-5678",
                "u@test.com", "테스트", Role.TUNTUN, null);

        TokenResponse res = authFacade.signup(req);

        assertThat(res.accessToken()).isEqualTo("acc");
        assertThat(res.role()).isEqualTo(Role.TUNTUN);
    }

    @Test
    @DisplayName("inviteToken 빈 문자열 이어도 정상 가입 성공")
    void signup_with_blank_invite_token_succeeds() {
        when(userRepository.existsByLoginId(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.issueAccess(any(UUID.class), any(Role.class))).thenReturn("acc");
        when(tokenProvider.issueRefresh(any(UUID.class))).thenReturn("ref");

        SignupRequest req = new SignupRequest(
                "newuser2", "Password1!", "010-1234-5678",
                null, "테스트", Role.TUNTUN, "   ");

        TokenResponse res = authFacade.signup(req);

        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("inviteToken 값이 있으면 현재는 log 처리 후 정상 가입 (TODO: InviteFacade.consume 연결)")
    void signup_with_invite_token_logs_and_proceeds() {
        // TODO(Day1PMend): InviteFacade.consume 연결 후 실제 소비 검증으로 업그레이드
        when(userRepository.existsByLoginId(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.issueAccess(any(UUID.class), any(Role.class))).thenReturn("acc");
        when(tokenProvider.issueRefresh(any(UUID.class))).thenReturn("ref");

        SignupRequest req = new SignupRequest(
                "newuser3", "Password1!", "010-1234-5678",
                null, "테스트", Role.DUNDUN, "INVITE-VALID-TOKEN");

        TokenResponse res = authFacade.signup(req);

        // 현재 구현은 inviteToken을 log만 남기고 통과 — 가입은 성공해야 한다
        assertThat(res.accessToken()).isEqualTo("acc");
        assertThat(res.role()).isEqualTo(Role.DUNDUN);
    }

    @Test
    @DisplayName("MANAGER role 가입 시도 → FORBIDDEN")
    void signup_manager_role_throws_forbidden() {
        SignupRequest req = new SignupRequest(
                "mgr01", "Password1!", "010-1234-5678",
                null, "매니저", Role.MANAGER, null);

        assertThatThrownBy(() -> authFacade.signup(req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("loginId 중복 → CONFLICT")
    void signup_duplicate_loginId_throws_conflict() {
        when(userRepository.existsByLoginId("existing")).thenReturn(true);

        SignupRequest req = new SignupRequest(
                "existing", "Password1!", "010-1234-5678",
                null, "기존유저", Role.TUNTUN, null);

        assertThatThrownBy(() -> authFacade.signup(req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("가입 성공 시 PointWallet 0잔액 행 INSERT 검증")
    void signup_creates_empty_point_wallet() {
        when(userRepository.existsByLoginId(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.issueAccess(any(UUID.class), any(Role.class))).thenReturn("acc");
        when(tokenProvider.issueRefresh(any(UUID.class))).thenReturn("ref");

        SignupRequest req = new SignupRequest(
                "newuser4", "Password1!", "010-1234-5678",
                null, "테스트", Role.TUNTUN, null);

        authFacade.signup(req);

        // PointWallet이 저장되었는지 확인
        verify(pointWalletRepository).save(any(PointWallet.class));
    }
}
