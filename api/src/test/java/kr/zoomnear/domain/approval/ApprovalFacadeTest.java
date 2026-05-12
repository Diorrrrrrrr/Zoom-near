package kr.zoomnear.domain.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
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
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ApprovalFacade 단위 테스트.
 * 4종 타입(EVENT_JOIN, EVENT_CANCEL, EVENT_CREATE) × 3 결과(approve/reject/expire) = 12 케이스.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalFacade — 4종 승인 워크플로우")
class ApprovalFacadeTest {

    @Mock ApprovalRepository approvalRepository;
    @Mock EventParticipationRepository participationRepository;
    @Mock SocialEventRepository eventRepository;
    @Mock PointService pointService;
    @Mock NotificationDispatcher notificationDispatcher;

    ApprovalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new ApprovalFacade(
                approvalRepository, participationRepository,
                eventRepository, pointService, notificationDispatcher);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────────────

    private Approval pendingApproval(ApprovalType type) {
        return Approval.builder()
                .id(TestFixtures.APPROVAL_ID)
                .type(type)
                .requesterId(TestFixtures.USER_DUNDUN_ID)
                .approverId(TestFixtures.USER_TUNTUN_ID)
                .status(ApprovalStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .payload(new HashMap<>())
                .build();
    }

    private Approval pendingApprovalWithPayload(ApprovalType type, Map<String, Object> payload) {
        return Approval.builder()
                .id(TestFixtures.APPROVAL_ID)
                .type(type)
                .requesterId(TestFixtures.USER_DUNDUN_ID)
                .approverId(TestFixtures.USER_TUNTUN_ID)
                .status(ApprovalStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .payload(new HashMap<>(payload))
                .build();
    }

    private SocialEvent openEvent(BigDecimal pointCost) {
        return SocialEvent.builder()
                .id(TestFixtures.EVENT_ID)
                .creatorId(TestFixtures.USER_TUNTUN_ID)
                .category(EventCategory.SOCIAL)
                .title("테스트 이벤트")
                .description("설명")
                .regionText("서울")
                .startsAt(Instant.now().plusSeconds(86400 * 2))  // 2일 후
                .endsAt(Instant.now().plusSeconds(86400 * 2 + 3600))
                .capacity(10)
                .pointCost(pointCost)
                .status(EventStatus.OPEN)
                .visibility(EventVisibility.PUBLIC)
                .managerProgram(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private EventParticipation pendingParticipation() {
        return EventParticipation.builder()
                .id(UUID.randomUUID())
                .eventId(TestFixtures.EVENT_ID)
                .participantId(TestFixtures.USER_TUNTUN_ID)
                .status(ParticipationStatus.PENDING_APPROVAL)
                .proxiedBy(TestFixtures.USER_DUNDUN_ID)
                .joinedAt(Instant.now())
                .build();
    }

    private EventParticipation confirmedParticipation() {
        return EventParticipation.builder()
                .id(UUID.randomUUID())
                .eventId(TestFixtures.EVENT_ID)
                .participantId(TestFixtures.USER_TUNTUN_ID)
                .status(ParticipationStatus.CONFIRMED)
                .proxiedBy(TestFixtures.USER_DUNDUN_ID)
                .joinedAt(Instant.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT_JOIN
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EVENT_JOIN")
    class EventJoinTests {

        @Test
        @DisplayName("approve — PENDING_APPROVAL 참여 존재·포인트 정상 → CONFIRMED + 포인트 차감")
        void approve_event_join_confirms_participation_and_debits_points() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_JOIN);
            EventParticipation participation = pendingParticipation();
            SocialEvent event = openEvent(BigDecimal.valueOf(5000));

            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(participationRepository.findByApprovalId(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(participation));
            when(eventRepository.findWithLockById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(event));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.approve(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
            verify(pointService).debit(
                    eq(TestFixtures.USER_TUNTUN_ID), eq(BigDecimal.valueOf(5000)),
                    eq(PointReason.EVENT_JOIN), any(), any());
            verify(notificationDispatcher).notify(
                    eq(TestFixtures.USER_DUNDUN_ID), any(), any(), any(), any());
        }

        @Test
        @DisplayName("approve — 무료 이벤트(pointCost=0) → 포인트 차감 없이 CONFIRMED")
        void approve_event_join_free_event_no_debit() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_JOIN);
            EventParticipation participation = pendingParticipation();
            SocialEvent freeEvent = openEvent(BigDecimal.ZERO);

            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(participationRepository.findByApprovalId(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(participation));
            when(eventRepository.findWithLockById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(freeEvent));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            facade.approve(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            verify(pointService, never()).debit(any(), any(), any(), any(), any());
            assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("reject — PENDING_APPROVAL 참여 → CANCELED 전환")
        void reject_event_join_cancels_participation() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_JOIN);
            EventParticipation participation = pendingParticipation();

            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(participationRepository.findByApprovalId(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(participation));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.reject(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
            verify(pointService, never()).debit(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("expire — PENDING_APPROVAL 참여 → EXPIRED + 참여 CANCELED")
        void expire_event_join_cancels_participation() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_JOIN);
            EventParticipation participation = pendingParticipation();

            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(participationRepository.findByApprovalId(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(participation));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.expireOne(TestFixtures.APPROVAL_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.EXPIRED);
            assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
            verify(notificationDispatcher).notify(
                    eq(TestFixtures.USER_DUNDUN_ID), any(), any(), any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT_CANCEL
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EVENT_CANCEL")
    class EventCancelTests {

        @Test
        @DisplayName("approve — 이벤트 24h 전 → 100% 환불 + 참여 CANCELED")
        void approve_event_cancel_full_refund_before_cutoff() {
            // given
            UUID participationId = UUID.randomUUID();
            Map<String, Object> payload = new HashMap<>();
            payload.put("participationId", participationId.toString());

            Approval approval = pendingApprovalWithPayload(ApprovalType.EVENT_CANCEL, payload);
            EventParticipation participation = confirmedParticipation();
            // 이벤트 시작이 25시간 후 → 100% 환불 구간
            SocialEvent event = SocialEvent.builder()
                    .id(TestFixtures.EVENT_ID)
                    .creatorId(TestFixtures.USER_TUNTUN_ID)
                    .category(EventCategory.SOCIAL)
                    .title("이벤트")
                    .description("")
                    .regionText("서울")
                    .startsAt(Instant.now().plus(25, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(26, ChronoUnit.HOURS))
                    .capacity(10)
                    .pointCost(BigDecimal.valueOf(5000))
                    .status(EventStatus.OPEN)
                    .visibility(EventVisibility.PUBLIC)
                    .managerProgram(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(participationRepository.findById(participationId)).thenReturn(Optional.of(participation));
            when(eventRepository.findById(TestFixtures.EVENT_ID)).thenReturn(Optional.of(event));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.approve(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
            verify(pointService).credit(
                    eq(TestFixtures.USER_TUNTUN_ID), eq(BigDecimal.valueOf(5000)),
                    eq(PointReason.EVENT_REFUND), any(), any());
        }

        @Test
        @DisplayName("reject — EVENT_CANCEL 거절 → 참여 상태 변경 없음")
        void reject_event_cancel_no_side_effect() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_CANCEL);
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.reject(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            verify(participationRepository, never()).save(any());
            verify(pointService, never()).credit(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("expire — EVENT_CANCEL → EXPIRED (참여는 그대로)")
        void expire_event_cancel_sets_expired() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_CANCEL);
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // EVENT_CANCEL 타입이므로 findByApprovalId는 호출 안 됨

            // when
            Approval result = facade.expireOne(TestFixtures.APPROVAL_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.EXPIRED);
            verify(participationRepository, never()).findByApprovalId(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT_CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EVENT_CREATE")
    class EventCreateTests {

        private Map<String, Object> validEventPayload() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("regionText", "서울");
            payload.put("category", "SOCIAL");
            payload.put("title", "새 이벤트");
            payload.put("description", "설명");
            payload.put("startsAt", Instant.now().plusSeconds(7200).toString());
            payload.put("endsAt", Instant.now().plusSeconds(10800).toString());
            payload.put("capacity", 10);
            payload.put("pointCost", "0");
            payload.put("visibility", "PUBLIC");
            payload.put("managerProgram", false);
            return payload;
        }

        @Test
        @DisplayName("approve — 유효 payload → SocialEvent INSERT + 페이로드에 createdEventId 기록")
        void approve_event_create_inserts_event_and_records_id() {
            // given
            Approval approval = pendingApprovalWithPayload(ApprovalType.EVENT_CREATE, validEventPayload());
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.approve(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            verify(eventRepository).save(any(SocialEvent.class));
            assertThat(result.getPayload()).containsKey("createdEventId");
        }

        @Test
        @DisplayName("reject — EVENT_CREATE 거절 → 이벤트 INSERT 없음")
        void reject_event_create_no_event_inserted() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_CREATE);
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.reject(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("expire — EVENT_CREATE → EXPIRED + 이벤트 INSERT 없음")
        void expire_event_create_no_event_inserted() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_CREATE);
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Approval result = facade.expireOne(TestFixtures.APPROVAL_ID);

            // then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.EXPIRED);
            verify(eventRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 가드 케이스
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("공통 가드")
    class GuardTests {

        @Test
        @DisplayName("approve — approver가 다른 사용자 → FORBIDDEN")
        void approve_wrong_approver_throws_forbidden() {
            // given
            Approval approval = pendingApproval(ApprovalType.EVENT_JOIN);
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID)).thenReturn(Optional.of(approval));

            UUID wrongUser = TestFixtures.USER_OTHER_ID;

            // when / then
            assertThatThrownBy(() -> facade.approve(TestFixtures.APPROVAL_ID, wrongUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("approve — 이미 결정된 APPROVED approval → APPROVAL_ALREADY_DECIDED")
        void approve_already_decided_throws_conflict() {
            // given
            Approval alreadyApproved = Approval.builder()
                    .id(TestFixtures.APPROVAL_ID)
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(TestFixtures.USER_DUNDUN_ID)
                    .approverId(TestFixtures.USER_TUNTUN_ID)
                    .status(ApprovalStatus.APPROVED)  // already decided
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .createdAt(Instant.now())
                    .payload(new HashMap<>())
                    .build();
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(alreadyApproved));

            // when / then
            assertThatThrownBy(() -> facade.approve(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.APPROVAL_ALREADY_DECIDED);
        }

        @Test
        @DisplayName("approve — 만료된 approval → APPROVAL_EXPIRED")
        void approve_expired_approval_throws_expired() {
            // given
            Approval expired = Approval.builder()
                    .id(TestFixtures.APPROVAL_ID)
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(TestFixtures.USER_DUNDUN_ID)
                    .approverId(TestFixtures.USER_TUNTUN_ID)
                    .status(ApprovalStatus.PENDING)
                    .expiresAt(Instant.now().minusSeconds(1))  // 이미 만료
                    .createdAt(Instant.now().minusSeconds(3600))
                    .payload(new HashMap<>())
                    .build();
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(expired));

            // when / then
            assertThatThrownBy(() -> facade.approve(TestFixtures.APPROVAL_ID, TestFixtures.USER_TUNTUN_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.APPROVAL_EXPIRED);
        }

        @Test
        @DisplayName("expireOne — 이미 APPROVED인 approval → 상태 변경 없이 반환")
        void expire_already_approved_returns_unchanged() {
            // given
            Approval approved = Approval.builder()
                    .id(TestFixtures.APPROVAL_ID)
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(TestFixtures.USER_DUNDUN_ID)
                    .approverId(TestFixtures.USER_TUNTUN_ID)
                    .status(ApprovalStatus.APPROVED)
                    .expiresAt(Instant.now().minusSeconds(1))
                    .createdAt(Instant.now().minusSeconds(3600))
                    .payload(new HashMap<>())
                    .build();
            when(approvalRepository.findById(TestFixtures.APPROVAL_ID))
                    .thenReturn(Optional.of(approved));

            // when
            Approval result = facade.expireOne(TestFixtures.APPROVAL_ID);

            // then — 이미 결정됐으므로 save 호출 없음, 상태 유지
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            verify(approvalRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // computeRefund 정적 메서드 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("computeRefund — 환불 정책")
    class ComputeRefundTests {

        private SocialEvent eventStartingAt(Instant startsAt) {
            return SocialEvent.builder()
                    .id(TestFixtures.EVENT_ID)
                    .creatorId(TestFixtures.USER_TUNTUN_ID)
                    .category(EventCategory.SOCIAL)
                    .title("이벤트")
                    .description("")
                    .regionText("서울")
                    .startsAt(startsAt)
                    .endsAt(startsAt.plusSeconds(3600))
                    .capacity(10)
                    .pointCost(BigDecimal.valueOf(10_000))
                    .status(EventStatus.OPEN)
                    .visibility(EventVisibility.PUBLIC)
                    .managerProgram(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("시작 25시간 전 취소 → 100% 환불")
        void refund_before_24h_cutoff_returns_full() {
            SocialEvent event = eventStartingAt(Instant.now().plus(25, ChronoUnit.HOURS));
            BigDecimal refund = ApprovalFacade.computeRefund(event, Instant.now());
            assertThat(refund).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        }

        @Test
        @DisplayName("시작 2시간 전 취소 → 50% 환불")
        void refund_between_24h_and_1h_returns_half() {
            SocialEvent event = eventStartingAt(Instant.now().plus(2, ChronoUnit.HOURS));
            BigDecimal refund = ApprovalFacade.computeRefund(event, Instant.now());
            assertThat(refund).isEqualByComparingTo(BigDecimal.valueOf(5_000));
        }

        @Test
        @DisplayName("이벤트 시작 후 취소 → 0% 환불")
        void refund_after_start_returns_zero() {
            SocialEvent event = eventStartingAt(Instant.now().minus(1, ChronoUnit.HOURS));
            BigDecimal refund = ApprovalFacade.computeRefund(event, Instant.now());
            assertThat(refund).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
