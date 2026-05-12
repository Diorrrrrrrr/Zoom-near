package kr.zoomnear.domain.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.event.EventCategory;
import kr.zoomnear.domain.event.EventParticipation;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.EventVisibility;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.event.SocialEvent;
import kr.zoomnear.domain.event.SocialEventRepository;
import kr.zoomnear.domain.point.PointReason;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.profile.UserStatus;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AdminFacade 단위 테스트.
 * suspend/activate + forceCloseEvent(모든 confirmed 환불 흐름) 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminFacade — 관리자 도메인 파사드")
class AdminFacadeTest {

    @Mock UserRepository userRepository;
    @Mock SocialEventRepository eventRepository;
    @Mock EventParticipationRepository participationRepository;
    @Mock PointService pointService;
    @Mock NotificationDispatcher notificationDispatcher;

    AdminFacade facade;

    @BeforeEach
    void setUp() {
        facade = new AdminFacade(
                userRepository, eventRepository, participationRepository,
                pointService, notificationDispatcher);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private SocialEvent openEvent(BigDecimal pointCost) {
        return SocialEvent.builder()
                .id(TestFixtures.EVENT_ID)
                .creatorId(TestFixtures.USER_MANAGER_ID)
                .category(EventCategory.SOCIAL)
                .title("테스트 이벤트")
                .description("설명")
                .regionText("서울")
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .capacity(10)
                .pointCost(pointCost)
                .status(EventStatus.OPEN)
                .visibility(EventVisibility.PUBLIC)
                .managerProgram(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private EventParticipation confirmedParticipation(UUID participantId) {
        return EventParticipation.builder()
                .id(UUID.randomUUID())
                .eventId(TestFixtures.EVENT_ID)
                .participantId(participantId)
                .status(ParticipationStatus.CONFIRMED)
                .joinedAt(Instant.now())
                .build();
    }

    private EventParticipation pendingParticipation(UUID participantId) {
        return EventParticipation.builder()
                .id(UUID.randomUUID())
                .eventId(TestFixtures.EVENT_ID)
                .participantId(participantId)
                .status(ParticipationStatus.PENDING_APPROVAL)
                .joinedAt(Instant.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // suspendUser
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("suspendUser — 존재하는 사용자 → status SUSPENDED 저장 + 알림")
    void suspend_existing_user_sets_suspended_and_notifies() {
        // given
        User user = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        facade.suspendUser(TestFixtures.USER_TUNTUN_ID, "규정 위반");

        // then
        verify(userRepository).save(user);
        org.assertj.core.api.Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(notificationDispatcher).notify(
                eq(TestFixtures.USER_TUNTUN_ID), any(), any(), any(), any());
    }

    @Test
    @DisplayName("suspendUser — 미존재 사용자 → NOT_FOUND")
    void suspend_nonexistent_user_throws_not_found() {
        // given
        UUID nonexistentId = UUID.randomUUID();
        when(userRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> facade.suspendUser(nonexistentId, "사유"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("suspendUser — reason null → 기본 메시지로 알림")
    void suspend_user_null_reason_still_notifies() {
        // given
        User user = TestFixtures.tuntunUser().build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        facade.suspendUser(TestFixtures.USER_TUNTUN_ID, null);

        // then
        verify(notificationDispatcher).notify(
                eq(TestFixtures.USER_TUNTUN_ID), any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // activateUser
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("activateUser — SUSPENDED 사용자 → status ACTIVE 저장 + 알림")
    void activate_suspended_user_sets_active_and_notifies() {
        // given
        User user = TestFixtures.tuntunUser().status(UserStatus.SUSPENDED).build();
        when(userRepository.findById(TestFixtures.USER_TUNTUN_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        facade.activateUser(TestFixtures.USER_TUNTUN_ID);

        // then
        verify(userRepository).save(user);
        org.assertj.core.api.Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(notificationDispatcher).notify(
                eq(TestFixtures.USER_TUNTUN_ID), any(), any(), any(), any());
    }

    @Test
    @DisplayName("activateUser — 미존재 사용자 → NOT_FOUND")
    void activate_nonexistent_user_throws_not_found() {
        // given
        UUID nonexistentId = UUID.randomUUID();
        when(userRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> facade.activateUser(nonexistentId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // forceCloseEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("forceCloseEvent — confirmed 참여 2명 → 이벤트 CANCELED + 각 참여자 100% 환불 + 알림 2건")
    void force_close_event_refunds_all_confirmed_participants() {
        // given
        SocialEvent event = openEvent(BigDecimal.valueOf(5000));
        EventParticipation p1 = confirmedParticipation(TestFixtures.USER_TUNTUN_ID);
        EventParticipation p2 = confirmedParticipation(TestFixtures.USER_OTHER_ID);

        when(eventRepository.findById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participationRepository.findByEventIdAndStatusIn(eq(TestFixtures.EVENT_ID), any()))
                .thenReturn(List.of(p1, p2));
        when(participationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        facade.forceCloseEvent(TestFixtures.EVENT_ID, "운영상 사유");

        // then
        org.assertj.core.api.Assertions.assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELED);
        // 각 confirmed 참여자에게 환불
        verify(pointService).credit(
                eq(TestFixtures.USER_TUNTUN_ID), eq(BigDecimal.valueOf(5000)),
                eq(PointReason.EVENT_REFUND), any(), any());
        verify(pointService).credit(
                eq(TestFixtures.USER_OTHER_ID), eq(BigDecimal.valueOf(5000)),
                eq(PointReason.EVENT_REFUND), any(), any());
        // 알림 2건
        verify(notificationDispatcher, times(2)).notify(any(), any(), any(), any(), any());
        // 참여 상태 CANCELED
        org.assertj.core.api.Assertions.assertThat(p1.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
        org.assertj.core.api.Assertions.assertThat(p2.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
    }

    @Test
    @DisplayName("forceCloseEvent — PENDING_APPROVAL 참여 → 환불 없이 CANCELED (미확정 참여는 환불 대상 아님)")
    void force_close_event_no_refund_for_pending_approval_participants() {
        // given
        SocialEvent event = openEvent(BigDecimal.valueOf(5000));
        EventParticipation pending = pendingParticipation(TestFixtures.USER_TUNTUN_ID);

        when(eventRepository.findById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participationRepository.findByEventIdAndStatusIn(eq(TestFixtures.EVENT_ID), any()))
                .thenReturn(List.of(pending));
        when(participationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        facade.forceCloseEvent(TestFixtures.EVENT_ID, null);

        // then — PENDING_APPROVAL은 환불 대상이 아님
        verify(pointService, never()).credit(any(), any(), any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(pending.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
    }

    @Test
    @DisplayName("forceCloseEvent — 무료 이벤트(pointCost=0) → 참여 CANCELED, 환불 없음")
    void force_close_free_event_no_refund() {
        // given
        SocialEvent freeEvent = openEvent(BigDecimal.ZERO);
        EventParticipation confirmed = confirmedParticipation(TestFixtures.USER_TUNTUN_ID);

        when(eventRepository.findById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(freeEvent));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participationRepository.findByEventIdAndStatusIn(eq(TestFixtures.EVENT_ID), any()))
                .thenReturn(List.of(confirmed));
        when(participationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        facade.forceCloseEvent(TestFixtures.EVENT_ID, null);

        // then
        verify(pointService, never()).credit(any(), any(), any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(confirmed.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
    }

    @Test
    @DisplayName("forceCloseEvent — 참여자 없는 이벤트 → 이벤트만 CANCELED")
    void force_close_event_no_participants_only_event_canceled() {
        // given
        SocialEvent event = openEvent(BigDecimal.valueOf(3000));

        when(eventRepository.findById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participationRepository.findByEventIdAndStatusIn(eq(TestFixtures.EVENT_ID), any()))
                .thenReturn(List.of());

        // when
        facade.forceCloseEvent(TestFixtures.EVENT_ID, "시스템 오류");

        // then
        org.assertj.core.api.Assertions.assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELED);
        verify(pointService, never()).credit(any(), any(), any(), any(), any());
        verify(notificationDispatcher, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("forceCloseEvent — 미존재 이벤트 → NOT_FOUND")
    void force_close_nonexistent_event_throws_not_found() {
        // given
        UUID nonexistentId = UUID.randomUUID();
        when(eventRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> facade.forceCloseEvent(nonexistentId, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND);
    }
}
