package kr.zoomnear.common.seed;

import java.time.Instant;
import java.util.UUID;
import kr.zoomnear.domain.point.PointWallet;
import kr.zoomnear.domain.point.PointWalletRepository;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.UniqueCodeGenerator;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.profile.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/// 로컬/데모 환경 한정 ADMIN 계정 자동 시드.
/// 운영 환경(local 외 프로필)에서는 빈 등록 자체가 안 된다.
@Slf4j
@Component
@Profile({"local", "supabase"})
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    private static final int CODE_MAX_ATTEMPTS = 10;

    private record SeedAccount(String loginId, String password, String name, String phone, Role role) {}

    private static final SeedAccount[] SEED_ACCOUNTS = new SeedAccount[]{
            new SeedAccount("admin09", "admin0909", "관리자", "010-0000-0000", Role.ADMIN),
            new SeedAccount("manager1", "manager0101", "매니저", "010-0000-0001", Role.MANAGER),
    };

    private final UserRepository userRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (SeedAccount seed : SEED_ACCOUNTS) {
            seedAccount(seed);
        }
    }

    private void seedAccount(SeedAccount seed) {
        if (userRepository.existsByLoginId(seed.loginId())) {
            log.info("[SEED] {} '{}' already exists. Skip.", seed.role(), seed.loginId());
            return;
        }
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        String code = nextAvailableCode();

        User user = User.builder()
                .id(userId)
                .loginId(seed.loginId())
                .passwordHash(passwordEncoder.encode(seed.password()))
                .phone(seed.phone())
                .email(null)
                .name(seed.name())
                .role(seed.role())
                .uniqueCode(code)
                .status(UserStatus.ACTIVE)
                .rankCode("PPOJJAK")
                .createdAt(now)
                .build();
        userRepository.saveAndFlush(user);

        pointWalletRepository.save(PointWallet.emptyFor(userId, now));

        log.warn("[SEED] {} account created: loginId={} uniqueCode={}",
                seed.role(), seed.loginId(), code);
    }

    private String nextAvailableCode() {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            String code = UniqueCodeGenerator.generate();
            if (!userRepository.existsByUniqueCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("고유 코드 채번에 실패했습니다.");
    }
}
