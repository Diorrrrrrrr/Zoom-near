package kr.zoomnear.domain.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.event.EventFacade;
import kr.zoomnear.domain.event.EventParticipation;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.event.SocialEvent;
import kr.zoomnear.domain.event.SocialEventRepository;
import kr.zoomnear.domain.event.dto.UpdateEventRequest;
import kr.zoomnear.domain.point.PointReason;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.profile.UserStatus;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 관리자 도메인 파사드. 사용자 정지/활성화, 이벤트 강제 종료, 감사 로그/사용자/이벤트 목록 조회.
/// 모든 진입은 ADMIN 권한 (컨트롤러의 @PreAuthorize)로 보호된다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFacade {

    private static final String REF_TYPE_EVENT = "EVENT";

    private final UserRepository userRepository;
    private final SocialEventRepository eventRepository;
    private final EventParticipationRepository participationRepository;
    private final PointService pointService;
    private final NotificationDispatcher notificationDispatcher;
    private final JdbcTemplate jdbcTemplate;
    private final EventFacade eventFacade;

    /// 사용자 정지. 로그인은 SecurityConfig에서 status=ACTIVE만 허용.
    @Transactional
    @AuditLog(value = "ADMIN_SUSPEND_USER", targetType = "user")
    public void suspendUser(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.warn("Admin suspended user={} reason={}", userId, reason);

        notificationDispatcher.notify(
                userId,
                NotificationType.USER_SUSPENDED,
                "계정이 정지되었습니다",
                reason == null ? "관리자에 의해 계정이 정지되었습니다." : reason,
                Map.of("reason", reason == null ? "" : reason));
    }

    /// 사용자 역할 직접 변경. TUNTUN↔DUNDUN 만 허용.
    /// 매니저 승격은 ManagerFacade.approve (매니저 신청 승인) 경로로만 가능.
    @Transactional
    @AuditLog(value = "ADMIN_CHANGE_ROLE", targetType = "user")
    public void changeUserRole(UUID userId, Role newRole, UUID actorId) {
        if (newRole == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "role 은 필수입니다.");
        }
        if (newRole != Role.TUNTUN && newRole != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "튼튼이/든든이 외 역할로는 변경할 수 없습니다.");
        }
        if (actorId != null && actorId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 역할은 변경할 수 없습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getRole() != Role.TUNTUN && user.getRole() != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "매니저/관리자 계정의 역할은 변경할 수 없습니다.");
        }
        if (user.getRole() == newRole) {
            return; // no-op
        }
        Role prev = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        log.warn("Admin changed role user={} {} -> {} by={}", userId, prev, newRole, actorId);

        notificationDispatcher.notify(
                userId,
                NotificationType.USER_ROLE_CHANGED,
                "역할이 변경되었습니다",
                "관리자에 의해 역할이 " + roleLabel(newRole) + " 로 변경되었습니다.",
                Map.of("prev", prev.name(), "next", newRole.name()));
    }

    private static String roleLabel(Role r) {
        return switch (r) {
            case TUNTUN -> "튼튼이";
            case DUNDUN -> "든든이";
            case MANAGER -> "매니저";
            case ADMIN -> "관리자";
        };
    }

    /// 사용자 재활성화.
    @Transactional
    @AuditLog(value = "ADMIN_ACTIVATE_USER", targetType = "user")
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        notificationDispatcher.notify(
                userId,
                NotificationType.USER_ACTIVATED,
                "계정이 활성화되었습니다",
                "다시 정상 이용이 가능합니다.",
                Map.of());
    }

    /// 이벤트 강제 종료. 모든 confirmed 참여자에게 100% 환불 + 알림 일괄.
    @Transactional
    @AuditLog(value = "ADMIN_FORCE_CLOSE_EVENT", targetType = "event")
    public void forceCloseEvent(UUID eventId, String reason) {
        SocialEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        event.setStatus(EventStatus.CANCELED);
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);

        List<EventParticipation> participations = participationRepository.findByEventIdAndStatusIn(
                eventId, List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED));
        for (EventParticipation p : participations) {
            if (p.getStatus() == ParticipationStatus.CONFIRMED && event.getPointCost().signum() > 0) {
                pointService.credit(p.getParticipantId(), event.getPointCost(),
                        PointReason.EVENT_REFUND, REF_TYPE_EVENT, eventId.toString());
            }
            p.setStatus(ParticipationStatus.CANCELED);
            p.setCanceledAt(Instant.now());
            participationRepository.save(p);

            notificationDispatcher.notify(
                    p.getParticipantId(),
                    NotificationType.EVENT_CANCELED_BY_ADMIN,
                    "이벤트가 관리자에 의해 종료되었습니다",
                    "\"" + event.getTitle() + "\" 이벤트가 종료되어 환불 처리되었습니다."
                            + (reason == null ? "" : " 사유: " + reason),
                    Map.of("eventId", eventId.toString()));
        }
    }

    /// 관리자 이벤트 수정. creator에게 EVENT_MODIFIED_BY_ADMIN 알림 (reason 본문).
    @Transactional
    @AuditLog(value = "ADMIN_MODIFY_EVENT", targetType = "event")
    public SocialEvent modifyEvent(UUID eventId, UpdateEventRequest req, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "수정 사유는 필수입니다.");
        }
        SocialEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        SocialEvent saved = eventFacade.applyAdminPatch(event, req);

        notificationDispatcher.notify(
                saved.getCreatorId(),
                NotificationType.EVENT_MODIFIED_BY_ADMIN,
                "관리자가 이벤트를 수정했습니다",
                reason,
                Map.of("eventId", eventId.toString(), "reason", reason));
        return saved;
    }

    /// 관리자 이벤트 삭제. 참여자 환불+취소 알림(forceClose와 동일) + creator에게 EVENT_DELETED_BY_ADMIN.
    @Transactional
    @AuditLog(value = "ADMIN_DELETE_EVENT", targetType = "event")
    public void deleteEvent(UUID eventId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "삭제 사유는 필수입니다.");
        }
        SocialEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

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
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    log.warn("Refund already exists for participant={} event={}",
                            p.getParticipantId(), eventId);
                }
            }
            p.setStatus(ParticipationStatus.CANCELED);
            p.setCanceledAt(Instant.now());
            participationRepository.save(p);

            notificationDispatcher.notify(
                    p.getParticipantId(),
                    NotificationType.EVENT_CANCELED_BY_ADMIN,
                    "이벤트가 관리자에 의해 종료되었습니다",
                    "\"" + event.getTitle() + "\" 이벤트가 종료되어 환불 처리되었습니다. 사유: " + reason,
                    Map.of("eventId", eventId.toString()));
        }

        notificationDispatcher.notify(
                event.getCreatorId(),
                NotificationType.EVENT_DELETED_BY_ADMIN,
                "관리자가 이벤트를 삭제했습니다",
                reason,
                Map.of("eventId", eventId.toString(), "reason", reason));
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(UserStatus statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return userRepository.findAll(pageable); // 필터링은 추후 확장
        }
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<SocialEvent> listEvents(EventStatus statusFilter, Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    /// audit_logs 페이지네이션 조회. action/actorId 필터, payload는 JSONB 문자열 그대로 노출.
    @Transactional(readOnly = true)
    public Map<String, Object> listAuditLogs(String action, UUID actorId, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new java.util.ArrayList<>();
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ?");
            args.add(action);
        }
        if (actorId != null) {
            where.append(" AND actor_id = ?");
            args.add(actorId);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_logs" + where, Long.class, args.toArray());

        List<Object> pageArgs = new java.util.ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(page * size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, actor_id, action, target_type, target_id, "
                        + "payload::text AS payload, status, ip, user_agent, created_at "
                        + "FROM audit_logs" + where
                        + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                pageArgs.toArray());

        return Map.of(
                "items", rows,
                "total", total == null ? 0L : total,
                "page", page,
                "size", size);
    }
}
