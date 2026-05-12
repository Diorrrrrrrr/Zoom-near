# Spring MockMvc 권한 매트릭스 단위 테스트 템플릿

> Day 1 PM에 Lane Q가 이 템플릿을 기반으로 실제 Controller 테스트 코드를 작성합니다.
> 패키지 경로: `api/src/test/java/kr/zoomnear/`

---

## 의존성 확인

```kotlin
// api/build.gradle.kts (이미 존재하면 스킵)
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // Testcontainers (Day 1 PM 통합 테스트용)
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

---

## 베이스 테스트 클래스

```java
// api/src/test/java/kr/zoomnear/common/BaseControllerTest.java
package kr.zoomnear.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller 슬라이스 테스트 공통 베이스.
 * @SpringBootTest 대신 @WebMvcTest로 교체 가능 (레이어 분리 시).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;
}
```

---

## AppPrincipal 주입 헬퍼

```java
// api/src/test/java/kr/zoomnear/common/WithAppPrincipalSecurityContextFactory.java
package kr.zoomnear.common;

import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.profile.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithAppPrincipalSecurityContextFactory
        implements WithSecurityContextFactory<WithAppPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithAppPrincipal annotation) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        UUID userId = annotation.userId().isBlank()
                ? UUID.randomUUID()
                : UUID.fromString(annotation.userId());
        Role role = Role.valueOf(annotation.role());
        AppPrincipal principal = new AppPrincipal(userId, role);
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        ));
        return ctx;
    }
}
```

```java
// api/src/test/java/kr/zoomnear/common/WithAppPrincipal.java
package kr.zoomnear.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * 테스트 메서드에 AppPrincipal을 SecurityContext에 주입하는 커스텀 어노테이션.
 *
 * 사용 예:
 *   @WithAppPrincipal(role = "TUNTUN")
 *   @WithAppPrincipal(role = "DUNDUN", userId = "550e8400-e29b-41d4-a716-446655440000")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithAppPrincipalSecurityContextFactory.class)
public @interface WithAppPrincipal {

    /** 역할: TUNTUN | DUNDUN | MANAGER | ADMIN */
    String role();

    /** UUID 문자열. 비어 있으면 랜덤 UUID 생성. */
    String userId() default "";
}
```

---

## 권한 매트릭스 단위 테스트 템플릿

```java
// api/src/test/java/kr/zoomnear/permission/PermissionMatrixTest.java
package kr.zoomnear.permission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import kr.zoomnear.common.BaseControllerTest;
import kr.zoomnear.common.WithAppPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 권한 매트릭스 시나리오 — permission_matrix.md 기반.
 * TODO(Day1PM): 각 TODO 주석을 실제 Controller 경로·Mock Service로 대체.
 */
class PermissionMatrixTest extends BaseControllerTest {

    // ── AUTH ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AUTH — 인증 시나리오")
    class Auth {

        @Test
        @DisplayName("#4 잘못된 비밀번호로 로그인하면 401을 반환한다")
        void loginWithWrongPassword_returns401() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of("loginId", "nobody", "password", "wrong")
            );
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        // TODO(Day1PM): #1 정상 TUNTUN 가입 → 201 테스트
        // TODO(Day1PM): #2 role=ADMIN 가입 시도 → 400 테스트
        // TODO(Day1PM): #3 정상 로그인 → 200 + token 반환 테스트
        // TODO(Day1PM): #5 로그아웃 → refresh_token 무효화 테스트
    }

    // ── PROFILE ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PROFILE — 프로필 조회·수정")
    class Profile {

        @Test
        @DisplayName("#6 TUNTUN은 본인 프로필을 조회할 수 있다")
        @WithAppPrincipal(role = "TUNTUN")
        void tuntunGetOwnProfile_returns200() throws Exception {
            mvc.perform(get("/api/v1/profile/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#8 TUNTUN이 타인 프로필을 조회하면 403을 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void tuntunGetOtherProfile_returns403() throws Exception {
            mvc.perform(get("/api/v1/profile/{id}", "other-user-id"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#9 ADMIN은 임의 사용자 프로필을 조회할 수 있다")
        @WithAppPrincipal(role = "ADMIN")
        void adminGetAnyProfile_returns200() throws Exception {
            // TODO(Day1PM): 실제 사용자 ID fixture 주입
            mvc.perform(get("/api/v1/admin/users/{id}", "some-user-uuid"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#10 비인증 요청은 401을 반환한다")
        void anonGetProfile_returns401() throws Exception {
            mvc.perform(get("/api/v1/profile/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── LINKAGE ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("LINKAGE — 연동 시나리오")
    class Linkage {

        @Test
        @DisplayName("#16 DUNDUN이 본인 ID로 연동 시도하면 400을 반환한다")
        @WithAppPrincipal(role = "DUNDUN", userId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        void dundunSelfLink_returns400() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of("tuntunId", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
            );
            mvc.perform(post("/api/v1/linkage")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#17 TUNTUN이 연동 요청을 시도하면 403을 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void tuntunInitiateLinkage_returns403() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of("tuntunId", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
            );
            mvc.perform(post("/api/v1/linkage")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        // TODO(Day1PM): #14 4명 한도 초과 → 409 테스트 (LinkageService Mock 필요)
        // TODO(Day1PM): #15 중복 연동 → 409 테스트
    }

    // ── EVENT_PARTICIPATE ─────────────────────────────────────────────────

    @Nested
    @DisplayName("EVENT_PARTICIPATE — 이벤트 참여")
    class EventParticipate {

        @Test
        @DisplayName("#32 잔액 부족 시 참여하면 409를 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void joinWithInsufficientPoints_returns409() throws Exception {
            // TODO(Day1PM): PointWalletService Mock — balance=0, fee=3000
            mvc.perform(post("/api/v1/events/{id}/join", "event-uuid"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#33 이미 참여한 이벤트에 재참여하면 409를 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void joinDuplicateEvent_returns409() throws Exception {
            // TODO(Day1PM): EventParticipantRepository Mock — 이미 row 존재
            mvc.perform(post("/api/v1/events/{id}/join", "event-uuid"))
                    .andExpect(status().isConflict());
        }
    }

    // ── PROXY_PARTICIPATE ─────────────────────────────────────────────────

    @Nested
    @DisplayName("PROXY_PARTICIPATE — 든든이 대리 참여")
    class ProxyParticipate {

        @Test
        @DisplayName("#36 비연동 DUNDUN이 대리 참여 시도하면 403을 반환한다")
        @WithAppPrincipal(role = "DUNDUN")
        void unlinkDundunProxy_returns403() throws Exception {
            // TODO(Day1PM): LinkageGuard Mock — assertLinked throws NOT_LINKED
            String body = om.writeValueAsString(
                    java.util.Map.of("tuntunId", "cccccccc-cccc-cccc-cccc-cccccccccccc")
            );
            mvc.perform(post("/api/v1/proxy/events/{id}/join", "event-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#39 타인의 approval을 승인 시도하면 403을 반환한다")
        @WithAppPrincipal(role = "TUNTUN",
                userId = "dddddddd-dddd-dddd-dddd-dddddddddddd")
        void approveOtherTuntunApproval_returns403() throws Exception {
            // TODO(Day1PM): ApprovalService Mock — approval.tuntunId != current userId
            mvc.perform(post("/api/v1/approvals/{id}/approve", "approval-uuid"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── POINT_TOPUP ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POINT_TOPUP — 포인트 충전")
    class PointTopup {

        @Test
        @DisplayName("#44 음수 amount로 충전 시도하면 400을 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void topupNegativeAmount_returns400() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of("amount", -1, "referenceId", "ref-neg")
            );
            mvc.perform(post("/api/v1/points/topup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#45 amount=0으로 충전 시도하면 400을 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void topupZeroAmount_returns400() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of("amount", 0, "referenceId", "ref-zero")
            );
            mvc.perform(post("/api/v1/points/topup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── MANAGER ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MANAGER — 매니저 권한")
    class Manager {

        @Test
        @DisplayName("#47 MANAGER가 타 매니저 이벤트 수정 시 403을 반환한다")
        @WithAppPrincipal(role = "MANAGER")
        void managerUpdateOtherManagerEvent_returns403() throws Exception {
            // TODO(Day1PM): EventService Mock — event.creatorId != current userId
            String body = om.writeValueAsString(
                    java.util.Map.of("title", "수정 시도")
            );
            mvc.perform(patch("/api/v1/events/{id}", "other-manager-event-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#48 TUNTUN이 is_manager_program=true 이벤트 등록 시 403을 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void tuntunCreateManagerProgram_returns403() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of(
                            "title", "테스트", "is_manager_program", true,
                            "capacity", 10, "fee", 0
                    )
            );
            mvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ADMIN — 관리자 전용")
    class Admin {

        @Test
        @DisplayName("#49 ADMIN은 회원을 강제 정지할 수 있다")
        @WithAppPrincipal(role = "ADMIN")
        void adminSuspendUser_returns200() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of("status", "SUSPENDED")
            );
            // TODO(Day1PM): UserService Mock — suspendUser 호출 검증
            mvc.perform(patch("/api/v1/admin/users/{id}/status", "target-user-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("비ADMIN이 admin 엔드포인트 접근 시 403을 반환한다")
        @WithAppPrincipal(role = "TUNTUN")
        void nonAdminAccessAdminEndpoint_returns403() throws Exception {
            mvc.perform(get("/api/v1/admin/audit-logs"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── EDGE ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EDGE — 경계/보안 케이스")
    class Edge {

        @Test
        @DisplayName("#55 변조된 JWT로 요청하면 401을 반환한다")
        void tamperedJwt_returns401() throws Exception {
            mvc.perform(get("/api/v1/profile/me")
                            .header("Authorization", "Bearer tampered.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("#56 만료된 JWT로 요청하면 401을 반환한다")
        void expiredJwt_returns401() throws Exception {
            // TODO(Day1PM): JwtTokenProvider에서 만료 토큰 생성 유틸 추가
            String expiredToken = "TODO_REPLACE_WITH_EXPIRED_JWT";
            mvc.perform(get("/api/v1/profile/me")
                            .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("#57 SQL injection 입력값은 400 또는 이스케이프 처리된다")
        @WithAppPrincipal(role = "TUNTUN")
        void sqlInjectionInput_returns400OrEscaped() throws Exception {
            String body = om.writeValueAsString(
                    java.util.Map.of(
                            "title", "' OR 1=1--",
                            "capacity", 10,
                            "fee", 0
                    )
            );
            mvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    // 400(검증 실패) 또는 201(저장은 되되 이스케이프됨)
                    // JPA/Hibernate 파라미터 바인딩으로 SQL injection 자체는 차단됨
                    .andExpect(result ->
                            org.junit.jupiter.api.Assertions.assertTrue(
                                    result.getResponse().getStatus() == 400
                                    || result.getResponse().getStatus() == 201,
                                    "SQL injection은 400 또는 이스케이프 처리되어야 합니다"
                            ));
        }
    }
}
```

---

## TODO 체크리스트 (Day 1 PM — Lane Q)

- [ ] `BaseControllerTest` 파일 생성 (`api/src/test/java/kr/zoomnear/common/`)
- [ ] `WithAppPrincipal` + `WithAppPrincipalSecurityContextFactory` 생성
- [ ] `SecurityConfig` 에서 role 기반 접근 규칙 확인 후 테스트 경로 조정
- [ ] 각 `TODO(Day1PM)` 주석 → 실제 Service Mock(`@MockBean`) 연결
- [ ] `LinkageGuard` stub → Mockito stub으로 교체
- [ ] Testcontainers PostgreSQL 설정 추가 (통합 테스트 전환 시)
- [ ] `./gradlew :api:test --tests "*.PermissionMatrixTest"` 실행하여 전체 GREEN 확인
