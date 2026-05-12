package kr.zoomnear.domain.linkage;

import kr.zoomnear.common.security.AppPrincipal;
import kr.zoomnear.domain.linkage.dto.UserSearchResponse;
import kr.zoomnear.domain.profile.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 6자리 코드로 연동 대상 사용자를 검색 (/api/v1/users/search).
/// 호출자 역할의 반대 역할 사용자만 노출. ADMIN/MANAGER 는 LinkageFacade 내부에서 차단.
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final LinkageFacade linkageFacade;

    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> searchByCode(@RequestParam("uniqueCode") String code) {
        AppPrincipal principal = AppPrincipal.current();
        User user = linkageFacade.searchByCode(principal.role(), code);
        return ResponseEntity.ok(new UserSearchResponse(
                user.getId(), user.getName(), user.getUniqueCode()));
    }
}
