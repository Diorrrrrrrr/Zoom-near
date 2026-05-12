package kr.zoomnear.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import kr.zoomnear.domain.profile.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// JWT 발급·파싱·검증을 담당하는 컴포넌트.
/// HS256 + application.yml의 zoomnear.jwt.secret을 사용한다.
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String TOKEN_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final String secret;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private SecretKey signingKey;

    public JwtTokenProvider(
            @Value("${zoomnear.jwt.secret}") String secret,
            @Value("${zoomnear.jwt.access-token-ttl-minutes:15}") long accessTtlMinutes,
            @Value("${zoomnear.jwt.refresh-token-ttl-days:14}") long refreshTtlDays) {
        this.secret = secret;
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("zoomnear.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /// 액세스 토큰 발급 (15분 TTL).
    public String issueAccess(UUID userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .claim(TOKEN_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /// 리프레시 토큰 발급 (14일 TTL).
    public String issueRefresh(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TOKEN_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }
}
