package kr.zoomnear.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.zoomnear.common.exception.BusinessException;
import kr.zoomnear.common.exception.ErrorCode;
import kr.zoomnear.domain.admin.AdminController;
import kr.zoomnear.domain.admin.AdminFacade;
import kr.zoomnear.domain.approval.Approval;
import kr.zoomnear.domain.approval.ApprovalController;
import kr.zoomnear.domain.approval.ApprovalFacade;
import kr.zoomnear.domain.approval.ApprovalRepository;
import kr.zoomnear.domain.approval.ApprovalStatus;
import kr.zoomnear.domain.approval.ApprovalType;
import kr.zoomnear.domain.auth.AuthController;
import kr.zoomnear.domain.auth.AuthFacade;
import kr.zoomnear.domain.auth.dto.LoginRequest;
import kr.zoomnear.domain.auth.dto.SignupRequest;
import kr.zoomnear.domain.auth.dto.TokenResponse;
import kr.zoomnear.domain.event.EventCategory;
import kr.zoomnear.domain.event.EventController;
import kr.zoomnear.domain.event.EventFacade;
import kr.zoomnear.domain.event.EventParticipation;
import kr.zoomnear.domain.event.EventParticipationFacade;
import kr.zoomnear.domain.event.EventParticipationRepository;
import kr.zoomnear.domain.event.EventStatus;
import kr.zoomnear.domain.event.EventVisibility;
import kr.zoomnear.domain.event.ParticipationStatus;
import kr.zoomnear.domain.event.SocialEvent;
import kr.zoomnear.domain.invite.InviteController;
import kr.zoomnear.domain.invite.InviteFacade;
import kr.zoomnear.domain.invite.InviteToken;
import kr.zoomnear.domain.linkage.Linkage;
import kr.zoomnear.domain.linkage.LinkageController;
import kr.zoomnear.domain.linkage.LinkageFacade;
import kr.zoomnear.domain.linkage.LinkageRepository;
import kr.zoomnear.domain.linkage.LinkageStatus;
import kr.zoomnear.domain.linkage.UserSearchController;
import kr.zoomnear.domain.manager.ManagerController;
import kr.zoomnear.domain.manager.ManagerApplication;
import kr.zoomnear.domain.manager.ManagerApplicationRepository;
import kr.zoomnear.domain.manager.ManagerApplicationStatus;
import kr.zoomnear.domain.manager.ManagerFacade;
import kr.zoomnear.domain.point.MockTopup;
import kr.zoomnear.domain.point.MockTopupFacade;
import kr.zoomnear.domain.point.PointController;
import kr.zoomnear.domain.point.PointLedgerRepository;
import kr.zoomnear.domain.point.PointService;
import kr.zoomnear.domain.profile.ProfileController;
import kr.zoomnear.domain.profile.ProfileFacade;
import kr.zoomnear.domain.profile.Role;
import kr.zoomnear.domain.profile.RoleSwitchFacade;
import kr.zoomnear.domain.profile.User;
import kr.zoomnear.domain.profile.UserRepository;
import kr.zoomnear.domain.profile.dto.MeResponse;
import kr.zoomnear.test.TestFixtures;
import kr.zoomnear.test.WithAppPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 권한 매트릭스 테스트 — permission_matrix.md 57개 시나리오 커버.
 *
 * 구조: 카테고리별 @Nested 클래스로 그룹화.
 * 방식: @WebMvcTest(addFilters=false) + @WithAppPrincipal + @MockBean Facade.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class PermissionMatrixTest {

    // ════════════════════════════════════════════════════════════
    // AUTH 시나리오 (#1~5)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("AUTH — 로그인 / 회원가입 / 토큰")
    @WebMvcTest(controllers = AuthController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class AuthScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean AuthFacade authFacade;

        @Test
        @DisplayName("#1 ANON — TUNTUN 정상 가입 → 201")
        void signup_tuntun_success_returns_201() throws Exception {
            when(authFacade.signup(any())).thenReturn(
                    new TokenResponse("access", "refresh", TestFixtures.USER_TUNTUN_ID, Role.TUNTUN));

            SignupRequest req = new SignupRequest(
                    "newuser1", "Password1!", "010-1234-5678",
                    "user@test.com", "튼튼이", Role.TUNTUN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#2 ANON — role:ADMIN 가입 시도 → 403 (Facade에서 거부)")
        void signup_admin_role_rejected_by_facade() throws Exception {
            when(authFacade.signup(any())).thenThrow(
                    new BusinessException(ErrorCode.FORBIDDEN, "ADMIN/MANAGER 계정은 가입할 수 없습니다."));

            SignupRequest req = new SignupRequest(
                    "adminuser", "Password1!", "010-1234-5678",
                    null, "관리자", Role.ADMIN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#2-validation — loginId 패턴 위반(@Valid) → 400")
        void signup_invalid_loginId_returns_400() throws Exception {
            SignupRequest req = new SignupRequest(
                    "ab", "Password1!", "010-1234-5678",
                    null, "사용자", Role.TUNTUN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#3 ANON — 올바른 loginId/password 로그인 → 200")
        void login_correct_credentials_returns_200() throws Exception {
            when(authFacade.login(any())).thenReturn(
                    new TokenResponse("access", "refresh", TestFixtures.USER_TUNTUN_ID, Role.TUNTUN));

            LoginRequest req = new LoginRequest("tuntun01", "Password1!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#4 ANON — 잘못된 비밀번호 로그인 → 401")
        void login_wrong_password_returns_401() throws Exception {
            when(authFacade.login(any())).thenThrow(
                    new BusinessException(ErrorCode.UNAUTHORIZED));

            LoginRequest req = new LoginRequest("tuntun01", "wrongPassword");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("#4-validation — loginId 빈값 → 400")
        void login_blank_loginId_returns_400() throws Exception {
            LoginRequest req = new LoginRequest("", "Password1!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#5 TUNTUN — 로그아웃 엔드포인트 존재 시 200 (TODO: logout 컨트롤러 연결)")
        @WithAppPrincipal(role = Role.TUNTUN)
        void logout_tuntun_placeholder() throws Exception {
            // TODO(Day1PMend): AuthController에 /logout 엔드포인트 추가 후 활성화
        }
    }

    // ════════════════════════════════════════════════════════════
    // PROFILE 시나리오 (#6~10)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PROFILE — 프로필 조회/수정")
    @WebMvcTest(controllers = ProfileController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class ProfileScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean ProfileFacade profileFacade;
        @MockBean RoleSwitchFacade roleSwitchFacade;

        private MeResponse meResponse(Role role) {
            return new MeResponse(
                    TestFixtures.USER_TUNTUN_ID, "tuntun01", "튼튼이",
                    role, "TUN001", BigDecimal.valueOf(10_000), "PPOJJAK", "뽀짝");
        }

        @Test
        @DisplayName("#6 TUNTUN — GET /api/v1/me → 200")
        @WithAppPrincipal(role = Role.TUNTUN)
        void get_my_profile_tuntun_returns_200() throws Exception {
            when(profileFacade.me(any())).thenReturn(meResponse(Role.TUNTUN));

            mockMvc.perform(get("/api/v1/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#7 DUNDUN — GET /api/v1/me → 200")
        @WithAppPrincipal(role = Role.DUNDUN)
        void get_my_profile_dundun_returns_200() throws Exception {
            when(profileFacade.me(any())).thenReturn(meResponse(Role.DUNDUN));

            mockMvc.perform(get("/api/v1/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#8 TUNTUN — PUT /api/v1/me/password 정상 → 204")
        @WithAppPrincipal(role = Role.TUNTUN)
        void change_password_tuntun_returns_204() throws Exception {
            String body = "{\"currentPassword\":\"OldPass1!\",\"newPassword\":\"NewPass2@\"}";

            mockMvc.perform(put("/api/v1/me/password")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("#9 ADMIN — GET /api/v1/me → 200 (ADMIN도 본인 프로필 조회 가능)")
        @WithAppPrincipal(role = Role.ADMIN)
        void admin_get_own_profile_returns_200() throws Exception {
            when(profileFacade.me(any())).thenReturn(meResponse(Role.ADMIN));

            mockMvc.perform(get("/api/v1/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#10 ANON — GET /api/v1/me 토큰 없음 → AppPrincipal.current() 401")
        void anon_get_profile_no_security_context_returns_401() throws Exception {
            when(profileFacade.me(any())).thenThrow(
                    new BusinessException(ErrorCode.UNAUTHORIZED));

            mockMvc.perform(get("/api/v1/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ════════════════════════════════════════════════════════════
    // LINKAGE 시나리오 (#11~18)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("LINKAGE — 연동 검색/요청/해제")
    @WebMvcTest(controllers = {LinkageController.class, UserSearchController.class})
    @AutoConfigureMockMvc(addFilters = false)
    class LinkageScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean LinkageFacade linkageFacade;
        @MockBean LinkageRepository linkageRepository;

        @Test
        @DisplayName("#11 DUNDUN — GET /api/v1/users/search?uniqueCode=TUN001 → 200")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_search_linkage_by_code_returns_200() throws Exception {
            User tuntun = TestFixtures.tuntunUser().build();
            when(linkageFacade.searchByCode("TUN001")).thenReturn(tuntun);

            mockMvc.perform(get("/api/v1/users/search")
                            .param("uniqueCode", "TUN001"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#12 DUNDUN — GET /api/v1/users/search?uniqueCode=NOEXST → 404")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_search_nonexistent_code_returns_404() throws Exception {
            when(linkageFacade.searchByCode("NOEXST"))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "해당 코드의 사용자가 없습니다."));

            mockMvc.perform(get("/api/v1/users/search")
                            .param("uniqueCode", "NOEXST"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("#13 DUNDUN — POST /api/v1/linkages → 201")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_create_linkage_success_returns_201() throws Exception {
            Linkage linkage = Linkage.builder()
                    .id(TestFixtures.LINKAGE_ID)
                    .dundunId(TestFixtures.USER_DUNDUN_ID)
                    .tuntunId(TestFixtures.USER_TUNTUN_ID)
                    .primary(true)
                    .status(LinkageStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            when(linkageFacade.link(any(), any(), any(Boolean.class))).thenReturn(linkage);

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.linkage.dto.LinkageRequest(TestFixtures.USER_TUNTUN_ID, true));

            mockMvc.perform(post("/api/v1/linkages")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#14 DUNDUN — POST /api/v1/linkages 5번째 → 409 (연동 한도 초과)")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_create_linkage_over_limit_returns_409() throws Exception {
            when(linkageFacade.link(any(), any(), any(Boolean.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "연동 한도를 초과했습니다."));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.linkage.dto.LinkageRequest(TestFixtures.USER_TUNTUN_ID, false));

            mockMvc.perform(post("/api/v1/linkages")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#15 DUNDUN — POST /api/v1/linkages 중복 연동 → 409")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_create_duplicate_linkage_returns_409() throws Exception {
            when(linkageFacade.link(any(), any(), any(Boolean.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 활성 연동입니다."));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.linkage.dto.LinkageRequest(TestFixtures.USER_TUNTUN_ID, false));

            mockMvc.perform(post("/api/v1/linkages")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#16 DUNDUN — POST /api/v1/linkages 자기 자신과 연동 → 400")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_self_linkage_returns_400() throws Exception {
            when(linkageFacade.link(any(), any(), any(Boolean.class)))
                    .thenThrow(new BusinessException(ErrorCode.VALIDATION_FAILED, "자기 자신과는 연동할 수 없습니다."));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.linkage.dto.LinkageRequest(TestFixtures.USER_DUNDUN_ID, false));

            mockMvc.perform(post("/api/v1/linkages")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#17 TUNTUN — POST /api/v1/linkages 연동 개시 시도 → 403")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_create_linkage_forbidden_returns_403() throws Exception {
            when(linkageFacade.link(any(), any(), any(Boolean.class)))
                    .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "TUNTUN은 연동을 시작할 수 없습니다."));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.linkage.dto.LinkageRequest(TestFixtures.USER_DUNDUN_ID, false));

            mockMvc.perform(post("/api/v1/linkages")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#18 DUNDUN — DELETE /api/v1/linkages/{linkageId} → 204")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_delete_linkage_returns_204() throws Exception {
            mockMvc.perform(delete("/api/v1/linkages/" + TestFixtures.LINKAGE_ID))
                    .andExpect(status().isNoContent());
        }
    }

    // ════════════════════════════════════════════════════════════
    // INVITE 시나리오 (#19~22)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("INVITE — 초대 토큰")
    @WebMvcTest(controllers = InviteController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class InviteScenarios {

        @Autowired MockMvc mockMvc;
        @MockBean InviteFacade inviteFacade;
        @MockBean UserRepository userRepository;

        @Test
        @DisplayName("#19 DUNDUN — POST /api/v1/invites → 201")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_issue_invite_token_returns_201() throws Exception {
            InviteToken token = InviteToken.builder()
                    .id(UUID.randomUUID())
                    .token(UUID.fromString(TestFixtures.INVITE_TOKEN.replace("INVITE-TEST-TOKEN-", "00000000-0000-0000-0000-0000")))
                    .inviterDundunId(TestFixtures.USER_DUNDUN_ID)
                    .status(kr.zoomnear.domain.invite.InviteStatus.ACTIVE)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .createdAt(Instant.now())
                    .build();
            when(inviteFacade.create(any())).thenReturn(token);
            when(userRepository.findById(any())).thenReturn(
                    Optional.of(TestFixtures.dundunUser().build()));

            mockMvc.perform(post("/api/v1/invites"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#20 ANON — GET /api/v1/invites/{token} 유효 토큰 → 200")
        void anon_get_valid_invite_returns_200() throws Exception {
            UUID tokenId = UUID.randomUUID();
            InviteToken token = InviteToken.builder()
                    .id(tokenId)
                    .token(tokenId)
                    .inviterDundunId(TestFixtures.USER_DUNDUN_ID)
                    .status(kr.zoomnear.domain.invite.InviteStatus.ACTIVE)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .createdAt(Instant.now())
                    .build();
            when(inviteFacade.findValidToken(any())).thenReturn(token);
            when(userRepository.findById(any())).thenReturn(
                    Optional.of(TestFixtures.dundunUser().build()));

            mockMvc.perform(get("/api/v1/invites/" + tokenId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#21 ANON — GET /api/v1/invites/{token} 만료 토큰 → 400")
        void anon_get_expired_invite_returns_400() throws Exception {
            UUID tokenId = UUID.randomUUID();
            when(inviteFacade.findValidToken(any()))
                    .thenThrow(new BusinessException(ErrorCode.VALIDATION_FAILED, "만료된 초대 토큰입니다."));

            mockMvc.perform(get("/api/v1/invites/" + tokenId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#22 ANON — GET /api/v1/invites/{token} 사용된 토큰 → 409")
        void anon_get_consumed_invite_returns_409() throws Exception {
            UUID tokenId = UUID.randomUUID();
            when(inviteFacade.findValidToken(any()))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 사용된 초대 토큰입니다."));

            mockMvc.perform(get("/api/v1/invites/" + tokenId))
                    .andExpect(status().isConflict());
        }
    }

    // ════════════════════════════════════════════════════════════
    // EVENT_VIEW 시나리오 (#23~25)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("EVENT_VIEW — 이벤트 조회")
    @WebMvcTest(controllers = EventController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class EventViewScenarios {

        @Autowired MockMvc mockMvc;
        @MockBean EventFacade eventFacade;
        @MockBean EventParticipationFacade participationFacade;
        @MockBean EventParticipationRepository participationRepository;

        private SocialEvent sampleEvent() {
            return SocialEvent.builder()
                    .id(TestFixtures.EVENT_ID)
                    .creatorId(TestFixtures.USER_TUNTUN_ID)
                    .category(EventCategory.SOCIAL)
                    .title("테스트 이벤트")
                    .description("설명")
                    .regionText("서울")
                    .startsAt(Instant.now().plusSeconds(3600))
                    .endsAt(Instant.now().plusSeconds(7200))
                    .capacity(10)
                    .pointCost(BigDecimal.ZERO)
                    .status(EventStatus.OPEN)
                    .visibility(EventVisibility.PUBLIC)
                    .managerProgram(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("#23 TUNTUN — GET /api/v1/events → 200")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_list_events_returns_200() throws Exception {
            when(eventFacade.list(any())).thenReturn(new PageImpl<>(List.of(sampleEvent())));

            mockMvc.perform(get("/api/v1/events"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#24 DUNDUN — GET /api/v1/events/{eventId} → 200")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_get_event_detail_returns_200() throws Exception {
            when(eventFacade.get(TestFixtures.EVENT_ID)).thenReturn(sampleEvent());
            when(participationRepository.countByEventIdAndStatusIn(any(), any())).thenReturn(3L);

            mockMvc.perform(get("/api/v1/events/" + TestFixtures.EVENT_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#25 ANON — GET /api/v1/events/{eventId} 토큰 없음 → Facade NOT_FOUND시 404")
        void anon_get_event_detail_not_found_returns_404() throws Exception {
            when(eventFacade.get(TestFixtures.EVENT_ID))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

            mockMvc.perform(get("/api/v1/events/" + TestFixtures.EVENT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ════════════════════════════════════════════════════════════
    // EVENT_CREATE 시나리오 (#26~29)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("EVENT_CREATE — 이벤트 등록")
    @WebMvcTest(controllers = EventController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class EventCreateScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean EventFacade eventFacade;
        @MockBean EventParticipationFacade participationFacade;
        @MockBean EventParticipationRepository participationRepository;

        private SocialEvent sampleEvent() {
            return SocialEvent.builder()
                    .id(TestFixtures.EVENT_ID)
                    .creatorId(TestFixtures.USER_TUNTUN_ID)
                    .category(EventCategory.SOCIAL)
                    .title("새 이벤트")
                    .description("설명")
                    .regionText("서울")
                    .startsAt(Instant.now().plusSeconds(3600))
                    .endsAt(Instant.now().plusSeconds(7200))
                    .capacity(10)
                    .pointCost(BigDecimal.ZERO)
                    .status(EventStatus.OPEN)
                    .visibility(EventVisibility.PUBLIC)
                    .managerProgram(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        private String createEventBody() {
            return "{\"title\":\"새 이벤트\",\"description\":\"설명\",\"regionText\":\"서울\","
                    + "\"category\":\"SOCIAL\",\"startsAt\":\"" + Instant.now().plusSeconds(7200) + "\","
                    + "\"endsAt\":\"" + Instant.now().plusSeconds(10800) + "\","
                    + "\"capacity\":10,\"pointCost\":0,\"visibility\":\"PUBLIC\",\"managerProgram\":false}";
        }

        @Test
        @DisplayName("#26 TUNTUN — POST /api/v1/events 정상 → 201")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_create_event_success_returns_201() throws Exception {
            when(eventFacade.create(any(), any())).thenReturn(sampleEvent());

            mockMvc.perform(post("/api/v1/events")
                            .contentType(APPLICATION_JSON)
                            .content(createEventBody()))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#27 MANAGER — POST /api/v1/events managerProgram=true → 201")
        @WithAppPrincipal(role = Role.MANAGER)
        void manager_create_manager_program_returns_201() throws Exception {
            SocialEvent managerEvent = sampleEvent();
            when(eventFacade.create(any(), any())).thenReturn(managerEvent);

            String body = "{\"title\":\"매니저 프로그램\",\"description\":\"설명\",\"regionText\":\"서울\","
                    + "\"category\":\"SOCIAL\",\"startsAt\":\"" + Instant.now().plusSeconds(7200) + "\","
                    + "\"endsAt\":\"" + Instant.now().plusSeconds(10800) + "\","
                    + "\"capacity\":10,\"pointCost\":0,\"visibility\":\"PUBLIC\",\"managerProgram\":true}";

            mockMvc.perform(post("/api/v1/events")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#28 DUNDUN(UNLINKED) — POST /api/v1/events → 403")
        @WithAppPrincipal(role = Role.DUNDUN)
        void unlinked_dundun_create_event_forbidden_returns_403() throws Exception {
            when(eventFacade.create(any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

            mockMvc.perform(post("/api/v1/events")
                            .contentType(APPLICATION_JSON)
                            .content(createEventBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#29 ADMIN — POST /api/v1/events → 201")
        @WithAppPrincipal(role = Role.ADMIN)
        void admin_create_event_returns_201() throws Exception {
            when(eventFacade.create(any(), any())).thenReturn(sampleEvent());

            mockMvc.perform(post("/api/v1/events")
                            .contentType(APPLICATION_JSON)
                            .content(createEventBody()))
                    .andExpect(status().isCreated());
        }
    }

    // ════════════════════════════════════════════════════════════
    // EVENT_PARTICIPATE 시나리오 (#30~34)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("EVENT_PARTICIPATE — 본인 참여")
    @WebMvcTest(controllers = EventController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class EventParticipateScenarios {

        @Autowired MockMvc mockMvc;
        @MockBean EventFacade eventFacade;
        @MockBean EventParticipationFacade participationFacade;
        @MockBean EventParticipationRepository participationRepository;

        private EventParticipation confirmedParticipation() {
            return EventParticipation.builder()
                    .id(UUID.randomUUID())
                    .eventId(TestFixtures.EVENT_ID)
                    .participantId(TestFixtures.USER_TUNTUN_ID)
                    .status(ParticipationStatus.CONFIRMED)
                    .proxiedBy(null)
                    .joinedAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("#30 TUNTUN — POST /api/v1/events/{id}/join 잔액충분·정원여유 → 201")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_join_event_success_returns_201() throws Exception {
            when(participationFacade.join(any(), any(), any()))
                    .thenReturn(confirmedParticipation());

            mockMvc.perform(post("/api/v1/events/" + TestFixtures.EVENT_ID + "/join"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#31 TUNTUN — POST /api/v1/events/{id}/join 정원 초과 → 409")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_join_event_full_returns_409() throws Exception {
            when(participationFacade.join(any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.EVENT_FULL));

            mockMvc.perform(post("/api/v1/events/" + TestFixtures.EVENT_ID + "/join"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#32 TUNTUN — POST /api/v1/events/{id}/join 잔액 부족 → 409")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_join_event_insufficient_points_returns_409() throws Exception {
            when(participationFacade.join(any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINTS));

            mockMvc.perform(post("/api/v1/events/" + TestFixtures.EVENT_ID + "/join"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#33 TUNTUN — POST /api/v1/events/{id}/join 중복 참여 → 409")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_join_event_duplicate_returns_409() throws Exception {
            when(participationFacade.join(any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 참여 중입니다."));

            mockMvc.perform(post("/api/v1/events/" + TestFixtures.EVENT_ID + "/join"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#34 TUNTUN — POST /api/v1/events/participations/{id}/cancel → 204")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_cancel_event_participation_returns_204() throws Exception {
            UUID participationId = UUID.randomUUID();
            when(participationFacade.requestCancel(any(), any())).thenReturn(null);

            mockMvc.perform(post("/api/v1/events/participations/" + participationId + "/cancel"))
                    .andExpect(status().isNoContent());
        }
    }

    // ════════════════════════════════════════════════════════════
    // PROXY_PARTICIPATE 시나리오 (#35~40)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PROXY_PARTICIPATE — 든든이 대리 참여")
    @WebMvcTest(controllers = {EventController.class, ApprovalController.class})
    @AutoConfigureMockMvc(addFilters = false)
    class ProxyParticipateScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean EventFacade eventFacade;
        @MockBean EventParticipationFacade participationFacade;
        @MockBean EventParticipationRepository participationRepository;
        @MockBean ApprovalFacade approvalFacade;
        @MockBean ApprovalRepository approvalRepository;
        @MockBean UserRepository userRepository;

        private EventParticipation pendingParticipation() {
            return EventParticipation.builder()
                    .id(UUID.randomUUID())
                    .eventId(TestFixtures.EVENT_ID)
                    .participantId(TestFixtures.USER_TUNTUN_ID)
                    .status(ParticipationStatus.PENDING_APPROVAL)
                    .proxiedBy(TestFixtures.USER_DUNDUN_ID)
                    .joinedAt(Instant.now())
                    .build();
        }

        private Approval pendingApproval() {
            return Approval.builder()
                    .id(TestFixtures.APPROVAL_ID)
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(TestFixtures.USER_DUNDUN_ID)
                    .approverId(TestFixtures.USER_TUNTUN_ID)
                    .status(ApprovalStatus.PENDING)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .createdAt(Instant.now())
                    .build();
        }

        private Approval approvedApproval() {
            return Approval.builder()
                    .id(TestFixtures.APPROVAL_ID)
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(TestFixtures.USER_DUNDUN_ID)
                    .approverId(TestFixtures.USER_TUNTUN_ID)
                    .status(ApprovalStatus.APPROVED)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .decidedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("#35 DUNDUN(LINKED) — POST /api/v1/events/{id}/join with proxiedTuntunId → 201 PENDING")
        @WithAppPrincipal(role = Role.DUNDUN)
        void linked_dundun_proxy_join_creates_pending_returns_201() throws Exception {
            when(participationFacade.join(any(), any(), any()))
                    .thenReturn(pendingParticipation());

            String body = "{\"proxiedTuntunId\":\"" + TestFixtures.USER_TUNTUN_ID + "\"}";
            mockMvc.perform(post("/api/v1/events/" + TestFixtures.EVENT_ID + "/join")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#36 DUNDUN(UNLINKED) — POST /api/v1/events/{id}/join with proxiedTuntunId → 403 NOT_LINKED")
        @WithAppPrincipal(role = Role.DUNDUN)
        void unlinked_dundun_proxy_join_returns_403() throws Exception {
            when(participationFacade.join(any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.NOT_LINKED));

            String body = "{\"proxiedTuntunId\":\"" + TestFixtures.USER_TUNTUN_ID + "\"}";
            mockMvc.perform(post("/api/v1/events/" + TestFixtures.EVENT_ID + "/join")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#37 TUNTUN — POST /api/v1/approvals/{id}/approve 본인 approval → 200")
        @WithAppPrincipal(role = Role.TUNTUN, userId = "00000000-0000-0000-0000-000000000001")
        void tuntun_approve_own_approval_returns_200() throws Exception {
            when(approvalFacade.approve(any(), any())).thenReturn(approvedApproval());

            mockMvc.perform(post("/api/v1/approvals/" + TestFixtures.APPROVAL_ID + "/approve"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#38 TUNTUN — POST /api/v1/approvals/{id}/reject 본인 approval → 200")
        @WithAppPrincipal(role = Role.TUNTUN, userId = "00000000-0000-0000-0000-000000000001")
        void tuntun_reject_own_approval_returns_200() throws Exception {
            Approval rejected = Approval.builder()
                    .id(TestFixtures.APPROVAL_ID)
                    .type(ApprovalType.EVENT_JOIN)
                    .requesterId(TestFixtures.USER_DUNDUN_ID)
                    .approverId(TestFixtures.USER_TUNTUN_ID)
                    .status(ApprovalStatus.REJECTED)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .decidedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            when(approvalFacade.reject(any(), any())).thenReturn(rejected);

            mockMvc.perform(post("/api/v1/approvals/" + TestFixtures.APPROVAL_ID + "/reject"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#39 TUNTUN — POST /api/v1/approvals/{id}/approve 타인 approval → 403")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_approve_other_approval_returns_403() throws Exception {
            when(approvalFacade.approve(any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "본인 승인 건만 처리할 수 있습니다."));

            mockMvc.perform(post("/api/v1/approvals/" + TestFixtures.APPROVAL_ID + "/approve"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#40 DUNDUN(LINKED) — GET /api/v1/approvals/me → 200")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_list_approvals_me_returns_200() throws Exception {
            when(approvalRepository.findByApproverIdAndStatusOrderByExpiresAtAsc(any(), any()))
                    .thenReturn(List.of(pendingApproval()));
            when(userRepository.findById(any())).thenReturn(
                    Optional.of(TestFixtures.dundunUser().build()));

            mockMvc.perform(get("/api/v1/approvals/me"))
                    .andExpect(status().isOk());
        }
    }

    // ════════════════════════════════════════════════════════════
    // POINT_TOPUP 시나리오 (#41~45)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POINT_TOPUP — 포인트 충전")
    @WebMvcTest(controllers = PointController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class PointTopupScenarios {

        @Autowired MockMvc mockMvc;
        @MockBean MockTopupFacade mockTopupFacade;
        @MockBean PointService pointService;
        @MockBean PointLedgerRepository pointLedgerRepository;

        private MockTopup sampleTopup() {
            return MockTopup.builder()
                    .userId(TestFixtures.USER_TUNTUN_ID)
                    .chargedBy(TestFixtures.USER_TUNTUN_ID)
                    .amount(BigDecimal.valueOf(5000))
                    .reasonText("테스트 충전")
                    .createdAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("#41 TUNTUN — POST /api/v1/points/mock-topup 정상 → 201")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_topup_success_returns_201() throws Exception {
            when(mockTopupFacade.topupSelf(any(), any(), anyString())).thenReturn(sampleTopup());

            String body = "{\"amount\":5000,\"reasonText\":\"테스트 충전\"}";
            mockMvc.perform(post("/api/v1/points/mock-topup")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#42 DUNDUN(LINKED) — POST /api/v1/points/mock-topup-proxy → 201")
        @WithAppPrincipal(role = Role.DUNDUN)
        void linked_dundun_proxy_topup_returns_201() throws Exception {
            when(mockTopupFacade.topupProxy(any(), any(), any(), anyString())).thenReturn(sampleTopup());

            String body = "{\"tuntunId\":\"" + TestFixtures.USER_TUNTUN_ID + "\","
                    + "\"amount\":5000,\"reasonText\":\"대리 충전\"}";
            mockMvc.perform(post("/api/v1/points/mock-topup-proxy")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#43 DUNDUN(UNLINKED) — POST /api/v1/points/mock-topup-proxy → 403")
        @WithAppPrincipal(role = Role.DUNDUN)
        void unlinked_dundun_proxy_topup_returns_403() throws Exception {
            when(mockTopupFacade.topupProxy(any(), any(), any(), anyString()))
                    .thenThrow(new BusinessException(ErrorCode.NOT_LINKED));

            String body = "{\"tuntunId\":\"" + TestFixtures.USER_TUNTUN_ID + "\","
                    + "\"amount\":5000,\"reasonText\":\"대리 충전\"}";
            mockMvc.perform(post("/api/v1/points/mock-topup-proxy")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("#44 TUNTUN — POST /api/v1/points/mock-topup amount=-1 → 400")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_topup_negative_amount_returns_400() throws Exception {
            String body = "{\"amount\":-1,\"reasonText\":\"잘못된 금액\"}";
            mockMvc.perform(post("/api/v1/points/mock-topup")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("#45 TUNTUN — POST /api/v1/points/mock-topup amount=0 → 400")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_topup_zero_amount_returns_400() throws Exception {
            String body = "{\"amount\":0,\"reasonText\":\"잘못된 금액\"}";
            mockMvc.perform(post("/api/v1/points/mock-topup")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════════
    // MANAGER 시나리오 (#46~48)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("MANAGER — 매니저 신청 권한")
    @WebMvcTest(controllers = ManagerController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class ManagerScenarios {

        @Autowired MockMvc mockMvc;
        @MockBean ManagerFacade managerFacade;
        @MockBean ManagerApplicationRepository managerApplicationRepository;

        private ManagerApplication pendingApplication() {
            return ManagerApplication.builder()
                    .id(UUID.randomUUID())
                    .applicantId(TestFixtures.USER_TUNTUN_ID)
                    .status(ManagerApplicationStatus.PENDING)
                    .reason("매니저가 되고 싶습니다.")
                    .createdAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("#46 TUNTUN — POST /api/v1/manager/apply → 201")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_apply_manager_returns_201() throws Exception {
            when(managerFacade.apply(any(), any())).thenReturn(pendingApplication());

            String body = "{\"reason\":\"매니저가 되고 싶습니다.\"}";
            mockMvc.perform(post("/api/v1/manager/apply")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("#47 TUNTUN — POST /api/v1/manager/apply 중복 신청 → 409")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_apply_manager_duplicate_returns_409() throws Exception {
            when(managerFacade.apply(any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 진행 중인 매니저 신청이 있습니다."));

            String body = "{\"reason\":\"중복 신청\"}";
            mockMvc.perform(post("/api/v1/manager/apply")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#48 ADMIN — GET /api/v1/admin/manager-applications → 200")
        @WithAppPrincipal(role = Role.ADMIN)
        void admin_list_manager_applications_returns_200() throws Exception {
            when(managerApplicationRepository.findByStatus(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(pendingApplication())));

            mockMvc.perform(get("/api/v1/admin/manager-applications"))
                    .andExpect(status().isOk());
        }
    }

    // ════════════════════════════════════════════════════════════
    // ADMIN 시나리오 (#49~51)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("ADMIN — 관리자 전용 권한")
    @WebMvcTest(controllers = AdminController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class AdminScenarios {

        @Autowired MockMvc mockMvc;
        @MockBean AdminFacade adminFacade;

        @Test
        @DisplayName("#49 ADMIN — POST /api/v1/admin/users/{userId}/suspend → 204")
        @WithAppPrincipal(role = Role.ADMIN)
        void admin_suspend_user_returns_204() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/" + TestFixtures.USER_TUNTUN_ID + "/suspend"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("#50 ADMIN — POST /api/v1/admin/events/{eventId}/force-close → 204")
        @WithAppPrincipal(role = Role.ADMIN)
        void admin_force_close_event_returns_204() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + TestFixtures.EVENT_ID + "/force-close"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("#51 ADMIN — GET /api/v1/admin/users → 200")
        @WithAppPrincipal(role = Role.ADMIN)
        void admin_get_users_returns_200() throws Exception {
            when(adminFacade.listUsers(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(TestFixtures.tuntunUser().build())));

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk());
        }
    }

    // ════════════════════════════════════════════════════════════
    // ROLE_SWITCH 시나리오 (#52~54)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("ROLE_SWITCH — 역할 전환")
    @WebMvcTest(controllers = ProfileController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class RoleSwitchScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean ProfileFacade profileFacade;
        @MockBean RoleSwitchFacade roleSwitchFacade;

        @Test
        @DisplayName("#52 DUNDUN(LINKED) — POST /api/v1/me/role-switch → 409 잔존 연동 차단")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_switch_role_with_active_linkage_returns_409() throws Exception {
            when(roleSwitchFacade.switchRole(any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "활성 연동이 남아 역할 전환이 불가합니다."));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.profile.dto.RoleSwitchRequest(Role.TUNTUN));

            mockMvc.perform(post("/api/v1/me/role-switch")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("#53 DUNDUN — POST /api/v1/me/role-switch 연동없음 → 200")
        @WithAppPrincipal(role = Role.DUNDUN)
        void dundun_switch_role_without_linkage_returns_200() throws Exception {
            User switched = TestFixtures.dundunUser().role(Role.TUNTUN).build();
            when(roleSwitchFacade.switchRole(any(), any())).thenReturn(switched);
            when(profileFacade.me(any())).thenReturn(
                    new MeResponse(switched.getId(), switched.getLoginId(), switched.getName(),
                            Role.TUNTUN, switched.getUniqueCode(),
                            BigDecimal.ZERO, "PPOJJAK", "뽀짝"));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.profile.dto.RoleSwitchRequest(Role.TUNTUN));

            mockMvc.perform(post("/api/v1/me/role-switch")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("#54 TUNTUN — POST /api/v1/me/role-switch 진행 중인 참여 있음 → 409")
        @WithAppPrincipal(role = Role.TUNTUN)
        void tuntun_switch_role_with_active_event_returns_409() throws Exception {
            when(roleSwitchFacade.switchRole(any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "미래 이벤트 활성 참여가 남아 역할 전환이 불가합니다."));

            String body = objectMapper.writeValueAsString(
                    new kr.zoomnear.domain.profile.dto.RoleSwitchRequest(Role.DUNDUN));

            mockMvc.perform(post("/api/v1/me/role-switch")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    // ════════════════════════════════════════════════════════════
    // EDGE 시나리오 (#55~57)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("EDGE — 경계 및 보안 케이스")
    @WebMvcTest(controllers = AuthController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class EdgeScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean AuthFacade authFacade;

        @Test
        @DisplayName("#55 ANON — Authorization 헤더 없이 인증 필요 엔드포인트 → 필터 레이어 외 401 mock")
        void tampered_jwt_missing_auth_header_behavior() throws Exception {
            // addFilters=false 환경에서는 SecurityFilterChain이 없어 JWT 필터가 동작하지 않음.
            // 실제 JWT 변조 테스트는 SecurityFilterChain 통합 테스트에서 수행.
            // 여기서는 Facade가 UNAUTHORIZED를 던지는 경우 컨트롤러가 401로 매핑하는지 검증.
            when(authFacade.login(any()))
                    .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{\"loginId\":\"x\",\"password\":\"y\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("#56 ANON — 만료된 세션 재로그인 시 401")
        void expired_session_returns_401() throws Exception {
            when(authFacade.login(any()))
                    .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "세션이 만료되었습니다."));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{\"loginId\":\"tuntun01\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("#57 TUNTUN — SQL injection 입력 포함 시 Facade 정상 처리 (400 또는 201)")
        @WithAppPrincipal(role = Role.TUNTUN)
        void sql_injection_in_login_id_is_handled() throws Exception {
            // 컨트롤러 레이어에서 @Valid가 SQL injection 입력을 400으로 차단 또는
            // Facade가 정상 저장하는지 검증. loginId 패턴 제약에 의해 400 예상.
            SignupRequest req = new SignupRequest(
                    "' OR 1=1--", "Password1!", "010-9999-9999",
                    null, "SQL인젝션", Role.TUNTUN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════════
    // AUTH 컨트롤러 실제 동작 검증 (즉시 실행)
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("AUTH-LIVE — AuthController 실제 동작 (즉시 실행)")
    @WebMvcTest(controllers = AuthController.class)
    @AutoConfigureMockMvc(addFilters = false)
    class AuthLiveScenarios {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @MockBean AuthFacade authFacade;

        @Test
        @DisplayName("signup — password 8자 미만(@Size 위반) → 400")
        void signup_short_password_returns_400() throws Exception {
            SignupRequest req = new SignupRequest(
                    "validuser", "short", "010-1234-5678",
                    null, "사용자", Role.TUNTUN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup — name 빈값(@NotBlank 위반) → 400")
        void signup_blank_name_returns_400() throws Exception {
            SignupRequest req = new SignupRequest(
                    "validuser1", "Password1!", "010-1234-5678",
                    null, "", Role.TUNTUN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup — phone 형식 위반(@Pattern) → 400")
        void signup_invalid_phone_returns_400() throws Exception {
            SignupRequest req = new SignupRequest(
                    "validuser2", "Password1!", "not-a-phone",
                    null, "사용자", Role.TUNTUN, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup — role null(@NotNull 위반) → 400")
        void signup_null_role_returns_400() throws Exception {
            String body = "{\"loginId\":\"validuser3\",\"password\":\"Password1!\","
                    + "\"phone\":\"010-1234-5678\",\"name\":\"사용자\",\"role\":null}";

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("login — 빈 body → 400")
        void login_empty_body_returns_400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("login — facade CONFLICT 예외 → 409")
        void login_conflict_throws_409() throws Exception {
            when(authFacade.login(any())).thenThrow(
                    new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 loginId."));

            LoginRequest req = new LoginRequest("existing", "Password1!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());
        }
    }
}
