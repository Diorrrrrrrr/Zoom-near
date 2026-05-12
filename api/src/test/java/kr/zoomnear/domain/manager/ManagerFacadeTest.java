package kr.zoomnear.domain.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ManagerFacade 단위 테스트.
 * apply / approve / reject + 중복 신청 / 미존재 ID 케이스 포함.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerFacade — 매니저 신청/승인/거절")
class ManagerFacadeTest {

    @Mock ManagerApplicationRepository repository;
    @Mock UserRepository userRepository;
    @Mock NotificationDispatcher notificationDispatcher;

    ManagerFacade facade;

    @BeforeEach
    void setUp() {
        facade = new ManagerFacade(repository, userRepository, notificationDispatcher);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private ManagerApplication pendingApp(UUID applicantId) {
        return ManagerApplication.builder()
                .id(UUID.randomUUID())
                .applicantId(applicantId)
                .status(ManagerApplicationStatus.PENDING)
                .reason("매니저가 되고 싶습니다.")
                .createdAt(Instant.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // apply
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("apply — TUNTUN 사용자 정상 신청 → PENDING 상태로 저장")
    void apply_tuntun_user_saves_pending_application() {
        // given
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));
        when(repository.findByApplicantIdAndStatus(TestFixtures.USER_TUNTUN_ID, ManagerApplicationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ManagerApplication result = facade.apply(TestFixtures.USER_TUNTUN_ID, "매니저가 되고 싶습니다.");

        // then
        assertThat(result.getStatus()).isEqualTo(ManagerApplicationStatus.PENDING);
        assertThat(result.getApplicantId()).isEqualTo(TestFixtures.USER_TUNTUN_ID);
        assertThat(result.getReason()).isEqualTo("매니저가 되고 싶습니다.");
        verify(repository).save(any(ManagerApplication.class));
    }

    @Test
    @DisplayName("apply — reason null이면 빈 문자열로 저장")
    void apply_null_reason_saved_as_empty_string() {
        // given
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));
        when(repository.findByApplicantIdAndStatus(any(), any())).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ManagerApplication result = facade.apply(TestFixtures.USER_TUNTUN_ID, null);

        // then
        assertThat(result.getReason()).isEqualTo("");
    }

    @Test
    @DisplayName("apply — 중복 신청(PENDING 존재) → CONFLICT")
    void apply_duplicate_pending_throws_conflict() {
        // given
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));
        when(repository.findByApplicantIdAndStatus(TestFixtures.USER_TUNTUN_ID, ManagerApplicationStatus.PENDING))
                .thenReturn(List.of(pendingApp(TestFixtures.USER_TUNTUN_ID)));

        // when / then
        assertThatThrownBy(() -> facade.apply(TestFixtures.USER_TUNTUN_ID, "중복 신청"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("apply — 이미 MANAGER인 사용자 → CONFLICT")
    void apply_already_manager_throws_conflict() {
        // given
        User manager = TestFixtures.managerUser().build();
        when(userRepository.findById(TestFixtures.USER_MANAGER_ID)).thenReturn(Optional.of(manager));

        // when / then
        assertThatThrownBy(() -> facade.apply(TestFixtures.USER_MANAGER_ID, "신청"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("apply — ADMIN 사용자 신청 → CONFLICT")
    void apply_admin_user_throws_conflict() {
        // given
        User admin = TestFixtures.adminUser().build();
        when(userRepository.findById(TestFixtures.USER_ADMIN_ID)).thenReturn(Optional.of(admin));

        // when / then
        assertThatThrownBy(() -> facade.apply(TestFixtures.USER_ADMIN_ID, "신청"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("apply — 존재하지 않는 userId → NOT_FOUND")
    void apply_nonexistent_user_throws_not_found() {
        // given
        UUID nonexistentId = UUID.randomUUID();
        when(userRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> facade.apply(nonexistentId, "신청"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // approve
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve — PENDING 신청 존재 → APPROVED + 신청자 role MANAGER 전환 + 알림")
    void approve_pending_application_promotes_user_to_manager() {
        // given
        ManagerApplication app = pendingApp(TestFixtures.USER_TUNTUN_ID);
        User applicant = TestFixtures.tuntunUser().build();

        when(repository.findById(app.getId())).thenReturn(Optional.of(app));
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(applicant));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ManagerApplication result = facade.approve(app.getId(), TestFixtures.USER_ADMIN_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(ManagerApplicationStatus.APPROVED);
        assertThat(result.getDecidedBy()).isEqualTo(TestFixtures.USER_ADMIN_ID);
        assertThat(applicant.getRole()).isEqualTo(Role.MANAGER);
        verify(notificationDispatcher).notify(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("approve — 미존재 applicationId → NOT_FOUND")
    void approve_nonexistent_application_throws_not_found() {
        // given
        UUID nonexistentId = UUID.randomUUID();
        when(repository.findById(nonexistentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> facade.approve(nonexistentId, TestFixtures.USER_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("approve — 이미 처리된 신청(APPROVED) → CONFLICT")
    void approve_already_decided_application_throws_conflict() {
        // given
        ManagerApplication decided = ManagerApplication.builder()
                .id(UUID.randomUUID())
                .applicantId(TestFixtures.USER_TUNTUN_ID)
                .status(ManagerApplicationStatus.APPROVED)
                .reason("이미 승인됨")
                .createdAt(Instant.now())
                .build();
        when(repository.findById(decided.getId())).thenReturn(Optional.of(decided));

        // when / then
        assertThatThrownBy(() -> facade.approve(decided.getId(), TestFixtures.USER_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reject
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reject — PENDING 신청 → REJECTED + 알림 발송")
    void reject_pending_application_sets_rejected_and_notifies() {
        // given
        ManagerApplication app = pendingApp(TestFixtures.USER_TUNTUN_ID);
        when(repository.findById(app.getId())).thenReturn(Optional.of(app));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ManagerApplication result = facade.reject(app.getId(), TestFixtures.USER_ADMIN_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(ManagerApplicationStatus.REJECTED);
        assertThat(result.getDecidedBy()).isEqualTo(TestFixtures.USER_ADMIN_ID);
        verify(notificationDispatcher).notify(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reject — 미존재 applicationId → NOT_FOUND")
    void reject_nonexistent_application_throws_not_found() {
        // given
        UUID nonexistentId = UUID.randomUUID();
        when(repository.findById(nonexistentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> facade.reject(nonexistentId, TestFixtures.USER_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("reject — 이미 처리된 신청(REJECTED) → CONFLICT")
    void reject_already_decided_application_throws_conflict() {
        // given
        ManagerApplication decided = ManagerApplication.builder()
                .id(UUID.randomUUID())
                .applicantId(TestFixtures.USER_TUNTUN_ID)
                .status(ManagerApplicationStatus.REJECTED)
                .reason("이미 거절됨")
                .createdAt(Instant.now())
                .build();
        when(repository.findById(decided.getId())).thenReturn(Optional.of(decided));

        // when / then
        assertThatThrownBy(() -> facade.reject(decided.getId(), TestFixtures.USER_ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }
}
