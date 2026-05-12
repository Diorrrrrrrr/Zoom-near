package kr.zoomnear.domain.point;

import java.math.BigDecimal;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/// 포인트 차감/적립 도메인 서비스. RPC(debit_points, credit_points)를 호출해
/// 잔액 변경과 ledger INSERT를 한 트랜잭션으로 보장한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final JdbcTemplate jdbcTemplate;

    /// 포인트 차감. 잔액 부족 시 INSUFFICIENT_POINTS BusinessException.
    /// ledger UNIQUE 제약으로 동일 reference 재시도 시 중복 INSERT가 차단된다.
    public BigDecimal debit(UUID userId, BigDecimal amount, PointReason reason, String refType, String refId) {
        validate(userId, amount);
        try {
            BigDecimal newBalance = jdbcTemplate.queryForObject(
                    "SELECT debit_points(?, ?, ?, ?, ?)",
                    BigDecimal.class,
                    userId, amount, reason.name(), refType, refId);
            log.debug("debit user={} amount={} reason={} new={}", userId, amount, reason, newBalance);
            return newBalance;
        } catch (DataAccessException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("insufficient_points")) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
            }
            throw ex;
        }
    }

    /// 포인트 적립. 신규 사용자라면 wallet INSERT, 기존이면 UPDATE.
    public BigDecimal credit(UUID userId, BigDecimal amount, PointReason reason, String refType, String refId) {
        validate(userId, amount);
        BigDecimal newBalance = jdbcTemplate.queryForObject(
                "SELECT credit_points(?, ?, ?, ?, ?)",
                BigDecimal.class,
                userId, amount, reason.name(), refType, refId);
        log.debug("credit user={} amount={} reason={} new={}", userId, amount, reason, newBalance);
        return newBalance;
    }

    /// 현재 지갑 잔액. wallet 미존재 시 0 반환.
    public BigDecimal getBalance(UUID userId) {
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT balance FROM point_wallets WHERE user_id = ?), 0)",
                BigDecimal.class,
                userId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private void validate(UUID userId, BigDecimal amount) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "userId가 비어있습니다.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "amount는 양수여야 합니다.");
        }
    }
}
