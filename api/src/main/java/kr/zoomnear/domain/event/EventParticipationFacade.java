package kr.zoomnear.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.LinkageGuard;
import kr.zoomnear.domain.approval.Approval;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.approval.ApprovalType;
import kr.zoomnear.domain.event.dto.CreateEventRequest;
import kr.zoomnear.domain.point.PointReason;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.rank.RankService;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 이벤트 참여 워크플로우 파사드.
/// 정원 직렬화: pg_advisory_xact_lock(hashtext(eventId::text)) + SocialEvent PESSIMISTIC_WRITE.
/// 본인 참여=즉시 CONFIRMED + 포인트 차감, 대리 참여=PENDING_APPROVAL + 승인 대기.
@Slf4j
@Service
@RequiredArgsConstructor
public class EventParticipationFacade {

    private static final long PROXY_APPROVAL_DEFAULT_TTL_HOURS = 48;
    private static final long PROXY_CREATE_TTL_HOURS = 48;
    private static final String REF_TYPE_EVENT = "EVENT";

    private final SocialEventRepository eventRepository;
    private final EventParticipationRepository participationRepository;
    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final LinkageGuard linkageGuard;
    private final RankService rankService;
    private final NotificationDispatcher notificationDispatcher;
    private final JdbcTemplate jdbcTemplate;

    /// 이벤트 참여 신청.
    /// proxiedBy != null 이면 둔둔이 튼튼을 대리 신청 → PENDING_APPROVAL.
    /// proxiedBy == null 이면 본인 참여 → CONFIRMED + 포인트 즉시 차감.
    @Transactional
    @AuditLog("EVENT_JOIN")
    public EventParticipation join(UUID eventId, UUID participantId, UUID proxiedBy) {
        // 1) 같은 이벤트에 대한 동시 join 직렬화 (hashtext 기반 advisory lock; void 반환이라 queryForList로 호출)
        jdbcTemplate.queryForList(
                "SELECT pg_advisory_xact_lock(hashtext(?))", eventId.toString());

        // 2) 이벤트 잠금 조회 + 상태 검증
        SocialEvent event = eventRepository.findWithLockById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "참여 가능한 상태가 아닙니다.");
        }
        if (!event.getEndsAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 종료된 이벤트입니다.");
        }

        // 3) 정원 검증
        long active = participationRepository.countByEventIdAndStatusIn(
                eventId, List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED));
        if (active >= event.getCapacity()) {
            throw new BusinessException(ErrorCode.EVENT_FULL);
        }

        Instant now = Instant.now();

        // 4) 대리 참여
        if (proxiedBy != null) {
            linkageGuard.assertLinked(proxiedBy, participantId);

            Instant approvalDeadline = event.getEndsAt().minus(1, ChronoUnit.DAYS);
            Instant defaultDeadline = now.plus(PROXY_APPROVAL_DEFAULT_TTL_HOURS, ChronoUnit.HOURS);
            Instant expiresAt = approvalDeadline.isBefore(defaultDeadline) ? approvalDeadline : defaultDeadline;
            if (!expiresAt.isAfter(now)) {
                throw new BusinessException(ErrorCode.CONFLICT, "승인 만료 시각이 유효하지 않습니다.");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId.toString());
            payload.put("title", event.getTitle());

            Approval approval = Approval.builder()
                    .id(UUID.randomUUID())
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(proxiedBy)
                    .approverId(participantId)
                    .payload(payload)
                    .status(ApprovalStatus.PENDING)
                    .expiresAt(expiresAt)
                    .createdAt(now)
                    .build();
            approvalRepository.save(approval);

            EventParticipation participation = EventParticipation.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .participantId(participantId)
                    .proxiedBy(proxiedBy)
                    .status(ParticipationStatus.PENDING_APPROVAL)
                    .approvalId(approval.getId())
                    .joinedAt(now)
                    .build();
            EventParticipation saved;
            try {
                saved = participationRepository.saveAndFlush(participation);
            } catch (DataIntegrityViolationException ex) {
                throw new BusinessException(ErrorCode.CONFLICT, "이미 신청한 이벤트입니다.");
            }

            // 알림 — 튼튼이에게 대리 참여 요청 알림.
            notificationDispatcher.notify(
                    participantId,
                    NotificationType.EVENT_JOIN_REQUEST,
                    "이벤트 참여 요청",
                    "둔둔이 \"" + event.getTitle() + "\" 참여를 대신 신청했습니다. 승인이 필요합니다.",
                    Map.of("approvalId", approval.getId().toString(),
                            "eventId", eventId.toString()));
            return saved;
        }

        // 5) 본인 참여 — 즉시 차감 + CONFIRMED
        if (event.getPointCost().signum() > 0) {
            pointService.debit(participantId, event.getPointCost(),
                    PointReason.EVENT_JOIN, REF_TYPE_EVENT, eventId.toString());
        }

        EventParticipation participation = EventParticipation.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .participantId(participantId)
                .status(ParticipationStatus.CONFIRMED)
                .joinedAt(now)
                .build();
        try {
            return participationRepository.saveAndFlush(participation);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 신청한 이벤트입니다.");
        }
    }

    /// 본인 또는 대리 취소 요청. 본인이면 즉시 취소 + 환불, 대리면 EVENT_CANCEL approval 생성.
    @Transactional
    @AuditLog("EVENT_CANCEL_REQUEST")
    public Approval requestCancel(UUID participationId, UUID actorId) {
        EventParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        boolean isOwner = participation.getParticipantId().equals(actorId);
        boolean isProxy = participation.getProxiedBy() != null && participation.getProxiedBy().equals(actorId);

        // 둔둔이 본인은 아니지만 ACTIVE 연동을 가지면 대리 취소 가능
        if (!isOwner && !isProxy) {
            User actor = userRepository.findById(actorId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            if (actor.getRole() != Role.DUNDUN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "취소 권한이 없습니다.");
            }
            linkageGuard.assertLinked(actorId, participation.getParticipantId());
            isProxy = true;
        }

        if (participation.getStatus() == ParticipationStatus.CANCELED
                || participation.getStatus() == ParticipationStatus.COMPLETED
                || participation.getStatus() == ParticipationStatus.NO_SHOW) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 종료된 참여입니다.");
        }

        // 호스트(creator)는 자동 참여 — 본인 참여 취소 불가
        SocialEvent eventForCreatorCheck = eventRepository.findById(participation.getEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (eventForCreatorCheck.getCreatorId().equals(participation.getParticipantId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "호스트는 참여를 취소할 수 없습니다.");
        }

        // 본인 취소 — 즉시 처리
        if (isOwner) {
            cancelImmediately(participation);
            return null;
        }

        // 대리 취소 — approval 생성. 만료=min(48h, 시작-1h)
        SocialEvent event = eventRepository.findById(participation.getEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Instant now = Instant.now();
        Instant cap = event.getStartsAt().minus(1, ChronoUnit.HOURS);
        Instant defaultDeadline = now.plus(PROXY_APPROVAL_DEFAULT_TTL_HOURS, ChronoUnit.HOURS);
        Instant expiresAt = cap.isBefore(defaultDeadline) ? cap : defaultDeadline;
        if (!expiresAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "취소 승인 만료 시각이 유효하지 않습니다.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("participationId", participationId.toString());
        payload.put("eventId", event.getId().toString());
        payload.put("title", event.getTitle());

        Approval approval = Approval.builder()
                .id(UUID.randomUUID())
                .type(ApprovalType.EVENT_CANCEL)
                .requesterId(actorId)
                .approverId(participation.getParticipantId())
                .payload(payload)
                .status(ApprovalStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();
        Approval saved = approvalRepository.save(approval);

        notificationDispatcher.notify(
                participation.getParticipantId(),
                NotificationType.EVENT_CANCEL_REQUEST,
                "이벤트 취소 요청",
                "둔둔이 \"" + event.getTitle() + "\" 참여 취소를 요청했습니다.",
                Map.of("approvalId", saved.getId().toString(),
                        "eventId", event.getId().toString()));
        return saved;
    }

    /// 둔둔의 대리 이벤트 등록 요청. 매니저 프로그램은 거부.
    @Transactional
    @AuditLog("EVENT_CREATE_REQUEST")
    public Approval requestCreate(CreateEventRequest req, UUID dundunId, UUID tuntunId) {
        if (req.managerProgram()) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "매니저 프로그램은 매니저 본인만 등록할 수 있습니다.");
        }
        linkageGuard.assertLinked(dundunId, tuntunId);

        if (!req.endsAt().isAfter(req.startsAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "종료 시각은 시작 시각보다 이후여야 합니다.");
        }
        if (req.startsAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "시작 시각은 현재 이후여야 합니다.");
        }

        User tuntun = userRepository.findById(tuntunId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (tuntun.getRole() != Role.TUNTUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "튼튼이 아닌 사용자는 대리 등록 대상이 될 수 없습니다.");
        }

        Instant now = Instant.now();
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", req.title());
        payload.put("description", req.description());
        payload.put("regionText", req.regionText());
        payload.put("category", req.category().name());
        payload.put("startsAt", req.startsAt().toString());
        payload.put("endsAt", req.endsAt().toString());
        payload.put("capacity", req.capacity());
        payload.put("pointCost", req.pointCost() == null ? "0" : req.pointCost().toPlainString());
        EventVisibility visibility = req.visibility() != null ? req.visibility() : EventVisibility.REGION_ONLY;
        payload.put("visibility", visibility.name());
        payload.put("managerProgram", false);

        Approval approval = Approval.builder()
                .id(UUID.randomUUID())
                .type(ApprovalType.EVENT_CREATE)
                .requesterId(dundunId)
                .approverId(tuntunId)
                .payload(payload)
                .status(ApprovalStatus.PENDING)
                .expiresAt(now.plus(PROXY_CREATE_TTL_HOURS, ChronoUnit.HOURS))
                .createdAt(now)
                .build();
        Approval saved = approvalRepository.save(approval);

        notificationDispatcher.notify(
                tuntunId,
                NotificationType.EVENT_CREATE_REQUEST,
                "이벤트 등록 요청",
                "둔둔이 \"" + req.title() + "\" 이벤트 등록을 대신 신청했습니다.",
                Map.of("approvalId", saved.getId().toString()));
        return saved;
    }

    /// 참여 직접 취소. 환불 정책에 따라 ledger 적립.
    @Transactional
    @AuditLog("EVENT_CANCEL")
    public void cancel(UUID participationId, UUID actorId) {
        EventParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        boolean isOwner = participation.getParticipantId().equals(actorId);
        boolean isProxy = participation.getProxiedBy() != null && participation.getProxiedBy().equals(actorId);
        if (!isOwner && !isProxy) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "취소 권한이 없습니다.");
        }
        if (isProxy) {
            linkageGuard.assertLinked(actorId, participation.getParticipantId());
        }
        if (participation.getStatus() == ParticipationStatus.CANCELED
                || participation.getStatus() == ParticipationStatus.COMPLETED
                || participation.getStatus() == ParticipationStatus.NO_SHOW) {
            return;
        }

        SocialEvent eventForCreatorCheck = eventRepository.findById(participation.getEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (eventForCreatorCheck.getCreatorId().equals(participation.getParticipantId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "호스트는 참여를 취소할 수 없습니다.");
        }

        cancelImmediately(participation);
    }

    /// 참여 완료 처리. 사용자 랭크 재산정 트리거.
    @Transactional
    @AuditLog("EVENT_COMPLETE")
    public void markCompleted(UUID participationId) {
        EventParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (participation.getStatus() != ParticipationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CONFLICT, "확정된 참여만 완료 처리할 수 있습니다.");
        }
        participation.setStatus(ParticipationStatus.COMPLETED);
        participationRepository.save(participation);

        rankService.recomputeForUser(participation.getParticipantId());
    }

    private void cancelImmediately(EventParticipation participation) {
        SocialEvent event = eventRepository.findById(participation.getEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Instant now = Instant.now();
        BigDecimal refund = (participation.getStatus() == ParticipationStatus.CONFIRMED)
                ? kr.zoomnear.domain.approval.ApprovalFacade.computeRefund(event, now)
                : BigDecimal.ZERO;
        if (refund.signum() > 0) {
            try {
                pointService.credit(participation.getParticipantId(), refund,
                        PointReason.EVENT_REFUND, REF_TYPE_EVENT, event.getId().toString());
            } catch (DataIntegrityViolationException ex) {
                // ledger UNIQUE 충돌 — 이미 환불됨
                throw new BusinessException(ErrorCode.CONFLICT, "이미 환불 처리된 참여입니다.");
            }
        }
        participation.setStatus(ParticipationStatus.CANCELED);
        participation.setCanceledAt(now);
        participationRepository.save(participation);
    }
}
