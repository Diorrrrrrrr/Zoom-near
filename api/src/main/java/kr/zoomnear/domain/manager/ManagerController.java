package kr.zoomnear.domain.manager;

import jakarta.validation.Valid;
import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.manager.dto.ManagerApplyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 매니저 신청 진입점.
/// - 사용자: POST /api/v1/manager/apply
/// - 관리자: GET/POST /api/v1/admin/manager-applications/**
@RestController
@RequiredArgsConstructor
public class ManagerController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ManagerFacade managerFacade;
    private final ManagerApplicationRepository repository;

    @PostMapping("/api/v1/manager/apply")
    public ResponseEntity<ManagerApplication> apply(@Valid @RequestBody ManagerApplyRequest req) {
        AppPrincipal principal = AppPrincipal.current();
        ManagerApplication saved = managerFacade.apply(principal.userId(), req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/v1/admin/manager-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ManagerApplication>> list(
            @RequestParam(required = false, defaultValue = "PENDING") ManagerApplicationStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(repository.findByStatus(status, pageable));
    }

    @PostMapping("/api/v1/admin/manager-applications/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManagerApplication> approve(@PathVariable("id") UUID id) {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(managerFacade.approve(id, principal.userId()));
    }

    @PostMapping("/api/v1/admin/manager-applications/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManagerApplication> reject(@PathVariable("id") UUID id) {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(managerFacade.reject(id, principal.userId()));
    }
}
