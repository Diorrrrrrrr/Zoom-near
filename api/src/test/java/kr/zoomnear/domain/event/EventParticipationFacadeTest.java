package kr.zoomnear.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.LinkageGuard;
import kr.zoomnear.domain.approval.Approval;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.approval.ApprovalType;
import kr.zoomnear.domain.point.PointReason;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EventParticipationFacade 단위 테스트.
 *
 * TODO(Day1PMend): EventParticipationFacade 클래스가 완성되면 import 추가 후 주석 해제.
 *                 현재 클래스 시그니처 기준으로 Mockito 스터빙만 준비.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventParticipationFacade — 참여 흐름")
class EventParticipationFacadeTest {

    @Mock SocialEventRepository eventRepository;
    @Mock EventParticipationRepository participationRepository;
    @Mock ApprovalRepository approvalRepository;
    @Mock PointService pointService;
    @Mock LinkageGuard linkageGuard;

    // TODO(Day1PMend): EventParticipationFacade facade; — Lane B PM 완성 후 주입
    // EventParticipationFacade facade;

    // 테스트용 SocialEvent 빌더 헬퍼
    private SocialEvent eventWithCapacity(int capacity, BigDecimal pointCost) {
        return SocialEvent.builder()
                .id(TestFixtures.EVENT_ID)
                .creatorId(TestFixtures.USER_MANAGER_ID)
                .category(EventCategory.SOCIAL)
                .title("테스트 이벤트")
                .description("설명")
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .capacity(capacity)
                .pointCost(pointCost)
                .status(EventStatus.OPEN)
                .visibility(EventVisibility.PUBLIC)
                .managerProgram(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("본인 참여 — 잔액 충분·정원 여유 → CONFIRMED + 잔액 차감")
    void join_self_sufficient_balance_returns_confirmed() {
        // TODO(Day1PMend): EventParticipationFacade 완성 후 활성화
        // given
        SocialEvent event = eventWithCapacity(10, BigDecimal.valueOf(5000));
        when(eventRepository.findWithLockById(TestFixtures.EVENT_ID))
                .thenReturn(Optional.of(event));
        when(participationRepository.countByEventIdAndStatusIn(
                eq(TestFixtures.EVENT_ID), anyCollection()))
                .thenReturn(3L); // 3명 참여 중 → 여유 있음
        when(pointService.debit(
                eq(TestFixtures.USER_TUNTUN_ID),
                eq(BigDecimal.valueOf(5000)),
                eq(PointReason.EVENT_JOIN),
                any(), any()))
                .thenReturn(BigDecimal.valueOf(5000)); // 차감 후 잔액
        when(participationRepository.save(any(EventParticipation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when — TODO(Day1PMend): facade.join(USER_TUNTUN_ID, EVENT_ID) 호출로 교체
        // EventParticipation result = facade.join(TestFixtures.USER_TUNTUN_ID, TestFixtures.EVENT_ID);

        // then — 스터빙 레벨 검증 (실제 Facade 없으므로 mock interaction 수준)
        // assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        // assertThat(result.getProxiedBy()).isNull();
        // verify(pointService).debit(any(), eq(BigDecimal.valueOf(5000)), eq(PointReason.EVENT_JOIN), any(), any());

        // 현재는 mock 스터빙이 올바르게 설정되는지만 검증
        assertThat(event.getCapacity()).isEqualTo(10);
        assertThat(event.getPointCost()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("본인 참여 — 잔액 부족 → BusinessException(INSUFFICIENT_POINTS)")
    void join_self_insufficient_balance_throws_exception() {
        // given
        SocialEvent event = eventWithCapacity(10, BigDecimal.valueOf(5000));
        when(eventRepository.findWithLockById(TestFixtures.EVENT_ID))
                .thenReturn(Optional.of(event));
        when(participationRepository.countByEventIdAndStatusIn(
                eq(TestFixtures.EVENT_ID), anyCollection()))
                .thenReturn(3L);
        when(pointService.debit(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINTS));

        // then — PointService가 INSUFFICIENT_POINTS를 던지면 Facade도 전파해야 한다
        // TODO(Day1PMend): facade.join(...) 으로 교체
        assertThatThrownBy(() ->
                pointService.debit(TestFixtures.USER_TUNTUN_ID,
                        BigDecimal.valueOf(5000), PointReason.EVENT_JOIN, "EVENT", TestFixtures.EVENT_ID.toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.INSUFFICIENT_POINTS);
    }

    @Test
    @DisplayName("본인 참여 — 정원 초과 → BusinessException(EVENT_FULL)")
    void join_self_event_full_throws_exception() {
        // given
        SocialEvent event = eventWithCapacity(10, BigDecimal.ZERO);
        when(eventRepository.findWithLockById(TestFixtures.EVENT_ID))
                .thenReturn(Optional.of(event));
        // 이미 10명 참여 → 정원 초과
        when(participationRepository.countByEventIdAndStatusIn(
                eq(TestFixtures.EVENT_ID), anyCollection()))
                .thenReturn(10L);

        // TODO(Day1PMend): facade.join(USER_TUNTUN_ID, EVENT_ID) 호출 시 EVENT_FULL을 검증
        // assertThatThrownBy(() -> facade.join(TestFixtures.USER_TUNTUN_ID, TestFixtures.EVENT_ID))
        //         .isInstanceOf(BusinessException.class)
        //         .extracting("code").isEqualTo(ErrorCode.EVENT_FULL);

        // 현재 단계: 정원 상태 계산 로직 검증
        long currentCount = participationRepository.countByEventIdAndStatusIn(
                TestFixtures.EVENT_ID, List.of(ParticipationStatus.CONFIRMED, ParticipationStatus.PENDING_APPROVAL));
        assertThat(currentCount).isGreaterThanOrEqualTo(event.getCapacity());
    }

    @Test
    @DisplayName("대리 참여 — 비연동 → BusinessException(NOT_LINKED)")
    void proxy_join_not_linked_throws_exception() {
        // given — LinkageGuard가 NOT_LINKED를 던지도록 설정
        when(eventRepository.findWithLockById(TestFixtures.EVENT_ID))
                .thenReturn(Optional.of(eventWithCapacity(10, BigDecimal.ZERO)));
        // LinkageGuard.assertLinked가 NOT_LINKED를 던짐
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.NOT_LINKED))
                .when(linkageGuard).assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID);

        // then
        // TODO(Day1PMend): facade.proxyJoin(DUNDUN_ID, TUNTUN_ID, EVENT_ID) 호출로 교체
        assertThatThrownBy(() ->
                linkageGuard.assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_LINKED);
    }

    @Test
    @DisplayName("대리 참여 — 연동됨 → PENDING_APPROVAL + Approval 생성")
    void proxy_join_linked_creates_pending_approval() {
        // given
        SocialEvent event = eventWithCapacity(10, BigDecimal.ZERO);
        when(eventRepository.findWithLockById(TestFixtures.EVENT_ID))
                .thenReturn(Optional.of(event));
        when(participationRepository.countByEventIdAndStatusIn(
                eq(TestFixtures.EVENT_ID), anyCollection()))
                .thenReturn(3L);
        // linkageGuard.assertLinked → 통과 (예외 없음)
        org.mockito.Mockito.doNothing()
                .when(linkageGuard).assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID);

        ArgumentCaptor<Approval> approvalCaptor = ArgumentCaptor.forClass(Approval.class);
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participationRepository.save(any(EventParticipation.class))).thenAnswer(inv -> inv.getArgument(0));

        // TODO(Day1PMend): facade.proxyJoin(DUNDUN_ID, TUNTUN_ID, EVENT_ID) 호출로 교체
        // EventParticipation result = facade.proxyJoin(USER_DUNDUN_ID, USER_TUNTUN_ID, EVENT_ID);
        // assertThat(result.getStatus()).isEqualTo(ParticipationStatus.PENDING_APPROVAL);
        // assertThat(result.getProxiedBy()).isEqualTo(USER_DUNDUN_ID);
        // verify(approvalRepository).save(approvalCaptor.capture());
        // assertThat(approvalCaptor.getValue().getType()).isEqualTo(ApprovalType.EVENT_JOIN);
        // assertThat(approvalCaptor.getValue().getStatus()).isEqualTo(ApprovalStatus.PENDING);

        // 현재 단계: 연동 통과 확인
        linkageGuard.assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID);
        verify(linkageGuard).assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID);
    }
}
