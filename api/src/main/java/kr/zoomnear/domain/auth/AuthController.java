package kr.zoomnear.domain.auth;

import jakarta.validation.Valid;
import kr.zoomnear.domain.auth.dto.LoginRequest;
import kr.zoomnear.domain.auth.dto.RefreshRequest;
import kr.zoomnear.domain.auth.dto.SignupRequest;
import kr.zoomnear.domain.auth.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 인증 진입점 (/api/v1/auth/**). 가입과 로그인을 노출한다.
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest req) {
        TokenResponse body = authFacade.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authFacade.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authFacade.refresh(req.refreshToken()));
    }
}
