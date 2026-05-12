package kr.zoomnear.domain.linkage;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.approval.Approval;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.approval.ApprovalType;
import kr.zoomnear.domain.linkage.dto.LinkageListItem;
import kr.zoomnear.domain.linkage.dto.LinkageRequest;
import kr.zoomnear.domain.linkage.dto.PendingLinkageItem;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 연동 진입점 (/api/v1/linkages/**). ADMIN/MANAGER 는 모든 엔드포인트에서 차단.
@RestController
@RequestMapping("/api/v1/linkages")
@RequiredArgsConstructor
public class LinkageController {

    private final LinkageFacade linkageFacade;
    private final LinkageRepository linkageRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final ApprovalRepository approvalRepository;

    @GetMapping("/me")
    public ResponseEntity<List<LinkageListItem>> myLinkages() {
        AppPrincipal principal = AppPrincipal.current();
        Role role = principal.role();
        if (role != Role.TUNTUN && role != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "연동 정보는 튼튼이/든든이만 조회할 수 있습니다.");
        }

        List<Linkage> linkages = switch (role) {
            case DUNDUN -> linkageRepository.findByDundunIdAndStatus(principal.userId(), LinkageStatus.ACTIVE);
            case TUNTUN -> linkageRepository.findByTuntunIdAndStatus(principal.userId(), LinkageStatus.ACTIVE);
            default -> Collections.emptyList();
        };

        List<LinkageListItem> items = linkages.stream()
                .map(l -> toItem(l, role))
                .toList();
        return ResponseEntity.ok(items);
    }

    /// 내가 보낸 대기중 연동 요청 목록. 호출자 = requester.
    /// ADMIN/MANAGER 는 거부.
    @GetMapping("/me/pending-outgoing")
    public ResponseEntity<List<PendingLinkageItem>> myPendingOutgoing() {
        AppPrincipal principal = AppPrincipal.current();
        Role role = principal.role();
        if (role != Role.TUNTUN && role != Role.DUNDUN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "연동 정보는 튼튼이/든든이만 조회할 수 있습니다.");
        }
        List<Approval> pending = approvalRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(principal.userId(), ApprovalStatus.PENDING)
                .stream()
                .filter(a -> a.getType() == ApprovalType.LINKAGE_CREATE)
                .toList();

        List<PendingLinkageItem> items = pending.stream().map(a -> {
            UUID otherId = a.getApproverId();
            User other = userRepository.findById(otherId).orElse(null);
            return new PendingLinkageItem(
                    a.getId(),
                    otherId,
                    other == null ? "" : other.getName(),
                    other == null ? "" : other.getLoginId(),
                    other == null ? "" : other.getUniqueCode(),
                    a.getCreatedAt(),
                    a.getExpiresAt());
        }).toList();
        return ResponseEntity.ok(items);
    }

    /// 연동 요청 — 즉시 linkage 가 생기지 않고 상대에게 LINKAGE_CREATE Approval 이 전달된다.
    @PostMapping
    public ResponseEntity<Approval> requestLink(@Valid @RequestBody LinkageRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        Approval approval = linkageFacade.requestLink(
                principal.userId(), principal.role(), req.tuntunId(), req.isPrimary());
        return ResponseEntity.status(HttpStatus.CREATED).body(approval);
    }

    @DeleteMapping("/{linkageId}")
    public ResponseEntity<Void> unlink(@PathVariable UUID linkageId) {
        AppPrincipal principal = AppPrincipal.current();
        linkageFacade.unlink(linkageId, principal.userId(), principal.role());
        return ResponseEntity.noContent().build();
    }

    private LinkageListItem toItem(Linkage l, Role currentRole) {
        UUID otherUserId = (currentRole == Role.DUNDUN) ? l.getTuntunId() : l.getDundunId();
        User other = userRepository.findById(otherUserId).orElse(null);
        String name = other == null ? "" : other.getName();
        String loginId = other == null ? "" : other.getLoginId();
        String uniqueCode = other == null ? "" : other.getUniqueCode();
        // 정책: 든든이가 자신과 연동된 튼튼이의 잔액을 조회하는 케이스만 노출.
        BigDecimal balance = currentRole == Role.DUNDUN && other != null
                ? pointService.getBalance(otherUserId)
                : null;
        return new LinkageListItem(
                l.getId(),
                otherUserId,
                name,
                loginId,
                uniqueCode,
                balance,
                l.isPrimary(),
                currentRole.name());
    }
}
