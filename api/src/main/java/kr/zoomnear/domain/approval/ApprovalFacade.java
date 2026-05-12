package kr.zoomnear.domain.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
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
import kr.zoomnear.domain.linkage.LinkageFacade;
import kr.zoomnear.domain.point.PointReason;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 4종 승인 워크플로우 파사드.
/// EVENT_JOIN / EVENT_CANCEL / EVENT_CREATE 의 approve / reject / expire 분기를 통합 관리한다.
/// approve/reject 시 부수 효과: 참여 상태 전이, 포인트 차감/환불, 이벤트 INSERT, 알림 발사.
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFacade {

    private static final String REF_TYPE_EVENT = "EVENT";

    private final ApprovalRepository approvalRepository;
    private final EventParticipationRepository participationRepository;
    private final SocialEventRepository eventRepository;
    private final PointService pointService;
    private final NotificationDispatcher notificationDispatcher;
    private final LinkageFacade linkageFacade;

    /// 승인 처리. type별 부수효과 적용 후 상태 APPROVED.
    @Transactional
    @AuditLog("APPROVAL_APPROVE")
    public Approval approve(UUID approvalId, UUID approverId) {
        Approval approval = loadPending(approvalId, approverId);
        Instant now = Instant.now();

        switch (approval.getType()) {
            case EVENT_JOIN -> applyEventJoinApprove(approval);
            case EVENT_CANCEL -> applyEventCancelApprove(approval, now);
            case EVENT_CREATE -> applyEventCreateApprove(approval, approverId, now);
            case LINKAGE_CREATE -> linkageFacade.applyApprovedLinkage(approval);
        }

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setDecidedAt(now);
        Approval saved = approvalRepository.save(approval);

        notifyApprovalResult(saved, true);
        return saved;
    }

    /// 거절 처리. type별 후처리 후 상태 REJECTED.
    @Transactional
    @AuditLog("APPROVAL_REJECT")
    public Approval reject(UUID approvalId, UUID approverId) {
        Approval approval = loadPending(approvalId, approverId);
        Instant now = Instant.now();

        switch (approval.getType()) {
            case EVENT_JOIN -> applyEventJoinReject(approval, now);
            case EVENT_CANCEL -> { /* no side effect — 참여는 그대로 CONFIRMED */ }
            case EVENT_CREATE -> { /* no side effect — 이벤트는 생성되지 않음 */ }
            case LINKAGE_CREATE -> { /* no side effect — 연동은 만들어지지 않음 */ }
        }

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setDecidedAt(now);
        Approval saved = approvalRepository.save(approval);

        notifyApprovalResult(saved, false);
        return saved;
    }

    /// 만료 처리. 배치에서 호출. EVENT_JOIN은 참여를 CANCELED로 마킹.
    @Transactional
    public Approval expireOne(UUID approvalId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            return approval;
        }
        Instant now = Instant.now();

        if (approval.getType() == ApprovalType.EVENT_JOIN) {
            participationRepository.findByApprovalId(approvalId).ifPresent(p -> {
                if (p.getStatus() == ParticipationStatus.PENDING_APPROVAL) {
                    p.setStatus(ParticipationStatus.CANCELED);
                    p.setCanceledAt(now);
                    participationRepository.save(p);
                }
            });
        }

        approval.setStatus(ApprovalStatus.EXPIRED);
        approval.setDecidedAt(now);
        Approval saved = approvalRepository.save(approval);

        // 만료 알림 — 요청자(둔둔)에게.
        notificationDispatcher.notify(
                approval.getRequesterId(),
                NotificationType.APPROVAL_EXPIRED,
                "승인 요청이 만료되었습니다",
                approval.getType().name() + " 승인 요청이 처리되지 않아 만료되었습니다.",
                Map.of("approvalId", approvalId.toString(), "type", approval.getType().name()));
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT_JOIN
    // ─────────────────────────────────────────────────────────────────────────
    private void applyEventJoinApprove(Approval approval) {
        EventParticipation participation = participationRepository.findByApprovalId(approval.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "대상 참여 내역이 없습니다."));
        if (participation.getStatus() != ParticipationStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 참여입니다.");
        }
        SocialEvent event = eventRepository.findWithLockById(participation.getEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (event.getPointCost().signum() > 0) {
            pointService.debit(participation.getParticipantId(), event.getPointCost(),
                    PointReason.EVENT_JOIN, REF_TYPE_EVENT, event.getId().toString());
        }
        participation.setStatus(ParticipationStatus.CONFIRMED);
        participationRepository.save(participation);
    }

    private void applyEventJoinReject(Approval approval, Instant now) {
        participationRepository.findByApprovalId(approval.getId()).ifPresent(p -> {
            if (p.getStatus() == ParticipationStatus.PENDING_APPROVAL) {
                p.setStatus(ParticipationStatus.CANCELED);
                p.setCanceledAt(now);
                participationRepository.save(p);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT_CANCEL
    // ─────────────────────────────────────────────────────────────────────────
    private void applyEventCancelApprove(Approval approval, Instant now) {
        UUID participationId = uuidFromPayload(approval, "participationId");
        EventParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "참여 내역이 없습니다."));
        if (participation.getStatus() != ParticipationStatus.CONFIRMED
                && participation.getStatus() != ParticipationStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.CONFLICT, "취소 가능한 상태가 아닙니다.");
        }
        SocialEvent event = eventRepository.findById(participation.getEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        BigDecimal refund = computeRefund(event, now);
        if (refund.signum() > 0) {
            pointService.credit(participation.getParticipantId(), refund,
                    PointReason.EVENT_REFUND, REF_TYPE_EVENT, event.getId().toString());
        }
        participation.setStatus(ParticipationStatus.CANCELED);
        participation.setCanceledAt(now);
        participationRepository.save(participation);
    }

    /// 환불 정책: 시작 24h 전 100% / 1h 전 50% / 이후 0%.
    public static BigDecimal computeRefund(SocialEvent event, Instant now) {
        BigDecimal cost = event.getPointCost();
        if (cost == null || cost.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        Instant startsAt = event.getStartsAt();
        Instant cutoff100 = startsAt.minus(24, ChronoUnit.HOURS);
        Instant cutoff50 = startsAt.minus(1, ChronoUnit.HOURS);
        if (now.isBefore(cutoff100)) {
            return cost;
        }
        if (now.isBefore(cutoff50)) {
            return cost.divide(BigDecimal.valueOf(2));
        }
        return BigDecimal.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT_CREATE
    // ─────────────────────────────────────────────────────────────────────────
    private void applyEventCreateApprove(Approval approval, UUID approverId, Instant now) {
        Map<String, Object> p = approval.getPayload();
        if (p == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "이벤트 생성 페이로드가 비었습니다.");
        }
        SocialEvent event = SocialEvent.builder()
                .id(UUID.randomUUID())
                .creatorId(approverId)
                .regionText((String) p.get("regionText"))
                .category(EventCategory.valueOf((String) p.get("category")))
                .title((String) p.get("title"))
                .description(p.get("description") == null ? "" : (String) p.get("description"))
                .startsAt(Instant.parse((String) p.get("startsAt")))
                .endsAt(Instant.parse((String) p.get("endsAt")))
                .capacity(((Number) p.get("capacity")).intValue())
                .pointCost(p.get("pointCost") == null
                        ? BigDecimal.ZERO
                        : new BigDecimal(p.get("pointCost").toString()))
                .status(EventStatus.OPEN)
                .visibility(EventVisibility.valueOf(
                        (String) p.getOrDefault("visibility", EventVisibility.REGION_ONLY.name())))
                .managerProgram(Boolean.TRUE.equals(p.get("managerProgram")))
                .createdAt(now)
                .updatedAt(now)
                .build();
        eventRepository.save(event);

        // 생성된 eventId를 payload에 기록해 이후 추적 가능하게 한다.
        Map<String, Object> updated = new HashMap<>(p);
        updated.put("createdEventId", event.getId().toString());
        approval.setPayload(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────────
    private Approval loadPending(UUID approvalId, UUID approverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!approval.getApproverId().equals(approverId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 승인 건만 처리할 수 있습니다.");
        }
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DECIDED);
        }
        if (approval.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.APPROVAL_EXPIRED);
        }
        return approval;
    }

    private UUID uuidFromPayload(Approval approval, String key) {
        Object raw = approval.getPayload() == null ? null : approval.getPayload().get(key);
        if (raw == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "payload." + key + "가 비어있습니다.");
        }
        return UUID.fromString(raw.toString());
    }

    private void notifyApprovalResult(Approval approval, boolean approved) {
        NotificationType type = switch (approval.getType()) {
            case EVENT_JOIN -> approved ? NotificationType.EVENT_JOIN_APPROVED : NotificationType.EVENT_JOIN_REJECTED;
            case EVENT_CANCEL -> approved ? NotificationType.EVENT_CANCEL_APPROVED : NotificationType.EVENT_CANCEL_REJECTED;
            case EVENT_CREATE -> approved ? NotificationType.EVENT_CREATE_APPROVED : NotificationType.EVENT_CREATE_REJECTED;
            case LINKAGE_CREATE -> approved ? NotificationType.LINKAGE_REQUEST_APPROVED : NotificationType.LINKAGE_REQUEST_REJECTED;
        };
        String label = approved ? "승인되었어요" : "거절되었어요";
        String prefix = switch (approval.getType()) {
            case EVENT_JOIN -> "이벤트 참여 요청";
            case EVENT_CANCEL -> "이벤트 취소 요청";
            case EVENT_CREATE -> "이벤트 등록 요청";
            case LINKAGE_CREATE -> "연동 요청";
        };
        notificationDispatcher.notify(
                approval.getRequesterId(),
                type,
                prefix + "이 " + label,
                prefix + "이 " + label + ".",
                Map.of("approvalId", approval.getId().toString(),
                        "type", approval.getType().name()));
    }
}
