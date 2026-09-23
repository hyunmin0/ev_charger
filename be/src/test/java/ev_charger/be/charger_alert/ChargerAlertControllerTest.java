package ev_charger.be.charger_alert;

import ev_charger.be.charger_alert.dto.request.ChargerStatusRequest;
import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ChargerAlertController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
// 컨트롤러의 @Value("${internal.secret-key}")에 테스트용 키를 주입
@TestPropertySource(properties = "internal.secret-key=" + ChargerAlertControllerTest.INTERNAL_SECRET_KEY)
class ChargerAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChargerAlertService chargerAlertService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 파이썬 서버가 보내는 내부 키 (X-Internal-Key 헤더)
    static final String INTERNAL_SECRET_KEY = "test-internal-key";
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    // 틀린 내부 키
    private static final String INVALID_INTERNAL_KEY = "invalid-internal-key";

    // 사용 가능(waiting)으로 바뀐 충전기 정보
    private static final String STAT_ID = "ME000001";
    private static final String CHGER_ID = "01";
    private static final String SECOND_STAT_ID = "ME000002";
    private static final String SECOND_CHGER_ID = "02";

    // 알림 발송 요청 본문
    private List<ChargerStatusRequest> chargerStatusRequests;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();

        chargerStatusRequests = List.of(
                new ChargerStatusRequest(STAT_ID, CHGER_ID),
                new ChargerStatusRequest(SECOND_STAT_ID, SECOND_CHGER_ID)
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== POST /internal/charger-alerts/notify ==========

    @Test
    void 알림_발송_요청_성공시_200을_반환하고_알림을_발송한다() throws Exception {
        // when
        mockMvc.perform(post("/internal/charger-alerts/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargerStatusRequests))
                        .header(INTERNAL_KEY_HEADER, INTERNAL_SECRET_KEY))

        // then
                .andExpect(status().isOk());

        verify(chargerAlertService).notifyWaitingChargers(chargerStatusRequests);
    }

    @Test
    void 알림_발송_요청시_충전기_목록이_비어있어도_200을_반환한다() throws Exception {
        // given

        // when

        // then
    }

    @Test
    void 알림_발송_요청시_내부_키가_틀리면_403을_반환하고_알림을_발송하지_않는다() throws Exception {
        // given

        // when

        // then
    }

    @Test
    void 알림_발송_요청시_내부_키_헤더가_없으면_400을_반환한다() throws Exception {
        // given

        // when

        // then
    }

    @Test
    void 알림_발송_요청시_요청_본문이_없으면_400을_반환한다() throws Exception {
        // given

        // when

        // then
    }
}
