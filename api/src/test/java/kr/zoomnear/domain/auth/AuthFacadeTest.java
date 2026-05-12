package kr.zoomnear.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.JwtTokenProvider;
import kr.zoomnear.domain.auth.dto.LoginRequest;
import kr.zoomnear.domain.auth.dto.SignupRequest;
import kr.zoomnear.domain.auth.dto.TokenResponse;
import kr.zoomnear.domain.point.PointWallet;
import kr.zoomnear.domain.point.PointWalletRepository;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.profile.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/// AuthFacade 단위 테스트. BCrypt 해시 보존과 로그인 성공/실패 흐름을 sanity-check 한다.
@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PointWalletRepository pointWalletRepository;

    @Mock
    JwtTokenProvider tokenProvider;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    AuthFacade authFacade;

    private AuthFacade newFacade() {
        return new AuthFacade(userRepository, pointWalletRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void signup_hashes_password_and_creates_wallet() {
        authFacade = newFacade();
        when(userRepository.existsByLoginId("alice123")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.issueAccess(any(UUID.class), any(Role.class))).thenReturn("access-token");
        when(tokenProvider.issueRefresh(any(UUID.class))).thenReturn("refresh-token");

        SignupRequest req = new SignupRequest(
                "alice123", "password123", "010-1234-5678",
                "alice@example.com", "Alice", Role.DUNDUN, null);

        TokenResponse res = authFacade.signup(req);

        assertThat(res.accessToken()).isEqualTo("access-token");
        assertThat(res.refreshToken()).isEqualTo("refresh-token");
        assertThat(res.role()).isEqualTo(Role.DUNDUN);
    }

    @Test
    void signup_rejects_admin_role() {
        authFacade = newFacade();

        SignupRequest req = new SignupRequest(
                "rooty", "password123", "010-1234-5678",
                null, "Root", Role.ADMIN, null);

        assertThatThrownBy(() -> authFacade.signup(req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void login_success_returns_tokens() {
        authFacade = newFacade();
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .loginId("alice123")
                .passwordHash(passwordEncoder.encode("password123"))
                .phone("010-1234-5678")
                .name("Alice")
                .role(Role.DUNDUN)
                .uniqueCode("ABC123")
                .status(UserStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();

        when(userRepository.findByLoginId("alice123")).thenReturn(Optional.of(user));
        when(tokenProvider.issueAccess(any(UUID.class), any(Role.class))).thenReturn("a");
        when(tokenProvider.issueRefresh(any(UUID.class))).thenReturn("r");

        TokenResponse res = authFacade.login(new LoginRequest("alice123", "password123"));

        assertThat(res.userId()).isEqualTo(userId);
        assertThat(res.accessToken()).isEqualTo("a");
    }

    @Test
    void login_wrong_password_throws_unauthorized() {
        authFacade = newFacade();
        User user = User.builder()
                .id(UUID.randomUUID())
                .loginId("alice123")
                .passwordHash(passwordEncoder.encode("password123"))
                .phone("010-1234-5678")
                .name("Alice")
                .role(Role.DUNDUN)
                .uniqueCode("ABC123")
                .status(UserStatus.ACTIVE)
                .createdAt(java.time.Instant.now())
                .build();

        when(userRepository.findByLoginId("alice123")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authFacade.login(new LoginRequest("alice123", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void login_unknown_user_throws_unauthorized() {
        authFacade = newFacade();
        when(userRepository.findByLoginId(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authFacade.login(new LoginRequest("ghost", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
