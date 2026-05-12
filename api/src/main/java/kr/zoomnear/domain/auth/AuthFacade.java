package kr.zoomnear.domain.auth;

import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import kr.zoomnear.domain.auth.dto.LoginRequest;
import kr.zoomnear.domain.auth.dto.SignupRequest;
import kr.zoomnear.domain.auth.dto.TokenResponse;
import kr.zoomnear.domain.invite.InviteFacade;
import kr.zoomnear.domain.point.PointWallet;
import kr.zoomnear.domain.point.PointWalletRepository;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.UniqueCodeGenerator;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.profile.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 회원가입·로그인 등 인증 흐름을 조율하는 도메인 파사드.
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private static final int CODE_MAX_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final InviteFacade inviteFacade;

    /// 회원가입을 수행한다.
    /// 1) 가입 정책 검증 (role, loginId 중복)
    /// 2) Crockford Base32 6자리 채번 후 User INSERT
    /// 3) PointWallet 0 잔액 행 INSERT
    /// 4) inviteToken이 있으면 InviteFacade.consume으로 자동 연동
    /// 5) Access/Refresh 토큰 발급
    @Transactional
    @AuditLog("AUTH_SIGNUP")
    public TokenResponse signup(SignupRequest req) {
        if (req.role() == Role.ADMIN || req.role() == Role.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "ADMIN/MANAGER 계정은 가입할 수 없습니다.");
        }
        if (userRepository.existsByLoginId(req.loginId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 loginId 입니다.");
        }

        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .loginId(req.loginId())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .email(req.email())
                .name(req.name())
                .role(req.role())
                .uniqueCode(nextAvailableCode())
                .status(UserStatus.ACTIVE)
                .rankCode("PPOJJAK")
                .createdAt(now)
                .build();
        userRepository.saveAndFlush(user);

        pointWalletRepository.save(PointWallet.emptyFor(userId, now));

        if (req.inviteToken() != null && !req.inviteToken().isBlank() && req.role() == Role.TUNTUN) {
            try {
                inviteFacade.consume(UUID.fromString(req.inviteToken().trim()), userId);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "초대 토큰 형식이 올바르지 않습니다.");
            }
        }

        return issueTokens(user);
    }

    /// 32^6 공간에서 미사용 코드 1건을 채번. 충돌 시 재시도.
    private String nextAvailableCode() {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            String code = UniqueCodeGenerator.generate();
            if (!userRepository.existsByUniqueCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL, "고유 코드 채번에 실패했습니다.");
    }

    /// 로그인. loginId + 비밀번호 검증 후 토큰 발급.
    @Transactional(readOnly = true)
    @AuditLog("AUTH_LOGIN")
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "loginId 또는 비밀번호가 올바르지 않습니다."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "비활성 계정입니다.");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "loginId 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = tokenProvider.issueAccess(user.getId(), user.getRole());
        String refresh = tokenProvider.issueRefresh(user.getId());
        return new TokenResponse(access, refresh, user.getId(), user.getRole());
    }

    /// 리프레시 토큰으로 새 access+refresh 토큰을 발급한다 (MVP — whitelist 미사용).
    @Transactional(readOnly = true)
    @AuditLog("AUTH_REFRESH")
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refreshToken 이 비어있습니다.");
        }
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 refreshToken 입니다.");
        }
        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 refreshToken 입니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "비활성 계정입니다.");
        }
        return issueTokens(user);
    }
}
