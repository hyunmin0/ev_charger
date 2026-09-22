package ev_charger.be.station;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.congestion.CongestionLevel;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.NearbyStationPageResponse;
import ev_charger.be.station.dto.response.StationDetailResponse;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = StationController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationService stationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 로그인한 유저 정보 (상세 조회에서만 사용)
    private static final String NICKNAME = "테스터";
    private static final String EMAIL = "test@example.com";
    private static final String PROVIDER_ID = "google-1234";

    // 가까운 충전소 조회 요청 파라미터 (내 위치, 반경, 커서)
    private static final double LAT = 37.5665;
    private static final double LNG = 126.9780;
    private static final int RANGE = 1000;
    private static final String CURSOR = "encoded-cursor";
    private static final String NEXT_CURSOR = "encoded-next-cursor";

    // 지도 범위 조회 요청 파라미터 (지도 모서리 좌표, 내 위치)
    private static final double MIN_LAT = 37.55;
    private static final double MAX_LAT = 37.58;
    private static final double MIN_LNG = 126.96;
    private static final double MAX_LNG = 126.99;

    // 필터 파라미터 (filter.availableOnly, filter.minOutput, filter.chgerTypes 로 넘김)
    private static final boolean AVAILABLE_ONLY = true;
    private static final int MIN_OUTPUT = 50;
    private static final String CHGER_TYPE = "04";
    private static final String SECOND_CHGER_TYPE = "07";

    // double 파라미터에 문자열을 넣는 타입 불일치 케이스
    private static final String NOT_NUMBER_VALUE = "abc";

    // 충전소 목록 응답에 들어갈 충전소 정보
    private static final String STAT_ID = "ME000001";
    private static final String STAT_NM = "서울시청 충전소";
    private static final String ADDR = "서울특별시 중구 세종대로 110";
    private static final String USE_TIME = "24시간 이용가능";
    private static final String KIND = "공공시설";
    private static final String FLOOR_TYPE = "지상";
    private static final String BUSI_NM = "환경부";
    private static final String BUSI_CALL = "1661-9408";
    private static final double DISTANCE = 120.5;
    private static final String SECOND_STAT_ID = "ME000002";
    private static final String SECOND_STAT_NM = "을지로 충전소";
    private static final double SECOND_DISTANCE = 480.0;

    // 충전소 상세 조회 응답에 들어갈 충전기 정보
    private static final String CHGER_ID = "01";
    private static final String CHGER_OUTPUT = "100";

    // DB에 없는 충전소 id
    private static final String INVALID_STAT_ID = "INVALID";

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 요청 필터 (nearby, bounds 공용)
    private StationFilter stationFilter;
    // 가까운 충전소 조회 요청 (내 위치 + 반경 + 커서 + 필터)
    private NearbyStationRequest nearbyStationRequest;
    // 지도 범위 조회 요청 (지도 모서리 좌표 + 내 위치 + 필터)
    private MapBoundsRequest mapBoundsRequest;
    // 충전소 목록 응답 (nearby, bounds 공용)
    private List<StationResponse> stationResponses;
    // 가까운 충전소 조회 응답
    private NearbyStationPageResponse nearbyStationPageResponse;
    // 비로그인 상세 조회 응답 (isFavorite = null, isAlert = false)
    private StationDetailResponse guestStationDetailResponse;
    // 로그인 상세 조회 응답 (isFavorite = true, isAlert = true)
    private StationDetailResponse userStationDetailResponse;

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

        // 넘기지 않은 필터 값(parkingFree, limitYn, maxOutput, kinds, floorTypes)은 null 로 바인딩됨
        stationFilter = new StationFilter(AVAILABLE_ONLY, null, null, MIN_OUTPUT, null,
                List.of(CHGER_TYPE, SECOND_CHGER_TYPE), null, null);
        nearbyStationRequest = new NearbyStationRequest(LAT, LNG, RANGE, CURSOR, stationFilter);
        mapBoundsRequest = new MapBoundsRequest(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG, LAT, LNG, stationFilter);

        stationResponses = List.of(
                new StationResponse(STAT_ID, STAT_NM, ADDR, LAT, LNG, USE_TIME, true, true, KIND, FLOOR_TYPE,
                        true, BUSI_NM, 4, 2, true, false, false, 4.5, 10, DISTANCE, CongestionLevel.SPACIOUS),
                new StationResponse(SECOND_STAT_ID, SECOND_STAT_NM, ADDR, LAT, LNG, USE_TIME, false, true, KIND, FLOOR_TYPE,
                        false, BUSI_NM, 2, 0, false, false, true, null, 0, SECOND_DISTANCE, null)
        );
        nearbyStationPageResponse = new NearbyStationPageResponse(stationResponses, NEXT_CURSOR);

        guestStationDetailResponse = createStationDetailResponse(null, false);
        userStationDetailResponse = createStationDetailResponse(true, true);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private StationDetailResponse createStationDetailResponse(Boolean isFavorite, boolean isAlert) {
        return new StationDetailResponse(
                STAT_ID, STAT_NM, ADDR, null, USE_TIME, true, null, true, null, KIND, null, null, FLOOR_TYPE,
                true, BUSI_NM, BUSI_CALL, 4.5, 10, isFavorite,
                List.of(new StationDetailResponse.ChgerDetail(CHGER_ID, ChgerType.DC_COMBO, CHGER_OUTPUT, ChgerStat.WAITING, isAlert)),
                List.of(),
                new StationDetailResponse.CongestionDetail(0.9, CongestionLevel.SPACIOUS, CongestionLevel.NORMAL, CongestionLevel.CONGESTED)
        );
    }

    // ========== GET /stations/nearby ==========

    @Test
    void 가까운_충전소_조회_성공시_200과_충전소_목록과_다음_커서를_반환한다() throws Exception {
        // given
        given(stationService.getNearbyStations(nearbyStationRequest)).willReturn(nearbyStationPageResponse);

        // when
        mockMvc.perform(get("/stations/nearby")
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG))
                        .param("range", String.valueOf(RANGE))
                        .param("cursor", CURSOR)
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY)) // boolean 되는지 확인
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT)) // int 되는지 확인
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE)) // list 되는지 확인

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations.length()").value(stationResponses.size()))
                .andExpect(jsonPath("$.stations[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$.stations[0].statNm").value(STAT_NM))
                .andExpect(jsonPath("$.stations[0].distance").value(DISTANCE))
                .andExpect(jsonPath("$.stations[1].statId").value(SECOND_STAT_ID))
                .andExpect(jsonPath("$.stations[1].statNm").value(SECOND_STAT_NM))
                .andExpect(jsonPath("$.stations[1].distance").value(SECOND_DISTANCE))
                .andExpect(jsonPath("$.nextCursor").value(NEXT_CURSOR));

        verify(stationService).getNearbyStations(nearbyStationRequest);
    }

    @Test
    void 가까운_충전소_조회시_주변에_충전소가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(stationService.getNearbyStations(nearbyStationRequest)).willReturn(new NearbyStationPageResponse(List.of(), null));

        // when
        mockMvc.perform(get("/stations/nearby")
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG))
                        .param("range", String.valueOf(RANGE))
                        .param("cursor", CURSOR)
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations").isArray())
                .andExpect(jsonPath("$.stations").isEmpty());

        verify(stationService).getNearbyStations(nearbyStationRequest);
    }

    @Test
    void 가까운_충전소_조회시_lat_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/stations/nearby")
                        .param("lng", String.valueOf(LNG))
                        .param("range", String.valueOf(RANGE))
                        .param("cursor", CURSOR)
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

                // then
                .andExpect(status().isBadRequest());

        verify(stationService, never()).getNearbyStations(any());
    }

    @Test
    void 가까운_충전소_조회시_lat이_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/stations/nearby")
                        .param("lat", NOT_NUMBER_VALUE)
                        .param("lng", String.valueOf(LNG))
                        .param("range", String.valueOf(RANGE))
                        .param("cursor", CURSOR)
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

                // then
                .andExpect(status().isBadRequest());

        verify(stationService, never()).getNearbyStations(any());
    }

    // ========== GET /stations/bounds ==========

    @Test
    void 지도_범위_충전소_조회_성공시_200과_충전소_목록을_반환한다() throws Exception {
        // given
        given(stationService.getStationsInBounds(mapBoundsRequest)).willReturn(stationResponses);

        // when
        mockMvc.perform(get("/stations/bounds")
                .param("minLat", String.valueOf(MIN_LAT))
                .param("maxLat", String.valueOf(MAX_LAT))
                .param("minLng", String.valueOf(MIN_LNG))
                .param("maxLng", String.valueOf(MAX_LNG))
                .param("userLat", String.valueOf(LAT))
                .param("userLng", String.valueOf(LNG))
                .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(stationResponses.size()))
                .andExpect(jsonPath("$[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$[0].statNm").value(STAT_NM))
                .andExpect(jsonPath("$[0].distance").value(DISTANCE))
                .andExpect(jsonPath("$[1].statId").value(SECOND_STAT_ID))
                .andExpect(jsonPath("$[1].statNm").value(SECOND_STAT_NM))
                .andExpect(jsonPath("$[1].distance").value(SECOND_DISTANCE));

        verify(stationService).getStationsInBounds(mapBoundsRequest);
    }

    @Test
    void 지도_범위_충전소_조회시_범위_내_충전소가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(stationService.getStationsInBounds(mapBoundsRequest)).willReturn(List.of());

        // when
        mockMvc.perform(get("/stations/bounds")
                        .param("minLat", String.valueOf(MIN_LAT))
                        .param("maxLat", String.valueOf(MAX_LAT))
                        .param("minLng", String.valueOf(MIN_LNG))
                        .param("maxLng", String.valueOf(MAX_LNG))
                        .param("userLat", String.valueOf(LAT))
                        .param("userLng", String.valueOf(LNG))
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(stationService).getStationsInBounds(mapBoundsRequest);
    }

    @Test
    void 지도_범위_충전소_조회시_minLat_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/stations/bounds")
                        .param("maxLat", String.valueOf(MAX_LAT))
                        .param("minLng", String.valueOf(MIN_LNG))
                        .param("maxLng", String.valueOf(MAX_LNG))
                        .param("userLat", String.valueOf(LAT))
                        .param("userLng", String.valueOf(LNG))
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

                // then
                .andExpect(status().isBadRequest());

        verify(stationService, never()).getStationsInBounds(any());
    }

    @Test
    void 지도_범위_충전소_조회시_minLat이_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/stations/bounds")
                        .param("minLat", NOT_NUMBER_VALUE)
                        .param("maxLat", String.valueOf(MAX_LAT))
                        .param("minLng", String.valueOf(MIN_LNG))
                        .param("maxLng", String.valueOf(MAX_LNG))
                        .param("userLat", String.valueOf(LAT))
                        .param("userLng", String.valueOf(LNG))
                        .param("filter.availableOnly", String.valueOf(AVAILABLE_ONLY))
                        .param("filter.minOutput", String.valueOf(MIN_OUTPUT))
                        .param("filter.chgerTypes", CHGER_TYPE, SECOND_CHGER_TYPE))

                // then
                .andExpect(status().isBadRequest());

        verify(stationService, never()).getStationsInBounds(any());
    }

    // ========== GET /stations/{statId} ==========

    @Test
    void 충전소_상세_조회시_비로그인이면_user를_null로_넘기고_200을_반환한다() throws Exception {
        // given
        given(stationService.getStationDetail(null, STAT_ID)).willReturn(guestStationDetailResponse);

        // when
        mockMvc.perform(get("/stations/{statId}", STAT_ID))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statId").value(STAT_ID))
                .andExpect(jsonPath("$.statNm").value(STAT_NM))
                .andExpect(jsonPath("$.isFavorite").isEmpty())
                .andExpect(jsonPath("$.chargers[0].chgerId").value(CHGER_ID))
                .andExpect(jsonPath("$.chargers[0].isAlert").value(false));

        verify(stationService).getStationDetail(null, STAT_ID);
    }

    @Test
    void 충전소_상세_조회시_로그인이면_user를_넘기고_즐겨찾기와_알림_여부를_반환한다() throws Exception {
        // given
        given(stationService.getStationDetail(user, STAT_ID)).willReturn(userStationDetailResponse);

        // when
        mockMvc.perform(get("/stations/{statId}", STAT_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statId").value(STAT_ID))
                .andExpect(jsonPath("$.isFavorite").value(true)) // 즐찾
                .andExpect(jsonPath("$.chargers[0].isAlert").value(true)); // 알림 여부

        verify(stationService).getStationDetail(user, STAT_ID);
    }

    @Test
    void 충전소_상세_조회시_존재하지_않는_충전소면_400을_반환한다() throws Exception {
        // given
        given(stationService.getStationDetail(null, INVALID_STAT_ID)).willThrow(new IllegalArgumentException("유효하지 않은 충전소입니다."));

        // when
        mockMvc.perform(get("/stations/{statId}", INVALID_STAT_ID))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 충전소입니다."));

        verify(stationService).getStationDetail(null, INVALID_STAT_ID);
    }
}
