package kr.zoomnear.domain.event;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EventParticipation 동시성 테스트.
 *
 * 목표: 50개 스레드가 capacity=10인 이벤트에 동시 join 시도 →
 *       정확히 10건만 CONFIRMED, 나머지 40건은 EVENT_FULL 검증.
 *
 * TODO(Day2-LaneQ): @SpringBootTest + Testcontainers Postgres로 구현 예정.
 *   - Testcontainers 의존성 build.gradle.kts에 추가 필요:
 *     testImplementation("org.testcontainers:junit-jupiter:1.20.0")
 *     testImplementation("org.testcontainers:postgresql:1.20.0")
 *   - EventParticipationFacade.join() 완성 필요 (Lane B PM)
 *   - SocialEvent.findWithLockById(@Lock PESSIMISTIC_WRITE) 검증 포함
 *
 * 현재 Phase 1: 구조 골격만 작성, @Disabled로 스킵.
 */
@DisplayName("EventParticipation — 동시성 (capacity race)")
class EventParticipationConcurrencyTest {

    @Test
    @Disabled("TODO(Day2-LaneQ): Testcontainers Postgres + EventParticipationFacade 완성 후 활성화")
    @DisplayName("50 스레드 동시 join, capacity=10 → 정확히 10건만 CONFIRMED")
    void concurrent_50_joins_capacity_10_exactly_10_confirmed() throws InterruptedException {
        // TODO(Day2-LaneQ): 아래 구현 완성
        //
        // @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
        // @Testcontainers
        // class EventParticipationConcurrencyTest {
        //
        //     @Container
        //     static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        //             .withDatabaseName("zoomnear_test")
        //             .withUsername("test")
        //             .withPassword("test");
        //
        //     @DynamicPropertySource
        //     static void registerPostgres(DynamicPropertyRegistry registry) {
        //         registry.add("spring.datasource.url", postgres::getJdbcUrl);
        //         registry.add("spring.datasource.username", postgres::getUsername);
        //         registry.add("spring.datasource.password", postgres::getPassword);
        //     }
        //
        //     @Autowired EventParticipationFacade facade;
        //     @Autowired SocialEventRepository eventRepository;
        //     @Autowired EventParticipationRepository participationRepository;
        //     @Autowired UserRepository userRepository;
        //     @Autowired PointWalletRepository walletRepository;
        //
        //     @Test
        //     void concurrent_50_joins_exactly_10_confirmed() throws InterruptedException {
        //         // 1. capacity=10 이벤트 생성
        //         SocialEvent event = /* ... */;
        //
        //         // 2. 50명 TUNTUN + 각 10000 포인트 지갑 생성
        //         List<UUID> userIds = /* ... */;
        //
        //         // 3. 50 스레드 동시 join
        //         int threadCount = 50;
        //         ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        //         CountDownLatch latch = new CountDownLatch(threadCount);
        //         AtomicInteger confirmed = new AtomicInteger(0);
        //         AtomicInteger eventFull = new AtomicInteger(0);
        //
        //         for (int i = 0; i < threadCount; i++) {
        //             UUID userId = userIds.get(i);
        //             executor.submit(() -> {
        //                 try {
        //                     facade.join(userId, event.getId());
        //                     confirmed.incrementAndGet();
        //                 } catch (BusinessException e) {
        //                     if (e.getCode() == ErrorCode.EVENT_FULL) {
        //                         eventFull.incrementAndGet();
        //                     }
        //                 } finally {
        //                     latch.countDown();
        //                 }
        //             });
        //         }
        //
        //         latch.await(30, TimeUnit.SECONDS);
        //         executor.shutdown();
        //
        //         // 4. 검증
        //         assertThat(confirmed.get()).isEqualTo(10);
        //         assertThat(eventFull.get()).isEqualTo(40);
        //
        //         // 5. DB 직접 검증
        //         long dbConfirmed = participationRepository
        //                 .countByEventIdAndStatusIn(event.getId(), List.of(ParticipationStatus.CONFIRMED));
        //         assertThat(dbConfirmed).isEqualTo(10);
        //     }
        // }
    }
}
