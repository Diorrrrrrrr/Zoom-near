package kr.zoomnear.common.security;

import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.linkage.LinkageRepository;
import kr.zoomnear.domain.linkage.LinkageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/// 둔둔-튼튼 연동 여부를 검사하는 가드.
/// 모든 대리 동작(proxy join, proxy topup, approval) 진입 즉시 호출한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkageGuard {

    private final LinkageRepository linkageRepository;

    /// 둔둔/튼튼 사이에 ACTIVE linkage가 존재하는지 검증한다. 없으면 NOT_LINKED.
    public void assertLinked(UUID dundunId, UUID tuntunId) {
        if (dundunId == null || tuntunId == null) {
            throw new BusinessException(ErrorCode.NOT_LINKED);
        }
        boolean linked = linkageRepository.existsByDundunIdAndTuntunIdAndStatus(
                dundunId, tuntunId, LinkageStatus.ACTIVE);
        if (!linked) {
            log.warn("LinkageGuard.assertLinked failed dundun={} tuntun={}", dundunId, tuntunId);
            throw new BusinessException(ErrorCode.NOT_LINKED);
        }
    }
}
