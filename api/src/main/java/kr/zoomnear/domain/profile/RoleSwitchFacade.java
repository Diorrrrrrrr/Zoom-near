package kr.zoomnear.domain.profile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.linkage.LinkageRepository;
import kr.zoomnear.domain.linkage.LinkageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 역할 전환(둔둔 ↔ 튼튼) 도메인 파사드.
/// MANAGER/ADMIN 변경은 ADMIN API 통해서만 가능.
/// 활성 연동·진행 중 승인·미래 활성 참여가 0건일 때만 허용.
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleSwitchFacade {

    private final UserRepository userRepository;
    private final LinkageRepository linkageRepository;
    private final ApprovalRepository approvalRepository;
    private final EventParticipationRepository participationRepository;

    @Transactional
    @AuditLog(value = "ROLE_SWITCH", targetType = "user")
    public User switchRole(UUID userId, Role newRole) {
        if (newRole != Role.DUNDUN && newRole != Role.TUNTUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "DUNDUN ↔ TUNTUN 전환만 허용됩니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "MANAGER/ADMIN은 역할 전환 대상이 아닙니다.");
        }
        if (user.getRole() == newRole) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 동일한 역할입니다.");
        }

        long activeLinkages = linkageRepository.countActiveAsAnyParty(userId, LinkageStatus.ACTIVE);
        if (activeLinkages > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "활성 연동(" + activeLinkages + "건)이 남아 역할 전환이 불가합니다.");
        }
        long pendingApprovals = approvalRepository.countActiveByRequesterOrApprover(
                userId, ApprovalStatus.PENDING);
        if (pendingApprovals > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "진행 중 승인(" + pendingApprovals + "건)이 남아 역할 전환이 불가합니다.");
        }
        long activeParticipations = participationRepository.countActiveFutureParticipations(
                userId,
                List.of(ParticipationStatus.PENDING_APPROVAL, ParticipationStatus.CONFIRMED),
                Instant.now());
        if (activeParticipations > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "미래 이벤트 활성 참여(" + activeParticipations + "건)가 남아 역할 전환이 불가합니다.");
        }

        Role old = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role switched user={} {} -> {}", userId, old, newRole);
        return user;
    }
}
