package kr.zoomnear.common.security;

import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.profile.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/// 현재 인증된 사용자의 식별 토큰 페이로드.
/// JwtAuthenticationFilter가 SecurityContext에 주입한다.
public record AppPrincipal(UUID userId, Role role) {

    /// 현재 SecurityContext에서 AppPrincipal을 꺼낸다. 인증되지 않은 경우 UNAUTHORIZED.
    public static AppPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppPrincipal p)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return p;
    }
}
