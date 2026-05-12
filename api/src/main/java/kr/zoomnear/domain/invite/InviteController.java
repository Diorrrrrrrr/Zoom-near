package kr.zoomnear.domain.invite;

import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.invite.dto.InviteResponse;
import kr.zoomnear.domain.profile.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 초대 진입점 (/api/v1/invites/**).
@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {

    private final InviteFacade inviteFacade;
    private final UserRepository userRepository;
    private final String inviteBaseUrl;

    public InviteController(InviteFacade inviteFacade,
                            UserRepository userRepository,
                            @Value("${zoomnear.invite.base-url:https://zoomnear.kr/invite}") String inviteBaseUrl) {
        this.inviteFacade = inviteFacade;
        this.userRepository = userRepository;
        this.inviteBaseUrl = inviteBaseUrl;
    }

    @PostMapping
    public ResponseEntity<InviteResponse> create() {
        AppPrincipal principal = AppPrincipal.current();
        InviteToken token = inviteFacade.create(principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(token));
    }

    @GetMapping("/{token}")
    public ResponseEntity<InviteResponse> describe(@PathVariable UUID token) {
        InviteToken invite = inviteFacade.findValidToken(token);
        return ResponseEntity.ok(toResponse(invite));
    }

    private InviteResponse toResponse(InviteToken invite) {
        String inviterName = userRepository.findById(invite.getInviterDundunId())
                .map(u -> u.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new InviteResponse(
                invite.getId(),
                invite.getToken(),
                invite.getInviterDundunId(),
                inviterName,
                invite.getExpiresAt(),
                invite.getStatus().name(),
                inviteBaseUrl + "?token=" + invite.getToken());
    }
}
