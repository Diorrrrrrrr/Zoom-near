package kr.zoomnear.domain.point;

import jakarta.validation.Valid;
import java.util.List;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.point.dto.BalanceResponse;
import kr.zoomnear.domain.point.dto.LedgerItemResponse;
import kr.zoomnear.domain.point.dto.LedgerListResponse;
import kr.zoomnear.domain.point.dto.MockTopupResponse;
import kr.zoomnear.domain.point.dto.ProxyTopupRequest;
import kr.zoomnear.domain.point.dto.TopupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 포인트 진입점 (/api/v1/points/**).
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private static final int MAX_LEDGER_LIMIT = 100;

    private final MockTopupFacade mockTopupFacade;
    private final PointService pointService;
    private final PointLedgerRepository pointLedgerRepository;

    @PostMapping("/mock-topup")
    public ResponseEntity<MockTopupResponse> topupSelf(@Valid @RequestBody TopupRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        MockTopupResponse result = mockTopupFacade.topupSelf(principal.userId(), req.amount(), req.reasonText());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/mock-topup-proxy")
    public ResponseEntity<MockTopupResponse> topupProxy(@Valid @RequestBody ProxyTopupRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        MockTopupResponse result = mockTopupFacade.topupProxy(
                principal.userId(), req.tuntunId(), req.amount(), req.reasonText());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> myBalance() {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(new BalanceResponse(
                principal.userId(), pointService.getBalance(principal.userId())));
    }

    @GetMapping("/me/ledger")
    public ResponseEntity<LedgerListResponse> myLedger(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        AppPrincipal principal = AppPrincipal.current();
        int safeLimit = Math.min(MAX_LEDGER_LIMIT, Math.max(1, limit));
        int safeOffset = Math.max(0, offset);
        Pageable pageable = PageRequest.of(safeOffset / safeLimit, safeLimit);
        List<PointLedger> page = pointLedgerRepository.findByUserIdOrderByCreatedAtDesc(
                principal.userId(), pageable);
        long total = pointLedgerRepository.countByUserId(principal.userId());
        boolean hasMore = (long) safeOffset + page.size() < total;
        List<LedgerItemResponse> items = page.stream().map(LedgerItemResponse::from).toList();
        return ResponseEntity.ok(new LedgerListResponse(items, total, hasMore));
    }
}
