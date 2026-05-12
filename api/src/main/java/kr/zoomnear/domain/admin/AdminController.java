package kr.zoomnear.domain.admin;

import jakarta.validation.Valid;
import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.admin.dto.ChangeRoleRequest;
import kr.zoomnear.domain.admin.dto.SuspendUserRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.event.SocialEvent;
import kr.zoomnear.domain.event.dto.AdminUpdateEventRequest;
import kr.zoomnear.domain.event.dto.EventSummaryResponse;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 관리자/매니저 진입점 (/api/v1/admin/**).
/// 기본: ADMIN. 회원·이벤트 조회/관리 일부는 MANAGER 도 허용. 역할 변경/감사 로그는 ADMIN 전용.
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminFacade adminFacade;
    private final EventParticipationRepository participationRepository;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspend(
            @PathVariable UUID userId,
            @Valid @RequestBody(required = false) SuspendUserRequest req) {
        String reason = req == null ? null : req.reason();
        adminFacade.suspendUser(userId, reason);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID userId) {
        adminFacade.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    /// 역할 변경. ADMIN/MANAGER 가능하지만 TUNTUN↔DUNDUN 전환만 허용 (Facade 에서 강제).
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/users/{userId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        UUID actorId = principal == null ? null : principal.userId();
        adminFacade.changeUserRole(userId, req.role(), actorId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/events/{eventId}/force-close")
    public ResponseEntity<Void> forceClose(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String reason) {
        adminFacade.forceCloseEvent(eventId, reason);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/events/{eventId}")
    public ResponseEntity<SocialEvent> modifyEvent(
            @PathVariable UUID eventId,
            @RequestBody AdminUpdateEventRequest req) {
        return ResponseEntity.ok(adminFacade.modifyEvent(eventId, req.event(), req.reason()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String reason) {
        adminFacade.deleteEvent(eventId, reason);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/users")
    public ResponseEntity<Page<User>> listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = pageable(page, size);
        return ResponseEntity.ok(adminFacade.listUsers(status, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/events")
    public ResponseEntity<Page<EventSummaryResponse>> listEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = pageable(page, size);
        Page<SocialEvent> events = adminFacade.listEvents(status, pageable);
        java.util.List<UUID> ids = events.getContent().stream().map(SocialEvent::getId).toList();
        java.util.Map<UUID, Long> counts = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            participationRepository.countActiveByEventIds(ids,
                    java.util.List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED))
                    .forEach(row -> counts.put((UUID) row[0], (Long) row[1]));
        }
        return ResponseEntity.ok(events.map(e ->
                EventSummaryResponse.from(e, counts.getOrDefault(e.getId(), 0L))));
    }

    /// 감사 로그는 ADMIN 전용.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit-logs")
    public ResponseEntity<java.util.Map<String, Object>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int safePage = Math.max(0, page);
        return ResponseEntity.ok(adminFacade.listAuditLogs(action, actorId, safePage, safeSize));
    }

    private Pageable pageable(int page, int size) {
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        return PageRequest.of(Math.max(0, page), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
