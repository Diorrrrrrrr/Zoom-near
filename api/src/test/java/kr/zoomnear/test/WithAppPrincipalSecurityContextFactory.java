package kr.zoomnear.test;

import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * {@link WithAppPrincipal} 어노테이션을 보고 SecurityContext를 구성한다.
 * AppPrincipal을 principal로, ROLE_{role} 권한을 부여한다.
 */
public class WithAppPrincipalSecurityContextFactory
        implements WithSecurityContextFactory<WithAppPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithAppPrincipal annotation) {
        UUID userId = UUID.fromString(annotation.userId());
        AppPrincipal principal = new AppPrincipal(userId, annotation.role());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role().name()))
        );

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        return ctx;
    }
}
