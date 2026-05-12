package kr.zoomnear.domain.invite;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.linkage.LinkageFacade;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 초대 토큰 도메인 파사드. 둔둔이 토큰을 발급하고, 가입한 튼튼이 consume 하여 자동 연동.
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteFacade {

    private static final long INVITE_TTL_HOURS = 72;

    private final InviteTokenRepository inviteTokenRepository;
    private final UserRepository userRepository;
    private final LinkageFacade linkageFacade;
    private final NotificationDispatcher notificationDispatcher;

    /// 둔둔이 신규 초대 토큰을 발급한다.
    @Transactional
    @AuditLog(value = "INVITE_CREATE", targetType = "invite")
    public InviteToken create(UUID dundunId) {
        User dundun = userRepository.findById(dundunId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (dundun.getRole() != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "둔둔만 초대 토큰을 발급할 수 있습니다.");
        }

        Instant now = Instant.now();
        InviteToken token = InviteToken.builder()
                .id(UUID.randomUUID())
                .token(UUID.randomUUID())
                .inviterDundunId(dundunId)
                .expiresAt(now.plus(INVITE_TTL_HOURS, ChronoUnit.HOURS))
                .status(InviteStatus.PENDING)
                .createdAt(now)
                .build();
        return inviteTokenRepository.save(token);
    }

    /// 토큰 조회. 만료/소비되었으면 발견되지 않은 것으로 간주한다.
    @Transactional(readOnly = true)
    public InviteToken findValidToken(UUID token) {
        InviteToken invite = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "초대 토큰을 찾을 수 없습니다."));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "사용할 수 없는 토큰입니다.");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "만료된 토큰입니다.");
        }
        return invite;
    }

    /// 토큰 소비 + 자동 연동. 가입 직후 호출되어 신규 튼튼과 둔둔을 ACTIVE로 연결.
    /// 성공 시 둔둔이에게 INVITE_CONSUMED 알림.
    @Transactional
    @AuditLog(value = "INVITE_CONSUME", targetType = "invite")
    public InviteToken consume(UUID tokenValue, UUID newUserId) {
        InviteToken invite = findValidToken(tokenValue);

        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (newUser.getRole() != Role.TUNTUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "튼튼만 초대 토큰을 사용할 수 있습니다.");
        }

        Instant now = Instant.now();
        invite.setStatus(InviteStatus.CONSUMED);
        invite.setConsumedBy(newUserId);
        invite.setConsumedAt(now);
        inviteTokenRepository.save(invite);

        linkageFacade.linkImmediately(invite.getInviterDundunId(), newUserId, false);

        notificationDispatcher.notify(
                invite.getInviterDundunId(),
                NotificationType.INVITE_CONSUMED,
                "초대가 수락되었습니다",
                newUser.getName() + "님이 초대 링크로 가입하여 연동되었습니다.",
                Map.of("inviteId", invite.getId().toString(),
                        "tuntunId", newUserId.toString()));
        return invite;
    }
}
