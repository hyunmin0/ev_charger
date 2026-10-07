package ev_charger.be.integration;

import ev_charger.be.config.IntegrationTestSupport;
import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import ev_charger.be.user.enums.Provider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 JWT로 시큐리티 필터를 통과하는지 + 토큰 재발급(/auth/reissue)이 동작하는지 검증
// 로그인이 필요한 API 대표로 GET /user/profile 사용
class AuthenticationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    private static final String PROFILE_URL = "/user/profile";
    private static final String REISSUE_URL = "/auth/reissue";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "유효하지 않은 refresh token";
    private static final String NICKNAME = "테스터";
    private static final String EMAIL = "test@example.com";

    private User user;

    // /auth/reissue, /auth/logout 요청 body
    private static String refreshTokenBody(String refreshToken) {
        return "{\"refreshToken\": \"" + refreshToken + "\"}";
    }

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .nickname(NICKNAME)
                .email(EMAIL)
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build());
    }

    @Test
    void 유효한_access_token이면_로그인한_유저의_정보를_조회한다() throws Exception {
        // given
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());

        // when & then
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void 토큰이_없으면_거부한다() throws Exception {
        // given

        // when & then
        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 서명이_잘못된_토큰이면_거부한다() throws Exception {
        // given
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        String tamperedToken = tamperSignature(accessToken);

        // when & then
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_token을_access_token_대신_쓰면_거부한다() throws Exception {
        // given
        String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());

        // when & then
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void DB에_없는_유저의_토큰이면_거부한다() throws Exception {
        // given
        String accessToken = jwtProvider.generateAccessToken(UUID.randomUUID()); // 탈퇴 등으로 없는 유저

        // when & then
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃한_access_token은_더이상_사용할_수_없다() throws Exception {
        // given
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        String refreshToken = login(); // logout은 refresh token으로 유저를 찾음

        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()); // 로그아웃 전에는 사용 가능

        // when
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(refreshToken)))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized()); // redis blacklist에 등록됨
    }

    // ===== 토큰 재발급 =====

    @Test
    void 유효한_refresh_token이면_새_토큰을_발급하고_새_access_token으로_인증된다() throws Exception {
        // given
        String refreshToken = login();

        // when
        String response = mockMvc.perform(post(REISSUE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(refreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        String newAccessToken = JsonPath.read(response, "$.newAccessToken");
        String newRefreshToken = JsonPath.read(response, "$.newRefreshToken");

        assertThat(user.getRefreshToken()).isEqualTo(newRefreshToken); // DB의 refresh token이 새 값으로 교체됨

        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(NICKNAME));
    }

    @Test
    void 재발급에_쓴_이전_refresh_token은_다시_사용할_수_없다() throws Exception {
        // given
        String oldRefreshToken = login();
        // jwt 만료시간은 초 단위라 같은 초 안에 재발급하면 이전 토큰과 똑같은 토큰이 나옴 -> 1초 뒤에 재발급
        Thread.sleep(1000);
        mockMvc.perform(post(REISSUE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(oldRefreshToken)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post(REISSUE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(oldRefreshToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_REFRESH_TOKEN_MESSAGE));
    }

    @Test
    void 로그아웃한_refresh_token으로는_재발급할_수_없다() throws Exception {
        // given
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        String refreshToken = login();
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(refreshToken)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post(REISSUE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(refreshToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_REFRESH_TOKEN_MESSAGE));
    }

    @Test
    void access_token으로는_재발급할_수_없다() throws Exception {
        // given
        login();
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());

        // when & then
        mockMvc.perform(post(REISSUE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_REFRESH_TOKEN_MESSAGE));
    }

    @Test
    void 서명이_잘못된_refresh_token으로는_재발급할_수_없다() throws Exception {
        // given
        String refreshToken = login();
        String tamperedToken = tamperSignature(refreshToken);

        // when & then
        mockMvc.perform(post(REISSUE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(tamperedToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_REFRESH_TOKEN_MESSAGE));
    }

    // 로그인 상태로 만들기 (로그인 시 발급한 refresh token을 DB에 저장하는 것과 동일)
    private String login() {
        String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());
        user.updateRefreshToken(refreshToken);
        return refreshToken;
    }

    // 서명 부분 변조: 서명의 첫 글자를 다른 글자로 교체
    // (마지막 글자는 base64 패딩 비트라 바꿔도 서명 값이 그대로일 수 있어서 첫 글자를 바꿈)
    private String tamperSignature(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    }
}
