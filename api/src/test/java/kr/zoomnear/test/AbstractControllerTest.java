package kr.zoomnear.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest 기반 컨트롤러 테스트의 공통 베이스 클래스.
 *
 * - addFilters=false 로 JWT 필터를 비활성화하여 SecurityContext 주입(@WithAppPrincipal)만 사용.
 * - MockMvc, ObjectMapper를 미리 주입받아 서브클래스에서 바로 사용 가능.
 * - perform/expectStatus/toJson 헬퍼 메서드 제공.
 *
 * 사용 예:
 * <pre>
 * {@literal @}WebMvcTest(controllers = AuthController.class)
 * class AuthControllerTest extends AbstractControllerTest {
 *     {@literal @}MockBean AuthFacade authFacade;
 *     ...
 * }
 * </pre>
 */
@AutoConfigureMockMvc(addFilters = false)
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── 헬퍼 메서드 ──────────────────────────────────────────

    /**
     * MockMvc perform 래퍼. 예외를 RuntimeException으로 변환.
     */
    protected ResultActions perform(MockHttpServletRequestBuilder request) {
        try {
            return mockMvc.perform(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 객체를 JSON 문자열로 직렬화한다.
     */
    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ResultActions에서 응답 body JSON을 추출한다.
     */
    protected String extractBody(ResultActions result) {
        try {
            return result.andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 응답 상태 코드를 검증한다. (체이닝 편의)
     */
    protected ResultActions expectStatus(ResultActions result, int expectedStatus) {
        try {
            return result.andExpect(status().is(expectedStatus));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
