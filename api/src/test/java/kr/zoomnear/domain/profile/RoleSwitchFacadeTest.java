package kr.zoomnear.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.linkage.LinkageRepository;
import kr.zoomnear.domain.linkage.LinkageStatus;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleSwitchFacade 단위 테스트.
 * 정상 전환 2가지 + 거부 4가지 = 총 6 케이스.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleSwitchFacade — 역할 전환 도메인")
class RoleSwitchFacadeTest {

    @Mock UserRepository userRepository;
    @Mock LinkageRepository linkageRepository;
    @Mock ApprovalRepository approvalRepository;
    @Mock EventParticipationRepository participationRepository;

    RoleSwitchFacade facade;

    @BeforeEach
    void setUp() {
        facade = new RoleSwitchFacade(
                userRepository, linkageRepository, approvalRepository, participationRepository);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 정상 전환
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DUNDUN → TUNTUN 전환 — 연동·승인·참여 없음 → role이 TUNTUN으로 변경됨")
    void switch_dundun_to_tuntun_succeeds_when_no_conflicts() {
        // given
        User dundun = TestFixtures.dundunUser().build();
        when(userRepository.findById(TestFixtures.USER_DUNDUN_ID)).thenReturn(Optional.of(dundun));
        when(linkageRepository.countActiveAsAnyParty(TestFixtures.USER_DUNDUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(0L);
        when(approvalRepository.countActiveByRequesterOrApprover(
                TestFixtures.USER_DUNDUN_ID, ApprovalStatus.PENDING))
                .thenReturn(0L);
        when(participationRepository.countActiveFutureParticipations(
                any(), any(), any(Instant.class)))
                .thenReturn(0L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        User result = facade.switchRole(TestFixtures.USER_DUNDUN_ID, Role.TUNTUN);

        // then
        assertThat(result.getRole()).isEqualTo(Role.TUNTUN);
        verify(userRepository).save(dundun);
    }

    @Test
    @DisplayName("TUNTUN → DUNDUN 전환 — 연동·승인·참여 없음 → role이 DUNDUN으로 변경됨")
    void switch_tuntun_to_dundun_succeeds_when_no_conflicts() {
        // given
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));
        when(linkageRepository.countActiveAsAnyParty(TestFixtures.USER_TUNTUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(0L);
        when(approvalRepository.countActiveByRequesterOrApprover(
                TestFixtures.USER_TUNTUN_ID, ApprovalStatus.PENDING))
                .thenReturn(0L);
        when(participationRepository.countActiveFutureParticipations(
                any(), any(), any(Instant.class)))
                .thenReturn(0L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        User result = facade.switchRole(TestFixtures.USER_TUNTUN_ID, Role.DUNDUN);

        // then
        assertThat(result.getRole()).isEqualTo(Role.DUNDUN);
        verify(userRepository).save(tuntun);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 거부 케이스
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("거부 — 활성 linkage 존재 → CONFLICT")
    void switch_role_with_active_linkage_throws_conflict() {
        // given
        User dundun = TestFixtures.dundunUser().build();
        when(userRepository.findById(TestFixtures.USER_DUNDUN_ID)).thenReturn(Optional.of(dundun));
        when(linkageRepository.countActiveAsAnyParty(TestFixtures.USER_DUNDUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(2L);

        // when / then
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_DUNDUN_ID, Role.TUNTUN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("거부 — active future participation 존재 → CONFLICT")
    void switch_role_with_active_future_participation_throws_conflict() {
        // given
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));
        when(linkageRepository.countActiveAsAnyParty(TestFixtures.USER_TUNTUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(0L);
        when(approvalRepository.countActiveByRequesterOrApprover(
                TestFixtures.USER_TUNTUN_ID, ApprovalStatus.PENDING))
                .thenReturn(0L);
        when(participationRepository.countActiveFutureParticipations(
                any(), any(), any(Instant.class)))
                .thenReturn(1L);

        // when / then
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_TUNTUN_ID, Role.DUNDUN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("거부 — PENDING approval 존재 → CONFLICT")
    void switch_role_with_pending_approval_throws_conflict() {
        // given
        User dundun = TestFixtures.dundunUser().build();
        when(userRepository.findById(TestFixtures.USER_DUNDUN_ID)).thenReturn(Optional.of(dundun));
        when(linkageRepository.countActiveAsAnyParty(TestFixtures.USER_DUNDUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(0L);
        when(approvalRepository.countActiveByRequesterOrApprover(
                TestFixtures.USER_DUNDUN_ID, ApprovalStatus.PENDING))
                .thenReturn(3L);

        // when / then
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_DUNDUN_ID, Role.TUNTUN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("거부 — MANAGER/ADMIN으로 변경 시도 → FORBIDDEN")
    void switch_role_to_manager_throws_forbidden() {
        // given — newRole이 MANAGER 또는 ADMIN이면 즉시 FORBIDDEN
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));

        // when / then — MANAGER 시도
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_TUNTUN_ID, Role.MANAGER))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("거부 — newRole = ADMIN → FORBIDDEN")
    void switch_role_to_admin_throws_forbidden() {
        // given
        User dundun = TestFixtures.dundunUser().build();
        when(userRepository.findById(TestFixtures.USER_DUNDUN_ID)).thenReturn(Optional.of(dundun));

        // when / then
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_DUNDUN_ID, Role.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("거부 — 현재 MANAGER인 사용자가 전환 시도 → FORBIDDEN")
    void switch_role_current_manager_throws_forbidden() {
        // given
        User manager = TestFixtures.managerUser().build();
        when(userRepository.findById(TestFixtures.USER_MANAGER_ID)).thenReturn(Optional.of(manager));

        // when / then — MANAGER는 전환 대상이 아님
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_MANAGER_ID, Role.TUNTUN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("거부 — 이미 동일한 역할 → CONFLICT")
    void switch_role_same_role_throws_conflict() {
        // given
        User tuntun = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(tuntun));

        // when / then
        assertThatThrownBy(() -> facade.switchRole(TestFixtures.USER_TUNTUN_ID, Role.TUNTUN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CONFLICT);
    }
}
