package ev_charger.be.auth;

import com.nimbusds.oauth2.sdk.SuccessResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import ev_charger.be.auth.dto.request.RegisterRequest;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import ev_charger.be.common.exception.InternalServerException;
import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.enums.Provider;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== 샘플 데이터 =====

    // 소셜 플랫폼이 발급한 accessToken / 인가코드
    private static final String SOCIAL_ACCESS_TOKEN = "social-access-token";
    private static final String INVALID_SOCIAL_ACCESS_TOKEN = "invalid-social-access-token";
    private static final String KAKAO_CODE = "kakao-auth-code";
    private static final String GOOGLE_CODE = "google-auth-code";
    private static final String INVALID_CODE = "invalid-auth-code";

    // 서버가 발급하는 JWT
    private static final String JWT_ACCESS_TOKEN = "jwt-access-token";
    private static final String JWT_REFRESH_TOKEN = "jwt-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-jwt-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-jwt-refresh-token";
    private static final String INVALID_REFRESH_TOKEN = "invalid-jwt-refresh-token";

    // 회원가입용 임시 토큰 / 프로필 정보
    private static final String TEMP_TOKEN = "temp-token-uuid";
    private static final String INVALID_TEMP_TOKEN = "invalid-temp-token";
    private static final String NICKNAME = "테스터";
    private static final Integer PROFILE_IMAGE_ID = 1;

    private static final Provider PROVIDER = Provider.KAKAO;

    // 기존 회원 로그인 응답 (status: SUCCESS)
    private SocialLoginResponse successResponse;
    // 신규 회원 로그인 응답 (status: NEED_PROFILE_SELECT)
    private SocialLoginResponse needProfileResponse;
    // 토큰 재발급 응답
    private ReissueResponse reissueResponse;
    // 회원가입 요청 본문
    private RegisterRequest registerRequest;
    private String registerRequestJson;

    private long startTime;

    @BeforeEach
    void setUp() throws Exception {
        startTime = System.currentTimeMillis();

        successResponse = new SocialLoginResponse("SUCCESS", JWT_ACCESS_TOKEN, JWT_REFRESH_TOKEN, null);
        needProfileResponse = new SocialLoginResponse("NEED_PROFILE_SELECT", null, null, TEMP_TOKEN);
        reissueResponse = new ReissueResponse(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN);

        registerRequest = new RegisterRequest(TEMP_TOKEN, NICKNAME, PROFILE_IMAGE_ID);
        registerRequestJson = objectMapper.writeValueAsString(registerRequest);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== POST /auth/login ==========

    @Test
    void 소셜_로그인_성공시_200과_토큰을_반환한다() throws Exception {
        // given
        given(authService.socialLogin(SOCIAL_ACCESS_TOKEN, PROVIDER))
                .willReturn(successResponse);

        // when
        mockMvc.perform(post("/auth/login")
                        .param("accessToken", SOCIAL_ACCESS_TOKEN)
                        .param("provider", PROVIDER.name()))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value(JWT_ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(JWT_REFRESH_TOKEN));

        verify(authService).socialLogin(SOCIAL_ACCESS_TOKEN, PROVIDER);
    }

    @Test
    void 신규_사용자_소셜_로그인시_200과_tempToken을_반환한다() throws Exception {
        // given
        given(authService.socialLogin(SOCIAL_ACCESS_TOKEN, PROVIDER))
                .willReturn(needProfileResponse);

        // when
        mockMvc.perform(post("/auth/login")
                        .param("accessToken", SOCIAL_ACCESS_TOKEN)
                        .param("provider", PROVIDER.name()))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEED_PROFILE_SELECT"))
                .andExpect(jsonPath("$.tempToken").value(TEMP_TOKEN));

        verify(authService).socialLogin(SOCIAL_ACCESS_TOKEN, PROVIDER);
    }

    @Test
    void 소셜_로그인시_accessToken_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/login")
                        .param("provider", PROVIDER.name()))

                // then
                .andExpect(status().isBadRequest());

        verify(authService, never()).socialLogin(any(), any());
    }

    @Test
    void 소셜_로그인시_provider_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/login")
                        .param("accessToken", SOCIAL_ACCESS_TOKEN))

                // then
                .andExpect(status().isBadRequest());

        verify(authService, never()).socialLogin(any(), any());
    }

    @Test
    void 소셜_로그인시_지원하지_않는_provider면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/login")
                        .param("accessToken", SOCIAL_ACCESS_TOKEN)
                        .param("provider", "FACEBOOK"))

                // then
                .andExpect(status().isBadRequest());

        verify(authService, never()).socialLogin(any(), any());
    }

    @Test
    void 소셜_로그인시_유효하지_않은_accessToken이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("유효하지 않은 소셜 토큰")).given(authService).socialLogin(INVALID_SOCIAL_ACCESS_TOKEN, PROVIDER);

        // when
        mockMvc.perform(post("/auth/login")
                        .param("accessToken", INVALID_SOCIAL_ACCESS_TOKEN)
                        .param("provider", PROVIDER.name()))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 소셜 토큰"));

        // 파라미터 검증은 통과해서 서비스는 호출됐고, 서비스 안에서 예외가 발생함
        verify(authService).socialLogin(INVALID_SOCIAL_ACCESS_TOKEN, PROVIDER);
    }

    // ========== POST /auth/register ==========

    @Test
    void 회원가입_성공시_200과_토큰을_반환한다() throws Exception {
        // given
        given(authService.register(registerRequest)).willReturn(successResponse);

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value(JWT_ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(JWT_REFRESH_TOKEN));

        verify(authService).register(registerRequest);
    }

    @Test
    void 회원가입시_tempToken이_유효하지_않으면_400을_반환한다() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(INVALID_TEMP_TOKEN, NICKNAME, PROFILE_IMAGE_ID);

        willThrow(new IllegalArgumentException("유효하지 않은 tempToken")).given(authService).register(invalidRequest);

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 tempToken"));

        verify(authService).register(invalidRequest);
    }

    @Test
    void 회원가입시_tempToken_데이터가_손상되었으면_500을_반환한다() throws Exception {
        // given
        willThrow(new InternalServerException("회원가입 정보를 확인할 수 없습니다. 다시 로그인해 주세요.", new RuntimeException("파싱 실패")))
                .given(authService).register(registerRequest);

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))

                // then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("회원가입 정보를 확인할 수 없습니다. 다시 로그인해 주세요."));

        verify(authService).register(registerRequest);
    }

    @Test
    void 회원가입시_이미_가입된_사용자면_409를_반환한다() throws Exception {
        // given
        given(authService.register(registerRequest)).willThrow(new IllegalStateException("이미 가입된 사용자입니다."));

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))

                // then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 가입된 사용자입니다."));

        verify(authService).register(registerRequest);
    }

    @Test
    void 회원가입시_요청_본문이_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/register"))

                // then
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    // ========== POST /auth/logout ==========

    @Test
    void 로그아웃_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/logout")
                .param("accessToken", JWT_ACCESS_TOKEN)
                .param("refreshToken", JWT_REFRESH_TOKEN))

        // then
                .andExpect(status().isOk());

        verify(authService).logout(JWT_ACCESS_TOKEN, JWT_REFRESH_TOKEN);
    }

    @Test
    void 로그아웃시_refreshToken_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/logout")
                        .param("accessToken", JWT_ACCESS_TOKEN))

                // then
                .andExpect(status().isBadRequest());

        verify(authService, never()).logout(any(), any());
    }

    @Test
    void 로그아웃시_유효하지_않은_토큰이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("유효하지 않은 refresh token")).given(authService).logout(JWT_ACCESS_TOKEN, INVALID_REFRESH_TOKEN);

        // when
        mockMvc.perform(post("/auth/logout")
                        .param("accessToken", JWT_ACCESS_TOKEN)
                        .param("refreshToken", INVALID_REFRESH_TOKEN))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 refresh token"));

        verify(authService).logout(JWT_ACCESS_TOKEN, INVALID_REFRESH_TOKEN);
    }

    // ========== POST /auth/reissue ==========

    @Test
    void 토큰_재발급_성공시_200과_새_토큰을_반환한다() throws Exception {
        // given
        given(authService.reissue(JWT_REFRESH_TOKEN))
                .willReturn(reissueResponse);

        // when
        mockMvc.perform(post("/auth/reissue")
                .param("refreshToken", JWT_REFRESH_TOKEN))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAccessToken").value(NEW_ACCESS_TOKEN))
                .andExpect(jsonPath("$.newRefreshToken").value(NEW_REFRESH_TOKEN));

        verify(authService).reissue(JWT_REFRESH_TOKEN);
    }

    @Test
    void 토큰_재발급시_refreshToken_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/reissue"))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다.")); // GlobalExceptionHandler.java

        verify(authService, never()).reissue(any());
    }

    @Test
    void 토큰_재발급시_유효하지_않은_refreshToken이면_400을_반환한다() throws Exception {
        // given
        given(authService.reissue(INVALID_REFRESH_TOKEN))
                .willThrow(new IllegalArgumentException("유효하지 않은 refresh token"));

        // when
        mockMvc.perform(post("/auth/reissue")
                        .param("refreshToken", INVALID_REFRESH_TOKEN))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 refresh token"));

        verify(authService).reissue(INVALID_REFRESH_TOKEN);
    }

    // ========== POST /auth/login/kakao/code ==========

    @Test
    void 카카오_인가코드_로그인_성공시_200과_토큰을_반환한다() throws Exception {
        // given
        given(authService.kakaoCodeLogin(KAKAO_CODE))
                .willReturn(successResponse);

        // when
        mockMvc.perform(post("/auth/login/kakao/code")
                .param("code", KAKAO_CODE))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value(JWT_ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(JWT_REFRESH_TOKEN));

        verify(authService).kakaoCodeLogin(KAKAO_CODE);
    }

    @Test
    void 카카오_인가코드_로그인시_신규_사용자면_200과_tempToken을_반환한다() throws Exception {
        // given
        given(authService.kakaoCodeLogin(KAKAO_CODE)).willReturn(needProfileResponse);

        // when
        mockMvc.perform(post("/auth/login/kakao/code")
                .param("code", KAKAO_CODE))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEED_PROFILE_SELECT"))
                .andExpect(jsonPath("$.tempToken").value(TEMP_TOKEN));

        verify(authService).kakaoCodeLogin(KAKAO_CODE);
    }

    @Test
    void 카카오_인가코드_로그인시_code_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/login/kakao/code"))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

        verify(authService, never()).kakaoCodeLogin(any());
    }

    @Test
    void 카카오_인가코드_로그인시_유효하지_않은_code면_400을_반환한다() throws Exception {
        // given
        given(authService.kakaoCodeLogin(INVALID_CODE))
                .willThrow(new IllegalArgumentException("유효하지 않은 인가 코드"));

        // when
        mockMvc.perform(post("/auth/login/kakao/code")
                        .param("code", INVALID_CODE))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인가 코드"));

        verify(authService).kakaoCodeLogin(INVALID_CODE);
    }

    // ========== POST /auth/google/token ==========

    @Test
    void 구글_인가코드_로그인_성공시_200과_토큰을_반환한다() throws Exception {
        // given
        given(authService.googleCodeLogin(GOOGLE_CODE))
                .willReturn(successResponse);

        // when
        mockMvc.perform(post("/auth/google/token")
                        .param("code", GOOGLE_CODE))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value(JWT_ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(JWT_REFRESH_TOKEN));

        verify(authService).googleCodeLogin(GOOGLE_CODE);
    }

    @Test
    void 구글_인가코드_로그인시_신규_사용자면_200과_tempToken을_반환한다() throws Exception {
        // given
        given(authService.googleCodeLogin(GOOGLE_CODE))
                .willReturn(needProfileResponse);

        // when
        mockMvc.perform(post("/auth/google/token")
                        .param("code", GOOGLE_CODE))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEED_PROFILE_SELECT"))
                .andExpect(jsonPath("$.tempToken").value(TEMP_TOKEN));

        verify(authService).googleCodeLogin(GOOGLE_CODE);
    }

    @Test
    void 구글_인가코드_로그인시_code_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/auth/google/token"))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    void 구글_인가코드_로그인시_유효하지_않은_code면_400을_반환한다() throws Exception {
        // given
        given(authService.googleCodeLogin(INVALID_CODE))
                .willThrow(new IllegalArgumentException("유효하지 않은 인가 코드"));

        // when
        mockMvc.perform(post("/auth/google/token")
                        .param("code", INVALID_CODE))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 인가 코드"));

        verify(authService).googleCodeLogin(INVALID_CODE);
    }
}
