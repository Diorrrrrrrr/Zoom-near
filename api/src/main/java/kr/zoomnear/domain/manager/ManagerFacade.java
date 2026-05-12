package kr.zoomnear.domain.manager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 매니저 신청/승인 도메인 파사드. 셀프 신청 후 ADMIN 승인 시 role을 MANAGER로 전환한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerFacade {

    private final ManagerApplicationRepository repository;
    private final UserRepository userRepository;
    private final NotificationDispatcher notificationDispatcher;

    /// 신청. 동일 사용자에 PENDING 신청이 있으면 거부 (DB unique 인덱스로도 보호).
    @Transactional
    @AuditLog("MANAGER_APPLY")
    public ManagerApplication apply(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getRole() == Role.MANAGER) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 매니저입니다.");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.CONFLICT, "관리자는 매니저 신청 대상이 아닙니다.");
        }
        if (user.getRole() != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "든든이만 매니저 신청이 가능합니다.");
        }
        if (!repository.findByApplicantIdAndStatus(userId, ManagerApplicationStatus.PENDING).isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 진행 중인 매니저 신청이 있습니다.");
        }

        ManagerApplication app = ManagerApplication.builder()
                .id(UUID.randomUUID())
                .applicantId(userId)
                .status(ManagerApplicationStatus.PENDING)
                .reason(reason == null ? "" : reason)
                .createdAt(Instant.now())
                .build();
        return repository.save(app);
    }

    /// 승인. ADMIN 권한자가 호출 — 권한 체크는 컨트롤러의 @PreAuthorize에 의존.
    @Transactional
    @AuditLog("MANAGER_APPROVE")
    public ManagerApplication approve(UUID applicationId, UUID adminId) {
        ManagerApplication app = loadPending(applicationId);
        Instant now = Instant.now();

        User applicant = userRepository.findById(app.getApplicantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신청자가 존재하지 않습니다."));
        applicant.setRole(Role.MANAGER);
        userRepository.save(applicant);

        app.setStatus(ManagerApplicationStatus.APPROVED);
        app.setDecidedBy(adminId);
        app.setDecidedAt(now);
        ManagerApplication saved = repository.save(app);

        notificationDispatcher.notify(
                applicant.getId(),
                NotificationType.MANAGER_APPLICATION_APPROVED,
                "매니저 신청이 승인되었습니다",
                "매니저 권한이 부여되었습니다.",
                Map.of("applicationId", saved.getId().toString()));
        return saved;
    }

    /// 거절. ADMIN 권한자가 호출.
    @Transactional
    @AuditLog("MANAGER_REJECT")
    public ManagerApplication reject(UUID applicationId, UUID adminId) {
        ManagerApplication app = loadPending(applicationId);
        Instant now = Instant.now();
        app.setStatus(ManagerApplicationStatus.REJECTED);
        app.setDecidedBy(adminId);
        app.setDecidedAt(now);
        ManagerApplication saved = repository.save(app);

        notificationDispatcher.notify(
                app.getApplicantId(),
                NotificationType.MANAGER_APPLICATION_REJECTED,
                "매니저 신청이 거절되었습니다",
                "추가 사유는 운영팀에 문의해주세요.",
                Map.of("applicationId", saved.getId().toString()));
        return saved;
    }

    private ManagerApplication loadPending(UUID applicationId) {
        ManagerApplication app = repository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (app.getStatus() != ManagerApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청입니다.");
        }
        return app;
    }
}
