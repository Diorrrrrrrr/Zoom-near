package kr.zoomnear.domain.linkage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.common.security.LinkageGuard;
import kr.zoomnear.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LinkageGuard 단위 테스트.
 * 연동 여부 검사 로직을 격리해서 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LinkageGuard — 연동 검증")
class LinkageGuardTest {

    @Mock LinkageRepository linkageRepository;

    LinkageGuard linkageGuard;

    @BeforeEach
    void setUp() {
        linkageGuard = new LinkageGuard(linkageRepository);
    }

    @Test
    @DisplayName("연동된 dundun-tuntun → assertLinked 예외 없음 통과")
    void assertLinked_active_linkage_passes() {
        when(linkageRepository.existsByDundunIdAndTuntunIdAndStatus(
                TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(true);

        // 예외 없이 통과해야 한다
        assertThatCode(() ->
                linkageGuard.assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비연동(ACTIVE 없음) → BusinessException(NOT_LINKED)")
    void assertLinked_no_active_linkage_throws_not_linked() {
        when(linkageRepository.existsByDundunIdAndTuntunIdAndStatus(
                TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() ->
                linkageGuard.assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_LINKED);
    }

    @Test
    @DisplayName("dundunId null → BusinessException(NOT_LINKED)")
    void assertLinked_null_dundun_id_throws_not_linked() {
        assertThatThrownBy(() ->
                linkageGuard.assertLinked(null, TestFixtures.USER_TUNTUN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_LINKED);
    }

    @Test
    @DisplayName("tuntunId null → BusinessException(NOT_LINKED)")
    void assertLinked_null_tuntun_id_throws_not_linked() {
        assertThatThrownBy(() ->
                linkageGuard.assertLinked(TestFixtures.USER_DUNDUN_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_LINKED);
    }

    @Test
    @DisplayName("양쪽 null → BusinessException(NOT_LINKED)")
    void assertLinked_both_null_throws_not_linked() {
        assertThatThrownBy(() -> linkageGuard.assertLinked(null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_LINKED);
    }

    @Test
    @DisplayName("INACTIVE 연동은 ACTIVE와 다르므로 NOT_LINKED")
    void assertLinked_inactive_linkage_throws_not_linked() {
        // ACTIVE가 없으면 false 반환
        when(linkageRepository.existsByDundunIdAndTuntunIdAndStatus(
                TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID, LinkageStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() ->
                linkageGuard.assertLinked(TestFixtures.USER_DUNDUN_ID, TestFixtures.USER_TUNTUN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_LINKED);
    }
}
