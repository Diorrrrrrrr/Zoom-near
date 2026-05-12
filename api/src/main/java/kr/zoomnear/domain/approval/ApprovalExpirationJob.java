package kr.zoomnear.domain.approval;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/// 만료 배치. 15분 간격으로 실행.
/// - approvals: PENDING + expires_at &lt; now → ApprovalFacade.expireOne 호출 (type별 후처리 포함)
/// - invite_tokens: PENDING + expires_at &lt; now → status='EXPIRED' 일괄 UPDATE
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalExpirationJob {

    private static final long FIXED_DELAY_MS = 15L * 60L * 1000L;

    private final ApprovalRepository approvalRepository;
    private final ApprovalFacade approvalFacade;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = 60_000L)
    public void runExpirationSweep() {
        Instant now = Instant.now();
        List<Approval> expired = approvalRepository.findByStatusAndExpiresAtBefore(
                ApprovalStatus.PENDING, now);
        if (!expired.isEmpty()) {
            log.info("ApprovalExpirationJob: expiring {} approvals", expired.size());
        }
        for (Approval a : expired) {
            try {
                approvalFacade.expireOne(a.getId());
            } catch (Exception ex) {
                log.warn("Failed to expire approval id={}", a.getId(), ex);
            }
        }

        try {
            // PG JDBC 드라이버가 Instant 의 SQL 타입을 추론하지 못해 Timestamp 로 명시 변환.
            int inviteUpdated = jdbcTemplate.update(
                    "UPDATE invite_tokens SET status = 'EXPIRED' "
                            + "WHERE status = 'PENDING' AND expires_at < ?",
                    Timestamp.from(Instant.now()));
            if (inviteUpdated > 0) {
                log.info("ApprovalExpirationJob: expired invite_tokens={} ", inviteUpdated);
            }
        } catch (Exception ex) {
            log.warn("Failed to expire invite tokens", ex);
        }
    }
}
