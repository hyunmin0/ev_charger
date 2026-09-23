package ev_charger.be.charger_alert;

import ev_charger.be.charger_alert.dto.response.UserChargerAlertResponse;
import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
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
        controllers = UserChargerAlertController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class UserChargerAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChargerAlertService chargerAlertService;

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

    // 알림 추가/해제 대상 충전기
    private static final String STAT_ID = "ME000001";
    private static final String CHGER_ID = "01";
    // 존재하지 않는 충전소/충전기
    private static final String INVALID_STAT_ID = "XX999999";
    private static final String INVALID_CHGER_ID = "99";

    // 알림 목록 조회 응답에 들어갈 충전소 정보
    private static final String STAT_NM = "테스트 충전소";
    private static final String USE_TIME = "24시간 이용가능";
    private static final String KIND = "공공시설";
    private static final String FLOOR_TYPE = "지상";
    private static final String BUSI_NM = "환경부";
    private static final String OUTPUT = "100";

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 알림 목록 조회 응답
    private List<UserChargerAlertResponse> userChargerAlertResponses;

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

        userChargerAlertResponses = List.of(
                new UserChargerAlertResponse(
                        STAT_ID,
                        STAT_NM,
                        USE_TIME,
                        true,  // parkingFree
                        true,  // openToPublic
                        KIND,
                        FLOOR_TYPE,
                        true,  // hasFast
                        BUSI_NM,
                        List.of(new UserChargerAlertResponse.AlertedCharger(CHGER_ID, ChgerType.DC_COMBO, OUTPUT, ChgerStat.CHARGING))
                )
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== GET /charger-alerts ==========

    @Test
    void 알림_목록_조회_성공시_200과_알림_목록을_반환한다() throws Exception {
        // given
        given(chargerAlertService.getChargerAlertsByUser(user)).willReturn(userChargerAlertResponses);

        // when
        mockMvc.perform(get("/charger-alerts")
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(userChargerAlertResponses.size()))
                .andExpect(jsonPath("$[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$[0].statNm").value(STAT_NM))
                .andExpect(jsonPath("$[0].useTime").value(USE_TIME))
                .andExpect(jsonPath("$[0].parkingFree").value(true))
                .andExpect(jsonPath("$[0].openToPublic").value(true))
                .andExpect(jsonPath("$[0].kind").value(KIND))
                .andExpect(jsonPath("$[0].floorType").value(FLOOR_TYPE))
                .andExpect(jsonPath("$[0].hasFast").value(true))
                .andExpect(jsonPath("$[0].busiNm").value(BUSI_NM))
                .andExpect(jsonPath("$[0].alertedChargers.length()").value(1))
                .andExpect(jsonPath("$[0].alertedChargers[0].chgerId").value(CHGER_ID))

                .andExpect(jsonPath("$[0].alertedChargers[0].chgerType").value(ChgerType.DC_COMBO.name()))
                .andExpect(jsonPath("$[0].alertedChargers[0].output").value(OUTPUT))
                .andExpect(jsonPath("$[0].alertedChargers[0].chgerStat").value(ChgerStat.CHARGING.name()));

        verify(chargerAlertService).getChargerAlertsByUser(user);
    }

    @Test
    void 알림_목록_조회시_등록된_알림이_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(chargerAlertService.getChargerAlertsByUser(user)).willReturn(List.of());

        // when
        mockMvc.perform(get("/charger-alerts")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(chargerAlertService).getChargerAlertsByUser(user);
    }

    @Test
    void 알림_목록_조회시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // given
        given(chargerAlertService.getChargerAlertsByUser(user)).willReturn(userChargerAlertResponses);

        // when
        mockMvc.perform(get("/charger-alerts"))

                // then
                .andExpect(status().isForbidden());

        verify(chargerAlertService, never()).getChargerAlertsByUser(any());
    }

    // ========== POST /charger-alerts/{statId}/{chgerId} ==========

    @Test
    void 알림_추가_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/charger-alerts/{statId}/{chgerId}", STAT_ID, CHGER_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(chargerAlertService).addChargerAlert(user, STAT_ID, CHGER_ID);
    }

    @Test
    void 알림_추가시_존재하지_않는_충전기면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("충전소 또는 충전기가 유효하지 않습니다.")).given(chargerAlertService).addChargerAlert(user, STAT_ID, INVALID_CHGER_ID);
        // when
        mockMvc.perform(post("/charger-alerts/{statId}/{chgerId}", STAT_ID, INVALID_CHGER_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전소 또는 충전기가 유효하지 않습니다."));

        verify(chargerAlertService).addChargerAlert(user, STAT_ID, INVALID_CHGER_ID);
    }

    @Test
    void 알림_추가시_이미_등록된_알림이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("이미 등록된 알림입니다.")).given(chargerAlertService).addChargerAlert(user, STAT_ID, CHGER_ID);
        // when
        mockMvc.perform(post("/charger-alerts/{statId}/{chgerId}", STAT_ID, CHGER_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록된 알림입니다."));

        verify(chargerAlertService).addChargerAlert(user, STAT_ID, CHGER_ID);
    }

    @Test
    void 알림_추가시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/charger-alerts/{statId}/{chgerId}", STAT_ID, CHGER_ID))

                // then
                .andExpect(status().isForbidden());

        verify(chargerAlertService, never()).addChargerAlert(any(), any(), any());
    }

    // ========== DELETE /charger-alerts/{statId}/{chgerId} ==========

    @Test
    void 알림_해제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/charger-alerts/{statId}/{chgerId}", STAT_ID, CHGER_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(chargerAlertService).deleteChargerAlert(user, STAT_ID, CHGER_ID);
    }

    @Test
    void 알림_해제시_존재하지_않는_충전기면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("충전소 또는 충전기가 유효하지 않습니다.")).given(chargerAlertService).deleteChargerAlert(user, STAT_ID, INVALID_CHGER_ID);

        // when
        mockMvc.perform(delete("/charger-alerts/{statId}/{chgerId}", STAT_ID, INVALID_CHGER_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("충전소 또는 충전기가 유효하지 않습니다."));

        verify(chargerAlertService).deleteChargerAlert(user, STAT_ID, INVALID_CHGER_ID);
    }

    @Test
    void 알림_해제시_등록되지_않은_알림이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("등록되지 않은 알림입니다.")).given(chargerAlertService).deleteChargerAlert(user, STAT_ID, CHGER_ID);

        // when
        mockMvc.perform(delete("/charger-alerts/{statId}/{chgerId}", STAT_ID, CHGER_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("등록되지 않은 알림입니다."));

        verify(chargerAlertService).deleteChargerAlert(user, STAT_ID, CHGER_ID);
    }

    @Test
    void 알림_해제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/charger-alerts/{statId}/{chgerId}", STAT_ID, CHGER_ID))

                // then
                .andExpect(status().isForbidden());

        verify(chargerAlertService, never()).deleteChargerAlert(any(), any(), any());
    }
}
