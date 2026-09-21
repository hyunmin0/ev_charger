package ev_charger.be.user;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.dto.response.UserResponse;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.fcmToken.FcmTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FcmTokenService fcmTokenService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 프로필 정보
    private static final String NICKNAME = "테스터";
    private static final String NEW_NICKNAME = "새로운닉네임";
    private static final String EMAIL = "test@example.com";
    private static final String PROVIDER_ID = "google-1234";

    // 프로필 이미지: 현재 이미지 -> 수정 후 이미지
    private static final Integer PROFILE_IMAGE_ID = 1;
    private static final String IMAGE_URL = "https://example.com/profile/1.png";
    private static final Integer NEW_PROFILE_IMAGE_ID = 2;
    private static final String NEW_IMAGE_URL = "https://example.com/profile/2.png";
    // DB에 없는 이미지 id
    private static final Integer INVALID_PROFILE_IMAGE_ID = 999;
    // Integer 파라미터에 문자열을 넣는 타입 불일치 케이스
    private static final String NOT_NUMBER_PROFILE_IMAGE_ID = "abc";

    // fcm 토큰
    private static final String FCM_TOKEN = "fcm-token-sample";
    private static final String UNREGISTERED_FCM_TOKEN = "unregistered-fcm-token";

    // 프로필 조회 응답에 들어갈 개수 (내 차량 / 리뷰 / 충전기 알림)
    private static final int USER_CAR_COUNT = 2;
    private static final int REVIEW_COUNT = 3;
    private static final int CHARGER_ALERT_COUNT = 1;

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 프로필 조회 응답
    private UserResponse userResponse;
    // 이메일/프로필 사진을 설정하지 않은 유저의 프로필 조회 응답
    private UserResponse emptyProfileResponse;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();

        user = User.builder()
                .nickname(NICKNAME)
                .email(EMAIL)
                .provider(Provider.GOOGLE)
                .providerId(PROVIDER_ID)
                .build();
        ReflectionTestUtils.setField(user, "userId", UUID.randomUUID());

        CustomUserDetails principal = new CustomUserDetails(user);
        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        userResponse = new UserResponse(NICKNAME, EMAIL, IMAGE_URL, USER_CAR_COUNT, REVIEW_COUNT, CHARGER_ALERT_COUNT);
        emptyProfileResponse = new UserResponse(NICKNAME, null, null, 0, 0, 0);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== GET /user/profile ==========

    @Test
    void 프로필_조회_성공시_200과_프로필_정보를_반환한다() throws Exception {
        // given
        given(userService.getProfile(user)).willReturn(userResponse);

        // when
        mockMvc.perform(get("/user/profile")
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(userService).getProfile(user);
    }

    @Test
    void 프로필_조회시_이메일과_프로필_사진이_없으면_null로_반환한다() throws Exception {
        // given
        given(userService.getProfile(user)).willReturn(emptyProfileResponse);

        // when
        mockMvc.perform(get("/user/profile")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.imageUrl").isEmpty());

        verify(userService).getProfile(user);
    }

    @Test
    void 프로필_조회시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/user/profile"))

                // then
                .andExpect(status().isForbidden());

        verify(userService, never()).getProfile(any());
    }

    @Test
    void 프로필_조회시_존재하지_않는_유저면_400을_반환한다() throws Exception {
        // given
        given(userService.getProfile(user)).willThrow(new IllegalArgumentException("존재하지 않은 유저"));

        // when
        mockMvc.perform(get("/user/profile")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않은 유저"));

        verify(userService).getProfile(user);
    }

    // ========== PATCH /user/nickname ==========

    @Test
    void 닉네임_수정_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/user/nickname")
                .with(authentication(auth))
                .param("newName", NEW_NICKNAME))

        // then
                .andExpect(status().isOk());

        verify(userService).updateNickname(user, NEW_NICKNAME);
    }

    @Test
    void 닉네임_수정시_newName_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/user/nickname")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateNickname(any(), any());
    }

    @Test
    void 닉네임_수정시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/user/nickname")
                        .param("newName", NEW_NICKNAME))

                // then
                .andExpect(status().isForbidden());

        verify(userService, never()).updateNickname(any(), any());
    }

    // ========== PATCH /user/profile-image ==========

    @Test
    void 프로필_사진_수정_성공시_200과_이미지_url을_반환한다() throws Exception {
        // given
        given(userService.updateProfileImage(user, NEW_PROFILE_IMAGE_ID)).willReturn(NEW_IMAGE_URL);

        // when
        mockMvc.perform(patch("/user/profile-image")
                .with(authentication(auth))
                .param("profileImageId", String.valueOf(NEW_PROFILE_IMAGE_ID)))

        // then
                .andExpect(status().isOk())
                .andExpect(content().string(NEW_IMAGE_URL));

        verify(userService).updateProfileImage(user, NEW_PROFILE_IMAGE_ID);
    }

    @Test
    void 프로필_사진_수정시_profileImageId_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/user/profile-image")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfileImage(any(), any());
    }

    @Test
    void 프로필_사진_수정시_profileImageId가_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/user/profile-image")
                        .with(authentication(auth))
                        .param("profileImageId", "string"))

                // then
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfileImage(any(), any());
    }

    @Test
    void 프로필_사진_수정시_존재하지_않는_이미지_id면_400을_반환한다() throws Exception {
        // given
        given(userService.updateProfileImage(user, INVALID_PROFILE_IMAGE_ID)).willThrow(new IllegalArgumentException("존재하지 않는 프로필 이미지"));

        // when
        mockMvc.perform(patch("/user/profile-image")
                        .with(authentication(auth))
                        .param("profileImageId", String.valueOf(INVALID_PROFILE_IMAGE_ID)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 프로필 이미지"));

        verify(userService).updateProfileImage(user, INVALID_PROFILE_IMAGE_ID);
    }

    @Test
    void 프로필_사진_수정시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/user/profile-image")
                        .param("profileImageId", String.valueOf(NEW_PROFILE_IMAGE_ID)))

                // then
                .andExpect(status().isForbidden());

        verify(userService, never()).updateProfileImage(any(), any());
    }

    // ========== POST /user/fcm-token ==========

    @Test
    void fcm토큰_등록_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/fcm-token")
                .with(authentication(auth))
                .param("fcmToken", FCM_TOKEN))

        // then
                .andExpect(status().isOk());

        verify(fcmTokenService).register(user, FCM_TOKEN);
    }

    @Test
    void fcm토큰_등록시_fcmToken_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/fcm-token")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest());

        verify(fcmTokenService, never()).register(any(), any());
    }

    @Test
    void fcm토큰_등록시_이미_등록된_토큰이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("중복된 Fcm 토큰 값입니다.")).given(fcmTokenService).register(user, FCM_TOKEN);

        // when
        mockMvc.perform(post("/user/fcm-token")
                        .with(authentication(auth))
                        .param("fcmToken", FCM_TOKEN))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("중복된 Fcm 토큰 값입니다."));

        verify(fcmTokenService).register(user, FCM_TOKEN);
    }

    @Test
    void fcm토큰_등록시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/fcm-token")
                        .param("fcmToken", FCM_TOKEN))

                // then
                .andExpect(status().isForbidden());

        verify(fcmTokenService, never()).register(any(), any());
    }

    // ========== DELETE /user/fcm-token ==========

    @Test
    void fcm토큰_삭제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/user/fcm-token")
                .with(authentication(auth))
                .param("fcmToken", FCM_TOKEN))

        // then
                .andExpect(status().isOk());

        verify(fcmTokenService).delete(user, FCM_TOKEN);
    }

    @Test
    void fcm토큰_삭제시_fcmToken_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/user/fcm-token")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest());

        verify(fcmTokenService, never()).delete(any(), any());
    }

    @Test
    void fcm토큰_삭제시_등록되지_않은_토큰이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("Fcm 토큰이 존재하지 않습니다.")).given(fcmTokenService).delete(user, FCM_TOKEN);

        // when
        mockMvc.perform(delete("/user/fcm-token")
                        .with(authentication(auth))
                        .param("fcmToken", FCM_TOKEN))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fcm 토큰이 존재하지 않습니다."));

        verify(fcmTokenService).delete(user, FCM_TOKEN);
    }

    @Test
    void fcm토큰_삭제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/user/fcm-token")
                        .param("fcmToken", FCM_TOKEN))

                // then
                .andExpect(status().isForbidden());

        verify(fcmTokenService, never()).delete(any(), any());
    }
}
