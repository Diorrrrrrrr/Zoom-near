package kr.zoomnear.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.event.dto.CreateEventRequest;
import kr.zoomnear.domain.event.dto.EventSearchRequest;
import kr.zoomnear.domain.event.dto.UpdateEventRequest;
import kr.zoomnear.domain.point.PointReason;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 이벤트 CRUD/조회 도메인 파사드.
/// 참여 흐름은 EventParticipationFacade로 분리한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class EventFacade {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String REF_TYPE_EVENT = "EVENT";

    private final SocialEventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipationRepository participationRepository;
    private final PointService pointService;
    private final NotificationDispatcher notificationDispatcher;

    /// OPEN 상태이고 시작 시각이 미래인 이벤트만 노출한다.
    @Transactional(readOnly = true)
    public Page<SocialEvent> list(EventSearchRequest req) {
        int page = req.page() == null ? 0 : Math.max(0, req.page());
        int size = req.size() == null ? DEFAULT_SIZE : Math.min(MAX_SIZE, Math.max(1, req.size()));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startsAt"));
        Instant now = Instant.now();

        if (req.regionText() != null && !req.regionText().isBlank()) {
            return eventRepository.findByStatusAndRegionTextContainingAndStartsAtAfter(
                    EventStatus.OPEN, req.regionText().trim(), now, pageable);
        }
        return eventRepository.findByStatusAndStartsAtAfter(EventStatus.OPEN, now, pageable);
    }

    /// 단건 조회. 미존재 시 NOT_FOUND.
    @Transactional(readOnly = true)
    public SocialEvent get(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /// 이벤트 생성. TUNTUN/MANAGER만 허용, managerProgram=true는 MANAGER만.
    @Transactional
    @AuditLog("EVENT_CREATE")
    public SocialEvent create(UUID creatorId, CreateEventRequest req) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (creator.getRole() != Role.TUNTUN
                && creator.getRole() != Role.MANAGER
                && creator.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이벤트 생성 권한이 없습니다.");
        }
        if (req.managerProgram()
                && creator.getRole() != Role.MANAGER
                && creator.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "매니저 프로그램은 MANAGER/ADMIN만 생성할 수 있습니다.");
        }
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "종료 시각은 시작 시각보다 이후여야 합니다.");
        }
        if (req.startsAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "시작 시각은 현재 이후여야 합니다.");
        }

        Instant now = Instant.now();
        SocialEvent event = SocialEvent.builder()
                .id(UUID.randomUUID())
                .creatorId(creatorId)
                .regionText(req.regionText())
                .category(req.category())
                .title(req.title())
                .description(req.description() == null ? "" : req.description())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .capacity(req.capacity())
                .pointCost(req.pointCost() == null ? BigDecimal.ZERO : req.pointCost())
                .status(EventStatus.OPEN)
                .visibility(req.visibility() != null ? req.visibility() : EventVisibility.REGION_ONLY)
                .managerProgram(req.managerProgram())
                .createdAt(now)
                .updatedAt(now)
                .build();
        SocialEvent saved = eventRepository.save(event);

        // TUNTUN 호스트는 자동 CONFIRMED 참여 (포인트는 차감 안 함 — 호스트 특권).
        // MANAGER 가 만든 매니저 프로그램은 자동 참여 없음 — 호스트가 진행자 역할.
        if (creator.getRole() == Role.TUNTUN) {
            EventParticipation hostParticipation = EventParticipation.builder()
                    .id(UUID.randomUUID())
                    .eventId(saved.getId())
                    .participantId(creatorId)
                    .status(ParticipationStatus.CONFIRMED)
                    .joinedAt(now)
                    .build();
            participationRepository.saveAndFlush(hostParticipation);
        }
        return saved;
    }

    /// 본인이 생성한 이벤트 목록 (createdAt DESC).
    @Transactional(readOnly = true)
    public Page<SocialEvent> listMine(UUID creatorId, Pageable pageable) {
        return eventRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId, pageable);
    }

    /// 이벤트 수정 (creator only, OPEN 상태만). null이 아닌 필드만 패치.
    @Transactional
    @AuditLog("EVENT_UPDATE")
    public SocialEvent update(UUID creatorId, UUID eventId, UpdateEventRequest req) {
        SocialEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!event.getCreatorId().equals(creatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 생성한 이벤트만 수정할 수 있습니다.");
        }
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "진행 중 이벤트만 수정할 수 있습니다.");
        }

        applyPatch(event, req);

        event.setUpdatedAt(Instant.now());
        SocialEvent saved = eventRepository.save(event);

        // 참여자(creator 제외)에게 수정 알림
        List<EventParticipation> participations = participationRepository.findByEventIdAndStatusIn(
                eventId, List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED));
        for (EventParticipation p : participations) {
            if (p.getParticipantId().equals(creatorId)) continue;
            notificationDispatcher.notify(
                    p.getParticipantId(),
                    NotificationType.EVENT_MODIFIED,
                    "이벤트가 수정되었어요",
                    "\"" + saved.getTitle() + "\" 이벤트 내용이 수정되었습니다.",
                    Map.of("eventId", eventId.toString()));
        }
        return saved;
    }

    /// 이벤트 삭제 (creator only, OPEN 상태만). CANCELED 전이 + 확정 참여자 전액 환불 + 알림.
    @Transactional
    @AuditLog("EVENT_DELETE_BY_CREATOR")
    public void deleteByCreator(UUID creatorId, UUID eventId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "삭제 사유는 필수입니다.");
        }
        SocialEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!event.getCreatorId().equals(creatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 생성한 이벤트만 삭제할 수 있습니다.");
        }
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "진행 중 이벤트만 삭제할 수 있습니다.");
        }

        event.setStatus(EventStatus.CANCELED);
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);

        List<EventParticipation> participations = participationRepository.findByEventIdAndStatusIn(
                eventId, List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED));
        for (EventParticipation p : participations) {
            if (p.getStatus() == ParticipationStatus.CONFIRMED && event.getPointCost().signum() > 0) {
                try {
                    pointService.credit(p.getParticipantId(), event.getPointCost(),
                            PointReason.EVENT_REFUND, REF_TYPE_EVENT, eventId.toString());
                } catch (DataIntegrityViolationException ex) {
                    // ledger UNIQUE 충돌 — 이미 환불됨, 계속 진행
                    log.warn("Refund already exists for participant={} event={}",
                            p.getParticipantId(), eventId);
                }
            }
            p.setStatus(ParticipationStatus.CANCELED);
            p.setCanceledAt(Instant.now());
            participationRepository.save(p);

            if (!p.getParticipantId().equals(creatorId)) {
                notificationDispatcher.notify(
                        p.getParticipantId(),
                        NotificationType.EVENT_DELETED,
                        "이벤트가 취소되었어요",
                        "\"" + event.getTitle() + "\" 이벤트가 취소되었습니다. 사유: " + reason,
                        Map.of("eventId", eventId.toString(), "reason", reason));
            }
        }
    }

    /// UpdateEventRequest의 non-null 필드를 event에 패치. 시각/정원 검증 포함.
    private void applyPatch(SocialEvent event, UpdateEventRequest req) {
        if (req.title() != null) {
            event.setTitle(req.title());
        }
        if (req.description() != null) {
            event.setDescription(req.description());
        }
        if (req.regionText() != null) {
            event.setRegionText(req.regionText());
        }
        if (req.category() != null) {
            event.setCategory(req.category());
        }
        Instant newStartsAt = req.startsAt() != null ? req.startsAt() : event.getStartsAt();
        Instant newEndsAt = req.endsAt() != null ? req.endsAt() : event.getEndsAt();
        if (!newEndsAt.isAfter(newStartsAt)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "종료 시각은 시작 시각보다 이후여야 합니다.");
        }
        if (req.startsAt() != null && req.startsAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "시작 시각은 현재 이후여야 합니다.");
        }
        if (req.startsAt() != null) event.setStartsAt(req.startsAt());
        if (req.endsAt() != null) event.setEndsAt(req.endsAt());

        if (req.capacity() != null) {
            if (req.capacity() < 1) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "정원은 1 이상이어야 합니다.");
            }
            long active = participationRepository.countByEventIdAndStatusIn(
                    event.getId(), List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED));
            if (req.capacity() < active) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "현재 참여자 수보다 적은 정원으로 변경할 수 없습니다.");
            }
            event.setCapacity(req.capacity());
        }
        if (req.pointCost() != null) {
            if (req.pointCost().signum() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "포인트 비용은 0 이상이어야 합니다.");
            }
            event.setPointCost(req.pointCost());
        }
    }

    /// 관리자 수정용: creator 권한 체크 없이 patch만 수행. AdminFacade에서 호출.
    public SocialEvent applyAdminPatch(SocialEvent event, UpdateEventRequest req) {
        applyPatch(event, req);
        event.setUpdatedAt(Instant.now());
        return eventRepository.save(event);
    }
}
