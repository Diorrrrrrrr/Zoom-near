package kr.zoomnear.domain.rank;

import java.util.List;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 랭크 산정 도메인 서비스. event_participations COMPLETED 카운트 → ranks 매핑.
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private static final String DEFAULT_RANK_CODE = "PPOJJAK";

    private final RankRepository rankRepository;
    private final UserRepository userRepository;
    private final EventParticipationRepository participationRepository;

    /// 완료된 이벤트 참여 수 → 랭크 코드 매핑.
    /// ranks 테이블의 min_count/max_count 범위 매칭. 매칭 실패 시 PPOJJAK.
    @Transactional(readOnly = true)
    public String calculateRank(int completedCount) {
        List<Rank> ranks = rankRepository.findAll();
        return ranks.stream()
                .filter(r -> completedCount >= r.getMinCount()
                        && (r.getMaxCount() == null || completedCount <= r.getMaxCount()))
                .map(Rank::getCode)
                .findFirst()
                .orElse(DEFAULT_RANK_CODE);
    }

    /// 사용자 랭크 재산정 후 users.rank_code 갱신. COMPLETED 전이 시점에 호출한다.
    @Transactional
    public void recomputeForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        long completed = participationRepository.countByParticipantIdAndStatus(
                userId, ParticipationStatus.COMPLETED);
        String newRank = calculateRank((int) completed);
        if (!newRank.equals(user.getRankCode())) {
            log.info("Rank changed user={} {} -> {} (completed={})",
                    userId, user.getRankCode(), newRank, completed);
            user.setRankCode(newRank);
            userRepository.save(user);
        }
    }

    /// 표시명 조회. 미존재 시 코드 그대로 반환.
    @Transactional(readOnly = true)
    public String displayName(String code) {
        if (code == null) {
            return null;
        }
        return rankRepository.findById(code).map(Rank::getDisplayName).orElse(code);
    }
}
