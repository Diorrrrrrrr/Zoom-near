package kr.zoomnear.domain.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PointService 단위 테스트.
 * JdbcTemplate을 Mock으로 대체하여 debit/credit/getBalance 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService — debit / credit / getBalance")
class PointServiceTest {

    @Mock JdbcTemplate jdbcTemplate;

    PointService pointService;

    @BeforeEach
    void setUp() {
        pointService = new PointService(jdbcTemplate);
    }

    // ── debit ──────────────────────────────────────────────

    @Test
    @DisplayName("debit — 잔액 충분 → 차감 후 새 잔액 반환")
    void debit_sufficient_balance_returns_new_balance() {
        when(jdbcTemplate.queryForObject(
                eq("SELECT debit_points(?, ?, ?, ?, ?)"),
                eq(BigDecimal.class),
                any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000));

        BigDecimal result = pointService.debit(
                TestFixtures.USER_TUNTUN_ID,
                BigDecimal.valueOf(5000),
                PointReason.EVENT_JOIN,
                "EVENT", TestFixtures.EVENT_ID.toString());

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("debit — 잔액 부족(DB 예외 insufficient_points) → INSUFFICIENT_POINTS")
    void debit_insufficient_balance_throws_business_exception() {
        DataAccessException dbEx = new org.springframework.jdbc.UncategorizedSQLException(
                "call failed", "SELECT", new java.sql.SQLException("insufficient_points"));
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(), any(), any(), any(), any()))
                .thenThrow(dbEx);

        assertThatThrownBy(() ->
                pointService.debit(
                        TestFixtures.USER_TUNTUN_ID,
                        BigDecimal.valueOf(99999),
                        PointReason.EVENT_JOIN,
                        "EVENT", TestFixtures.EVENT_ID.toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.INSUFFICIENT_POINTS);
    }

    @Test
    @DisplayName("debit — amount=0 → VALIDATION_FAILED")
    void debit_zero_amount_throws_validation_exception() {
        assertThatThrownBy(() ->
                pointService.debit(
                        TestFixtures.USER_TUNTUN_ID,
                        BigDecimal.ZERO,
                        PointReason.EVENT_JOIN,
                        "EVENT", "ref1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("debit — amount 음수 → VALIDATION_FAILED")
    void debit_negative_amount_throws_validation_exception() {
        assertThatThrownBy(() ->
                pointService.debit(
                        TestFixtures.USER_TUNTUN_ID,
                        BigDecimal.valueOf(-1),
                        PointReason.EVENT_JOIN,
                        "EVENT", "ref1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("debit — userId null → VALIDATION_FAILED")
    void debit_null_userId_throws_validation_exception() {
        assertThatThrownBy(() ->
                pointService.debit(
                        null,
                        BigDecimal.valueOf(1000),
                        PointReason.EVENT_JOIN,
                        "EVENT", "ref1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    // ── credit ─────────────────────────────────────────────

    @Test
    @DisplayName("credit — 정상 적립 → 새 잔액 반환")
    void credit_success_returns_new_balance() {
        when(jdbcTemplate.queryForObject(
                eq("SELECT credit_points(?, ?, ?, ?, ?)"),
                eq(BigDecimal.class),
                any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(15000));

        BigDecimal result = pointService.credit(
                TestFixtures.USER_TUNTUN_ID,
                BigDecimal.valueOf(5000),
                PointReason.MOCK_TOPUP,
                "MOCK_TOPUP", "REF-001");

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(15000));
    }

    @Test
    @DisplayName("credit — amount=0 → VALIDATION_FAILED")
    void credit_zero_amount_throws_validation_exception() {
        assertThatThrownBy(() ->
                pointService.credit(
                        TestFixtures.USER_TUNTUN_ID,
                        BigDecimal.ZERO,
                        PointReason.MOCK_TOPUP,
                        "MOCK_TOPUP", "REF-002"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    // ── 멱등성 처리 ────────────────────────────────────────

    @Test
    @DisplayName("동일 reference로 두 번 credit → 두 번째는 DB UNIQUE 예외 전파 (멱등성 보장은 DB)")
    void credit_duplicate_reference_propagates_db_exception() {
        // 첫 번째 호출 성공
        when(jdbcTemplate.queryForObject(
                eq("SELECT credit_points(?, ?, ?, ?, ?)"),
                eq(BigDecimal.class),
                any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000))  // 1번
                .thenThrow(new org.springframework.dao.DuplicateKeyException("unique violation")); // 2번

        // 첫 번째 호출
        BigDecimal firstResult = pointService.credit(
                TestFixtures.USER_TUNTUN_ID,
                BigDecimal.valueOf(5000),
                PointReason.MOCK_TOPUP,
                "MOCK_TOPUP", "FIXED-REF-ID-001");
        assertThat(firstResult).isEqualByComparingTo(BigDecimal.valueOf(5000));

        // 두 번째 동일 reference 호출 → UNIQUE 예외 전파
        // 실제 DB에서는 credit_points RPC 내부에서 ledger INSERT UNIQUE 위반 발생
        assertThatThrownBy(() ->
                pointService.credit(
                        TestFixtures.USER_TUNTUN_ID,
                        BigDecimal.valueOf(5000),
                        PointReason.MOCK_TOPUP,
                        "MOCK_TOPUP", "FIXED-REF-ID-001"))
                .isInstanceOf(DataAccessException.class);
    }

    // ── getBalance ─────────────────────────────────────────

    @Test
    @DisplayName("getBalance — wallet 존재 → 잔액 반환")
    void get_balance_existing_wallet_returns_balance() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any()))
                .thenReturn(BigDecimal.valueOf(10000));

        BigDecimal balance = pointService.getBalance(TestFixtures.USER_TUNTUN_ID);

        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("getBalance — wallet 미존재(COALESCE 0) → 0 반환")
    void get_balance_no_wallet_returns_zero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any()))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal balance = pointService.getBalance(TestFixtures.USER_TUNTUN_ID);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getBalance — DB null 반환(예외적) → 0으로 방어 처리")
    void get_balance_null_from_db_returns_zero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any()))
                .thenReturn(null);

        BigDecimal balance = pointService.getBalance(TestFixtures.USER_TUNTUN_ID);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
