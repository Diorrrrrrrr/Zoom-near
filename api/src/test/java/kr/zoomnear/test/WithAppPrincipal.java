package kr.zoomnear.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import kr.zoomnear.domain.profile.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * 테스트 메서드에 모의 AppPrincipal을 SecurityContext에 주입하는 어노테이션.
 * 기본값: userId=00000000-0000-0000-0000-000000000001, role=TUNTUN.
 *
 * 사용 예:
 * <pre>
 * {@literal @}Test
 * {@literal @}WithAppPrincipal(role = Role.DUNDUN)
 * void someTest() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = WithAppPrincipalSecurityContextFactory.class)
public @interface WithAppPrincipal {

    String userId() default "00000000-0000-0000-0000-000000000001";

    Role role() default Role.TUNTUN;
}
