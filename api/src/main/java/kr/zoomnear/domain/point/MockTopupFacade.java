package kr.zoomnear.domain.point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.audit.AuditLog;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.LinkageGuard;
import kr.zoomnear.domain.point.dto.MockTopupResponse;
import kr.zoomnear.infra.notification.NotificationDispatcher;
import kr.zoomnear.infra.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 모의 충전 도메인 파사드. mock_topups INSERT + PointService.credit을 한 트랜잭션에서 수행.
@Slf4j
@Service
@RequiredArgsConstructor
public class MockTopupFacade {

    private static final String DEFAULT_SELF_REASON = "본인 충전";
    private static final String DEFAULT_PROXY_REASON = "대리 충전";
    private static final String REF_TYPE_TOPUP = "MOCK_TOPUP";

    private final MockTopupRepository mockTopupRepository;
    private final PointService pointService;
    private final LinkageGuard linkageGuard;
    private final NotificationDispatcher notificationDispatcher;

    /// 본인 충전.
    @Transactional
    @AuditLog("MOCK_TOPUP_SELF")
    public MockTopupResponse topupSelf(UUID userId, BigDecimal amount, String reasonText) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "amount는 양수여야 합니다.");
        }
        MockTopup topup = MockTopup.builder()
                .userId(userId)
                .chargedBy(userId)
                .amount(amount)
                .reasonText(blankToDefault(reasonText, DEFAULT_SELF_REASON))
                .createdAt(Instant.now())
                .build();
        MockTopup saved = mockTopupRepository.saveAndFlush(topup);
        BigDecimal newBalance = pointService.credit(
                userId, amount, PointReason.MOCK_TOPUP, REF_TYPE_TOPUP, saved.getId().toString());
        return new MockTopupResponse(
                newBalance, saved.getId(), saved.getAmount(), saved.getUserId(), saved.getCreatedAt());
    }

    /// 대리 충전. linkageGuard를 진입 즉시 호출하여 미연동 호출을 차단한다.
    @Transactional
    @AuditLog("MOCK_TOPUP_PROXY")
    public MockTopupResponse topupProxy(UUID dundunId, UUID tuntunId, BigDecimal amount, String reasonText) {
        linkageGuard.assertLinked(dundunId, tuntunId);
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "amount는 양수여야 합니다.");
        }
        MockTopup topup = MockTopup.builder()
                .userId(tuntunId)
                .chargedBy(dundunId)
                .amount(amount)
                .reasonText(blankToDefault(reasonText, DEFAULT_PROXY_REASON))
                .createdAt(Instant.now())
                .build();
        MockTopup saved = mockTopupRepository.saveAndFlush(topup);
        BigDecimal newBalance = pointService.credit(
                tuntunId, amount, PointReason.MOCK_TOPUP, REF_TYPE_TOPUP, saved.getId().toString());
        notificationDispatcher.notify(
                tuntunId,
                NotificationType.MOCK_TOPUP_RECEIVED,
                "포인트가 충전되었습니다",
                "둔둔이 " + amount.toPlainString() + "P를 대신 충전했습니다.",
                Map.of("topupId", saved.getId().toString(),
                        "amount", amount.toPlainString()));
        return new MockTopupResponse(
                newBalance, saved.getId(), saved.getAmount(), saved.getUserId(), saved.getCreatedAt());
    }

    private String blankToDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
