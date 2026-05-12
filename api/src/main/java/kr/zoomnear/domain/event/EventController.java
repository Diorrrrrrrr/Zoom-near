package kr.zoomnear.domain.event;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.approval.Approval;
import kr.zoomnear.domain.event.dto.CancelEventRequest;
import kr.zoomnear.domain.event.dto.CreateEventRequest;
import kr.zoomnear.domain.event.dto.DeleteEventRequest;
import kr.zoomnear.domain.event.dto.EventDetailResponse;
import kr.zoomnear.domain.event.dto.EventPageResponse;
import kr.zoomnear.domain.event.dto.EventResponse;
import kr.zoomnear.domain.event.dto.EventSearchRequest;
import kr.zoomnear.domain.event.dto.EventSummaryResponse;
import kr.zoomnear.domain.event.dto.JoinEventRequest;
import kr.zoomnear.domain.event.dto.ProxyCreateEventRequest;
import kr.zoomnear.domain.event.dto.UpdateEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 이벤트 진입점 (/api/v1/events/**).
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventFacade eventFacade;
    private final EventParticipationFacade participationFacade;
    private final EventParticipationRepository participationRepository;

    @GetMapping
    public ResponseEntity<EventPageResponse> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        EventSearchRequest req = new EventSearchRequest(region, page, size);
        Page<SocialEvent> events = eventFacade.list(req);
        Page<EventSummaryResponse> mapped = mapWithJoinedCounts(events);
        return ResponseEntity.ok(EventPageResponse.from(mapped));
    }

    /// 본인이 생성한 이벤트 목록 (createdAt DESC).
    @GetMapping("/mine")
    public ResponseEntity<Page<EventSummaryResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppPrincipal principal = AppPrincipal.current();
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                mapWithJoinedCounts(eventFacade.listMine(principal.userId(), pageable)));
    }

    /// 페이지 단위로 활성 참여자 수를 batch 집계 후 매핑 (N+1 회피).
    private Page<EventSummaryResponse> mapWithJoinedCounts(Page<SocialEvent> events) {
        java.util.List<UUID> ids = events.getContent().stream().map(SocialEvent::getId).toList();
        java.util.Map<UUID, Long> counts = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            participationRepository
                    .countActiveByEventIds(ids,
                            List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED))
                    .forEach(row -> counts.put((UUID) row[0], (Long) row[1]));
        }
        return events.map(e -> EventSummaryResponse.from(e, counts.getOrDefault(e.getId(), 0L)));
    }

    /// 본인 이벤트 부분 수정 (OPEN 상태만).
    @PatchMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(EventResponse.from(eventFacade.update(principal.userId(), id, req)));
    }

    /// 본인 이벤트 삭제 (OPEN 상태만, reason 필수).
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestBody DeleteEventRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        eventFacade.deleteByCreator(principal.userId(), id, req == null ? null : req.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponse> get(@PathVariable UUID id) {
        SocialEvent event = eventFacade.get(id);
        long joined = participationRepository.countByEventIdAndStatusIn(
                id, List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED));
        AppPrincipal principal = AppPrincipal.current();
        String myStatus = participationRepository
                .findFirstByEventIdAndParticipantIdOrderByJoinedAtDesc(id, principal.userId())
                .map(p -> switch (p.getStatus()) {
                    case PENDING_APPROVAL, CONFIRMED -> "JOINED";
                    case CANCELED -> "CANCELLED";
                    default -> null;
                })
                .orElse(null);
        return ResponseEntity.ok(EventDetailResponse.from(event, joined, myStatus));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        SocialEvent event = eventFacade.create(principal.userId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @PostMapping("/proxy-create")
    public ResponseEntity<Approval> proxyCreate(@Valid @RequestBody ProxyCreateEventRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        Approval approval = participationFacade.requestCreate(req.event(), principal.userId(), req.tuntunId());
        return ResponseEntity.status(HttpStatus.CREATED).body(approval);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<EventParticipation> join(
            @PathVariable("id") UUID eventId,
            @RequestBody(required = false) JoinEventRequest req) {
        AppPrincipal principal = AppPrincipal.current();

        UUID participantId;
        UUID proxiedBy;
        if (req != null && req.proxiedTuntunId() != null) {
            participantId = req.proxiedTuntunId();
            proxiedBy = principal.userId();
        } else {
            participantId = principal.userId();
            proxiedBy = null;
        }

        EventParticipation participation = participationFacade.join(eventId, participantId, proxiedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(participation);
    }

    /// 이벤트 단위 취소. body 가 비어있으면 본인의 가장 최근 active 참여를 자동 매핑.
    /// 둔둔이 호출하면 EVENT_CANCEL approval 워크플로우, 본인이면 즉시 취소+환불.
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelByEvent(
            @PathVariable("id") UUID eventId,
            @RequestBody(required = false) CancelEventRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        UUID participationId = (req != null && req.participationId() != null)
                ? req.participationId()
                : resolveActiveParticipationId(eventId, principal.userId());

        Approval approval = participationFacade.requestCancel(participationId, principal.userId());
        if (approval != null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(approval);
        }
        return ResponseEntity.noContent().build();
    }

    /// 본인 또는 대리 취소 (proxy면 EVENT_CANCEL approval 생성).
    @PostMapping("/participations/{participationId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID participationId) {
        AppPrincipal principal = AppPrincipal.current();
        Approval approval = participationFacade.requestCancel(participationId, principal.userId());
        if (approval != null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(approval);
        }
        return ResponseEntity.noContent().build();
    }

    /// (deprecated) DELETE 본인 취소 — 호환 유지.
    @DeleteMapping("/participations/{participationId}")
    public ResponseEntity<Void> cancelLegacy(@PathVariable UUID participationId) {
        AppPrincipal principal = AppPrincipal.current();
        participationFacade.cancel(participationId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    private UUID resolveActiveParticipationId(UUID eventId, UUID userId) {
        return participationRepository
                .findByParticipantIdOrderByJoinedAtDesc(userId).stream()
                .filter(p -> p.getEventId().equals(eventId))
                .filter(p -> p.getStatus() == ParticipationStatus.PENDING_APPROVAL
                        || p.getStatus() == ParticipationStatus.CONFIRMED)
                .map(EventParticipation::getId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "취소 가능한 활성 참여를 찾을 수 없습니다."));
    }
}
