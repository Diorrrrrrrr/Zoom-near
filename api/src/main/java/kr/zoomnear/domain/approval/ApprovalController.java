package kr.zoomnear.domain.approval;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.approval.dto.ApprovalListItem;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 승인 진입점 (/api/v1/approvals/**). 본인이 approver인 항목 조회/처리.
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ApprovalRepository approvalRepository;
    private final ApprovalFacade approvalFacade;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> mine(
            @RequestParam(required = false, defaultValue = "PENDING") ApprovalStatus status,
            @RequestParam(required = false) ApprovalType type,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        AppPrincipal principal = AppPrincipal.current();
        int safe = Math.min(MAX_LIMIT, Math.max(1, limit == null ? DEFAULT_LIMIT : limit));

        List<Approval> approvals = (type == null)
                ? approvalRepository.findByApproverIdAndStatusOrderByExpiresAtAsc(principal.userId(), status)
                : approvalRepository.findByApproverIdAndStatusAndTypeOrderByExpiresAtAsc(
                        principal.userId(), status, type);

        List<ApprovalListItem> items = approvals.stream()
                .limit(safe)
                .map(this::toListItem)
                .toList();
        return ResponseEntity.ok(Map.of("items", items, "total", items.size()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Approval> approve(@PathVariable("id") UUID approvalId) {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(approvalFacade.approve(approvalId, principal.userId()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Approval> reject(@PathVariable("id") UUID approvalId) {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(approvalFacade.reject(approvalId, principal.userId()));
    }

    private ApprovalListItem toListItem(Approval a) {
        User requester = userRepository.findById(a.getRequesterId()).orElse(null);
        String name = requester == null ? "알 수 없음" : requester.getName();
        String loginId = requester == null ? "" : requester.getLoginId();
        return new ApprovalListItem(
                a.getId(),
                a.getType(),
                a.getStatus(),
                a.getRequesterId(),
                name,
                loginId,
                summarizePayload(a.getPayload()),
                a.getExpiresAt(),
                a.getCreatedAt());
    }

    /// payload 에서 사람 친화적인 요약을 추출. 추출할 게 없으면 빈 문자열.
    /// LINKAGE_CREATE 처럼 내부 UUID 만 있는 경우 JSON 노출은 피한다.
    private String summarizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        Object title = payload.get("title");
        if (title != null) {
            return String.valueOf(title);
        }
        return "";
    }
}
