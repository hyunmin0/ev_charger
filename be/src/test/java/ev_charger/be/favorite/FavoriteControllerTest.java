package ev_charger.be.favorite;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.station.congestion.CongestionLevel;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.user.User;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FavoriteController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 로그인한 유저 정보
    private static final String NICKNAME = "테스터";
    private static final String EMAIL = "test@example.com";
    private static final String PROVIDER_ID = "google-1234";

    // 즐겨찾기 추가/삭제 대상 충전소
    private static final String STAT_ID = "ME000001";
    // DB에 없는 충전소 id
    private static final String INVALID_STAT_ID = "INVALID";

    // 즐겨찾기 목록 조회 요청 파라미터 (내 위치)
    private static final double LAT = 37.5665;
    private static final double LNG = 126.9780;
    // double 파라미터에 문자열을 넣는 타입 불일치 케이스
    private static final String NOT_NUMBER_VALUE = "abc";

    // 즐겨찾기 목록 응답에 들어갈 충전소 정보
    private static final String STAT_NM = "서울시청 충전소";
    private static final String ADDR = "서울특별시 중구 세종대로 110";
    private static final String USE_TIME = "24시간 이용가능";
    private static final String KIND = "공공시설";
    private static final String FLOOR_TYPE = "지상";
    private static final String BUSI_NM = "환경부";
    private static final double DISTANCE = 120.5;
    private static final String SECOND_STAT_ID = "ME000002";
    private static final String SECOND_STAT_NM = "을지로 충전소";
    private static final double SECOND_DISTANCE = 480.0;

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 즐겨찾기 목록 조회 응답
    private List<StationResponse> stationResponses;

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

        stationResponses = List.of(
                new StationResponse(STAT_ID, STAT_NM, ADDR, LAT, LNG, USE_TIME, true, true, KIND, FLOOR_TYPE,
                        true, BUSI_NM, 4, 2, true, false, false, 4.5, 10, DISTANCE, CongestionLevel.SPACIOUS),
                new StationResponse(SECOND_STAT_ID, SECOND_STAT_NM, ADDR, LAT, LNG, USE_TIME, false, true, KIND, FLOOR_TYPE,
                        false, BUSI_NM, 2, 0, false, false, true, null, 0, SECOND_DISTANCE, null)
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== POST /favorites/{statId} ==========

    @Test
    void 즐겨찾기_추가_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/favorites/{statId}", STAT_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(favoriteService).addFavorite(user, STAT_ID);
    }

    @Test
    void 즐겨찾기_추가시_존재하지_않는_충전소면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("유효하지 않은 충전소입니다.")).given(favoriteService).addFavorite(user, INVALID_STAT_ID);

        // when
        mockMvc.perform(post("/favorites/{statId}", INVALID_STAT_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 충전소입니다."));

        verify(favoriteService).addFavorite(user, INVALID_STAT_ID);
    }

    @Test
    void 즐겨찾기_추가시_이미_등록된_충전소면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("이미 등록된 충전소입니다.")).given(favoriteService).addFavorite(user, STAT_ID);

        // when
        mockMvc.perform(post("/favorites/{statId}", STAT_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록된 충전소입니다."));

        verify(favoriteService).addFavorite(user, STAT_ID);
    }

    @Test
    void 즐겨찾기_추가시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/favorites/{statId}", STAT_ID))

                // then
                .andExpect(status().isForbidden());

        verify(favoriteService, never()).addFavorite(any(), any());
    }

    // ========== DELETE /favorites/{statId} ==========

    @Test
    void 즐겨찾기_삭제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/favorites/{statId}", STAT_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(favoriteService).deleteFavorite(user, STAT_ID);
    }

    @Test
    void 즐겨찾기_삭제시_존재하지_않는_충전소면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("즐겨찾기에 존재하지 않는 충전소입니다.")).given(favoriteService).deleteFavorite(user, STAT_ID);

        // when
        mockMvc.perform(delete("/favorites/{statId}", STAT_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("즐겨찾기에 존재하지 않는 충전소입니다."));

        verify(favoriteService).deleteFavorite(user, STAT_ID);
    }

    @Test
    void 즐겨찾기_삭제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/favorites/{statId}", STAT_ID))

                // then
                .andExpect(status().isForbidden());

        verify(favoriteService, never()).deleteFavorite(any(), any());
    }

    // ========== GET /favorites ==========

    @Test
    void 즐겨찾기_목록_조회_성공시_200과_충전소_목록을_반환한다() throws Exception {
        // given
        given(favoriteService.getFavoriteList(user, LAT, LNG)).willReturn(stationResponses);

        // when
        mockMvc.perform(get("/favorites")
                .with(authentication(auth))
                .param("lat", String.valueOf(LAT))
        .param("lng", String.valueOf(LNG)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(stationResponses.size()))
                .andExpect(jsonPath("$[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$[0].statNm").value(STAT_NM))
                .andExpect(jsonPath("$[0].distance").value(DISTANCE))
                .andExpect(jsonPath("$[1].statId").value(SECOND_STAT_ID))
                .andExpect(jsonPath("$[1].statNm").value(SECOND_STAT_NM))
                .andExpect(jsonPath("$[1].distance").value(SECOND_DISTANCE));

        verify(favoriteService).getFavoriteList(user, LAT, LNG);
    }

    @Test
    void 즐겨찾기_목록_조회시_즐겨찾기가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(favoriteService.getFavoriteList(user, LAT, LNG)).willReturn(List.of());

        // when
        mockMvc.perform(get("/favorites")
                        .with(authentication(auth))
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(favoriteService).getFavoriteList(user, LAT, LNG);
    }

    @Test
    void 즐겨찾기_목록_조회시_lat_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/favorites")
                        .with(authentication(auth))
                        .param("lng", String.valueOf(LNG)))

                // then
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).getFavoriteList(any(), anyDouble(), anyDouble());
    }

    @Test
    void 즐겨찾기_목록_조회시_lat이_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/favorites")
                        .with(authentication(auth))
                        .param("lat", NOT_NUMBER_VALUE)
                        .param("lng", String.valueOf(LNG)))

                // then
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).getFavoriteList(any(), anyDouble(), anyDouble());
    }

    @Test
    void 즐겨찾기_목록_조회시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/favorites")
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG)))

                // then
                .andExpect(status().isForbidden());

        verify(favoriteService, never()).getFavoriteList(any(), anyDouble(), anyDouble());
    }
}
