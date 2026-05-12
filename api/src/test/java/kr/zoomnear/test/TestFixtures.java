package kr.zoomnear.test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserStatus;

/**
 * 테스트 전반에서 사용하는 UUID 상수와 사용자 시드 헬퍼.
 * 모든 상수는 nil UUID 계열로 범위가 0001~00FF.
 */
public final class TestFixtures {

    // ── 사용자 ID 상수 ─────────────────────────────────────────
    public static final UUID USER_TUNTUN_ID  = uuid("0001");
    public static final UUID USER_DUNDUN_ID  = uuid("0002");
    public static final UUID USER_MANAGER_ID = uuid("0003");
    public static final UUID USER_ADMIN_ID   = uuid("0004");
    public static final UUID USER_OTHER_ID   = uuid("0005");

    // ── 이벤트 ID 상수 ─────────────────────────────────────────
    public static final UUID EVENT_ID        = uuid("0010");
    public static final UUID EVENT_FULL_ID   = uuid("0011");

    // ── 연동 ID 상수 ─────────────────────────────────────────
    public static final UUID LINKAGE_ID      = uuid("0020");

    // ── 승인 ID 상수 ─────────────────────────────────────────
    public static final UUID APPROVAL_ID     = uuid("0030");

    // ── 초대 토큰 상수 ────────────────────────────────────────
    public static final String INVITE_TOKEN  = "INVITE-TEST-TOKEN-0001";

    // ── 기본 포인트 ───────────────────────────────────────────
    public static final BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(10_000);

    private TestFixtures() {}

    private static UUID uuid(String suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + suffix.repeat(12 / suffix.length() + 1).substring(0, 12));
    }

    /** TUNTUN 역할의 기본 사용자 빌더 */
    public static User.UserBuilder tuntunUser() {
        return User.builder()
                .id(USER_TUNTUN_ID)
                .loginId("tuntun01")
                .passwordHash("$2a$04$hashedpassword")
                .phone("010-1111-1111")
                .email("tuntun@example.com")
                .name("튼튼이")
                .role(Role.TUNTUN)
                .uniqueCode("TUN001")
                .status(UserStatus.ACTIVE)
                .rankCode("PPOJJAK")
                .createdAt(Instant.now());
    }

    /** DUNDUN 역할의 기본 사용자 빌더 */
    public static User.UserBuilder dundunUser() {
        return User.builder()
                .id(USER_DUNDUN_ID)
                .loginId("dundun01")
                .passwordHash("$2a$04$hashedpassword")
                .phone("010-2222-2222")
                .email("dundun@example.com")
                .name("든든이")
                .role(Role.DUNDUN)
                .uniqueCode("DUN001")
                .status(UserStatus.ACTIVE)
                .rankCode("PPOJJAK")
                .createdAt(Instant.now());
    }

    /** MANAGER 역할의 기본 사용자 빌더 */
    public static User.UserBuilder managerUser() {
        return User.builder()
                .id(USER_MANAGER_ID)
                .loginId("manager01")
                .passwordHash("$2a$04$hashedpassword")
                .phone("010-3333-3333")
                .name("매니저")
                .role(Role.MANAGER)
                .uniqueCode("MGR001")
                .status(UserStatus.ACTIVE)
                .rankCode("PPOJJAK")
                .createdAt(Instant.now());
    }

    /** ADMIN 역할의 기본 사용자 빌더 */
    public static User.UserBuilder adminUser() {
        return User.builder()
                .id(USER_ADMIN_ID)
                .loginId("admin01")
                .passwordHash("$2a$04$hashedpassword")
                .phone("010-4444-4444")
                .name("관리자")
                .role(Role.ADMIN)
                .uniqueCode("ADM001")
                .status(UserStatus.ACTIVE)
                .rankCode("PPOJJAK")
                .createdAt(Instant.now());
    }
}
