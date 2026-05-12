package kr.zoomnear.domain.linkage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.approval.Approval;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.approval.ApprovalType;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 튼튼 ↔ 든든 연동 도메인 파사드.
///
/// 흐름:
///   1) 검색 (`searchByCode`): 호출자 역할의 반대 역할 사용자를 6자리 코드로 찾는다. ADMIN/MANAGER 거부.
///   2) 요청 (`requestLink`): 요청자가 상대에게 LINKAGE_CREATE 승인을 보냄. 실제 linkages 행은 아직 없음.
///   3) 승인/거절: `ApprovalFacade.approve|reject` 가 type=LINKAGE_CREATE 분기에서 `applyApprovedLinkage` 를 호출.
///   4) 해제 (`unlink`): 연동 당사자만 ACTIVE→REVOKED.
///
/// 가드:
///   - ADMIN/MANAGER 는 검색·요청·목록·해제 모두 차단 (호출자 역할 검증).
///   - 같은 (dundun, tuntun) 쌍의 ACTIVE 연동이 이미 있으면 CONFLICT.
///   - 동일 쌍의 PENDING 요청 중복도 CONFLICT.
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkageFacade {

    private static final long REQUEST_TTL_HOURS = 72;

    private final LinkageRepository linkageRepository;
    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher notificationDispatcher;

    /// 호출자 역할의 반대 역할 사용자 검색. ADMIN/MANAGER 호출은 FORBIDDEN.
    @Transactional(readOnly = true)
    public User searchByCode(Role callerRole, String code6) {
        if (callerRole != Role.TUNTUN && callerRole != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "연동 검색은 튼튼이/든든이만 사용할 수 있습니다.");
        }
        if (code6 == null || code6.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "uniqueCode가 비어있습니다.");
        }
        Role expectedRole = callerRole == Role.DUNDUN ? Role.TUNTUN : Role.DUNDUN;
        User user = userRepository.findByUniqueCode(code6.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 코드의 사용자가 없습니다."));
        if (user.getRole() != expectedRole) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 코드의 사용자가 없습니다.");
        }
        return user;
    }

    /// 연동 요청 생성. 즉시 linkage 행이 만들어지지 않고 Approval(LINKAGE_CREATE) 만 생성됨.
    /// 호출자(actor)가 DUNDUN 이면 dundunId=actor, tuntunId=other. TUNTUN 이면 그 반대.
    @Transactional
    @AuditLog(value = "LINKAGE_REQUEST", targetType = "linkage")
    public Approval requestLink(UUID actorId, Role actorRole, UUID otherUserId, boolean isPrimary) {
        if (actorId == null || otherUserId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (actorId.equals(otherUserId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "자기 자신과는 연동할 수 없습니다.");
        }
        if (actorRole != Role.TUNTUN && actorRole != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "연동은 튼튼이/든든이만 사용할 수 있습니다.");
        }
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상대 사용자를 찾을 수 없습니다."));
        Role expected = actorRole == Role.DUNDUN ? Role.TUNTUN : Role.DUNDUN;
        if (other.getRole() != expected) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "상대 사용자의 역할이 연동 가능한 대상이 아닙니다.");
        }

        UUID dundunId = actorRole == Role.DUNDUN ? actorId : otherUserId;
        UUID tuntunId = actorRole == Role.TUNTUN ? actorId : otherUserId;

        // 이미 ACTIVE 연동이 있으면 거부
        linkageRepository.findByDundunIdAndTuntunId(dundunId, tuntunId).ifPresent(existing -> {
            if (existing.getStatus() == LinkageStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT, "이미 활성 연동입니다.");
            }
        });

        // 동일 쌍의 PENDING 승인이 있으면 거부
        List<Approval> pendings = approvalRepository.findByApproverIdAndStatusAndTypeOrderByExpiresAtAsc(
                otherUserId, ApprovalStatus.PENDING, ApprovalType.LINKAGE_CREATE);
        for (Approval p : pendings) {
            if (p.getRequesterId().equals(actorId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "이미 대기 중인 연동 요청이 있습니다.");
            }
        }

        Instant now = Instant.now();
        Map<String, Object> payload = new HashMap<>();
        payload.put("dundunId", dundunId.toString());
        payload.put("tuntunId", tuntunId.toString());
        payload.put("isPrimary", isPrimary);
        payload.put("actorRole", actorRole.name());

        Approval approval = Approval.builder()
                .id(UUID.randomUUID())
                .type(ApprovalType.LINKAGE_CREATE)
                .requesterId(actorId)
                .approverId(otherUserId)
                .payload(payload)
                .status(ApprovalStatus.PENDING)
                .expiresAt(now.plus(REQUEST_TTL_HOURS, ChronoUnit.HOURS))
                .createdAt(now)
                .build();
        Approval saved = approvalRepository.save(approval);

        String actorName = userRepository.findById(actorId).map(User::getName).orElse("회원");
        notificationDispatcher.notify(
                otherUserId,
                NotificationType.LINKAGE_REQUEST_RECEIVED,
                "연동 요청이 도착했어요",
                actorName + "님이 연동을 요청했습니다. 승인하면 연동이 시작됩니다.",
                Map.of("approvalId", saved.getId().toString(),
                        "requesterId", actorId.toString()));
        return saved;
    }

    /// ApprovalFacade 가 LINKAGE_CREATE 승인 시 호출. 실제 linkage 행을 생성·재활성한다.
    /// 동일 (dundun, tuntun) 쌍이 과거 REVOKED 로 남아있으면 UNIQUE 충돌 회피 위해 UPDATE 로 재활성.
    @Transactional
    public Linkage applyApprovedLinkage(Approval approval) {
        Map<String, Object> p = approval.getPayload();
        if (p == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "연동 페이로드가 비었습니다.");
        }
        UUID dundunId = UUID.fromString(p.get("dundunId").toString());
        UUID tuntunId = UUID.fromString(p.get("tuntunId").toString());
        boolean isPrimary = Boolean.TRUE.equals(p.get("isPrimary"));

        var existingOpt = linkageRepository.findByDundunIdAndTuntunId(dundunId, tuntunId);
        if (existingOpt.isPresent()) {
            Linkage existing = existingOpt.get();
            if (existing.getStatus() == LinkageStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT, "이미 활성 연동입니다.");
            }
            // REVOKED → ACTIVE 재활성
            existing.setStatus(LinkageStatus.ACTIVE);
            existing.setPrimary(isPrimary);
            existing.setCreatedAt(Instant.now());
            Linkage saved = linkageRepository.save(existing);
            log.info("Linkage reactivated approval={} dundun={} tuntun={}", approval.getId(), dundunId, tuntunId);
            return saved;
        }

        Linkage linkage = Linkage.builder()
                .id(UUID.randomUUID())
                .dundunId(dundunId)
                .tuntunId(tuntunId)
                .primary(isPrimary)
                .status(LinkageStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        Linkage saved = linkageRepository.save(linkage);
        log.info("Linkage created via approval={} dundun={} tuntun={}", approval.getId(), dundunId, tuntunId);
        return saved;
    }

    /// 초대 토큰 consume 시 즉시 연동 생성 (승인 흐름 우회).
    /// 사용처: InviteFacade — 초대받은 튼튼이가 가입을 마치면 발급자(든든이)와 즉시 ACTIVE.
    @Transactional
    @AuditLog(value = "LINKAGE_CREATE_DIRECT", targetType = "linkage")
    public Linkage linkImmediately(UUID dundunId, UUID tuntunId, boolean isPrimary) {
        if (dundunId == null || tuntunId == null || dundunId.equals(tuntunId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        linkageRepository.findByDundunIdAndTuntunId(dundunId, tuntunId).ifPresent(existing -> {
            if (existing.getStatus() == LinkageStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT, "이미 활성 연동입니다.");
            }
        });
        Linkage linkage = Linkage.builder()
                .id(UUID.randomUUID())
                .dundunId(dundunId)
                .tuntunId(tuntunId)
                .primary(isPrimary)
                .status(LinkageStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        Linkage saved = linkageRepository.save(linkage);
        String dundunName = userRepository.findById(dundunId).map(User::getName).orElse("회원");
        notificationDispatcher.notify(
                tuntunId,
                NotificationType.LINKAGE_CREATED,
                "새 연동이 생성되었습니다",
                dundunName + "님과 연동되었습니다.",
                Map.of("linkageId", saved.getId().toString(),
                        "dundunId", dundunId.toString()));
        return saved;
    }

    /// 연결 soft revoke. ADMIN/MANAGER 는 거부, 본인의 연결만 해제 가능.
    @Transactional
    @AuditLog(value = "LINKAGE_REVOKE", targetType = "linkage")
    public void unlink(UUID linkageId, UUID actorId, Role actorRole) {
        if (actorRole != Role.TUNTUN && actorRole != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "연동 해제는 튼튼이/든든이만 사용할 수 있습니다.");
        }
        Linkage linkage = linkageRepository.findById(linkageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!linkage.getDundunId().equals(actorId) && !linkage.getTuntunId().equals(actorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "연동 당사자만 해제할 수 있습니다.");
        }
        if (linkage.getStatus() == LinkageStatus.REVOKED) {
            return;
        }
        linkage.setStatus(LinkageStatus.REVOKED);
        linkageRepository.save(linkage);
    }
}
