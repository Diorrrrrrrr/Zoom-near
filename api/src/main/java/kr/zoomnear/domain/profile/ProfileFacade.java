package kr.zoomnear.domain.profile;

import java.math.BigDecimal;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.dto.ChangePasswordRequest;
import kr.zoomnear.domain.profile.dto.MeResponse;
import kr.zoomnear.domain.rank.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 사용자 프로필 도메인 파사드. 본인 프로필 조회와 비밀번호 변경을 담당.
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileFacade {

    private final UserRepository userRepository;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;
    private final RankService rankService;

    /// 본인 프로필 + 잔액 + 랭크 코드/표시명 반환.
    /// 랭크는 users.rank_code 컬럼에서 직접 읽고, 표시명은 ranks 테이블에서 매핑한다.
    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        BigDecimal balance = pointService.getBalance(userId);
        String rankCode = user.getRankCode();
        String rankDisplayName = rankService.displayName(rankCode);
        return new MeResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                user.getUniqueCode(),
                balance,
                rankCode,
                rankDisplayName);
    }

    /// 비밀번호 변경. 현재 비밀번호 매칭 + BCrypt 재해시.
    @Transactional
    @AuditLog("PROFILE_CHANGE_PASSWORD")
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }
}
