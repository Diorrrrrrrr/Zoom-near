package kr.zoomnear.domain.notification;

import java.util.UUID;
import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.notification.dto.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 알림 진입점 (/api/v1/notifications/**).
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationFacade notificationFacade;

    @GetMapping
    public ResponseEntity<NotificationListResponse> list(
            @RequestParam(required = false) Boolean unread,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        AppPrincipal principal = AppPrincipal.current();
        return ResponseEntity.ok(notificationFacade.list(principal.userId(), unread, limit, offset));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable("id") UUID notificationId) {
        AppPrincipal principal = AppPrincipal.current();
        notificationFacade.markRead(notificationId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
