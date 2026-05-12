package kr.zoomnear.domain.profile;

import jakarta.validation.Valid;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.profile.dto.ChangePasswordRequest;
import kr.zoomnear.domain.profile.dto.MeResponse;
import kr.zoomnear.domain.profile.dto.RoleSwitchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 본인 프로필 진입점 (/api/v1/me/**).
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileFacade profileFacade;
    private final RoleSwitchFacade roleSwitchFacade;

    @GetMapping
    public ResponseEntity<MeResponse> me() {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(profileFacade.me(principal.userId()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        profileFacade.changePassword(principal.userId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/role-switch")
    public ResponseEntity<MeResponse> switchRole(@Valid @RequestBody RoleSwitchRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        roleSwitchFacade.switchRole(principal.userId(), req.newRole());
        return ResponseEntity.ok(profileFacade.me(principal.userId()));
    }
}
