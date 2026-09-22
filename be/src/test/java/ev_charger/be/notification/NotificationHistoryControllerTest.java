package ev_charger.be.notification;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.notification.dto.NotificationHistoryResponse;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationHistoryController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class NotificationHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationHistoryService notificationHistoryService;

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

    // 알림 목록 조회 응답에 들어갈 알림 기록 정보
    private static final Long HISTORY_ID = 1L;
    private static final String STAT_ID = "ME000001";
    private static final String CHGER_ID = "01";
    private static final Long SECOND_HISTORY_ID = 2L;
    private static final String SECOND_STAT_ID = "ME000002";
    private static final String SECOND_CHGER_ID = "02";

    // 존재하지 않거나 본인 알림이 아닌 id
    private static final Long INVALID_HISTORY_ID = 999L;
    // Long PathVariable에 문자열을 넣는 타입 불일치 케이스
    private static final String NOT_NUMBER_HISTORY_ID = "abc";

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 알림 목록 조회 응답 (최신순: 읽지 않은 알림, 읽은 알림)
    private List<NotificationHistoryResponse> notificationHistoryResponses;

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

        notificationHistoryResponses = List.of(
                new NotificationHistoryResponse(HISTORY_ID, STAT_ID, CHGER_ID, LocalDateTime.now(), false),
                new NotificationHistoryResponse(SECOND_HISTORY_ID, SECOND_STAT_ID, SECOND_CHGER_ID, LocalDateTime.now().minusHours(1), true)
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== GET /notifications ==========

    @Test
    void 알림_목록_조회_성공시_200과_알림_목록을_반환한다() throws Exception {
        // given
        given(notificationHistoryService.getChargerAlertHistories(user)).willReturn(notificationHistoryResponses);

        // when
        mockMvc.perform(get("/notifications")
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(notificationHistoryResponses.size()))
                .andExpect(jsonPath("$[0].id").value(HISTORY_ID))
                .andExpect(jsonPath("$[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$[0].chgerId").value(CHGER_ID))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[1].id").value(SECOND_HISTORY_ID))
                .andExpect(jsonPath("$[1].statId").value(SECOND_STAT_ID))
                .andExpect(jsonPath("$[1].chgerId").value(SECOND_CHGER_ID))
                .andExpect(jsonPath("$[1].isRead").value(true));

        verify(notificationHistoryService).getChargerAlertHistories(user);
    }

    @Test
    void 알림_목록_조회시_알림_기록이_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(notificationHistoryService.getChargerAlertHistories(user)).willReturn(List.of());

        // when
        mockMvc.perform(get("/notifications")
                        .with(authentication(auth)))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(notificationHistoryService).getChargerAlertHistories(user);
    }

    @Test
    void 알림_목록_조회시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/notifications"))

        // then
                .andExpect(status().isForbidden());

        verify(notificationHistoryService, never()).getChargerAlertHistories(any());
    }

    // ========== PATCH /notifications/{id}/read ==========

    @Test
    void 알림_읽음_처리_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/notifications/{id}/read", HISTORY_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(notificationHistoryService).markAsRead(user, HISTORY_ID);
    }

    @Test
    void 알림_읽음_처리시_id가_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/notifications/{id}/read", NOT_NUMBER_HISTORY_ID)
                        .with(authentication(auth)))

        // then
                .andExpect(status().isBadRequest());

        verify(notificationHistoryService, never()).markAsRead(any(), anyLong());
    }

    @Test
    void 알림_읽음_처리시_존재하지_않거나_본인_알림이_아니면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("알림 기록이 없습니다.")).given(notificationHistoryService).markAsRead(user, INVALID_HISTORY_ID);

        // when
        mockMvc.perform(patch("/notifications/{id}/read", INVALID_HISTORY_ID)
                        .with(authentication(auth)))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("알림 기록이 없습니다."));

        verify(notificationHistoryService).markAsRead(user, INVALID_HISTORY_ID);
    }

    @Test
    void 알림_읽음_처리시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/notifications/{id}/read", HISTORY_ID))

        // then
                .andExpect(status().isForbidden());

        verify(notificationHistoryService, never()).markAsRead(any(), anyLong());
    }

    // ========== DELETE /notifications/{id} ==========

    @Test
    void 알림_단건_삭제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/notifications/{id}", HISTORY_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(notificationHistoryService).deleteHistory(user, HISTORY_ID);
    }

    @Test
    void 알림_단건_삭제시_id가_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/notifications/{id}", NOT_NUMBER_HISTORY_ID)
                        .with(authentication(auth)))

        // then
                .andExpect(status().isBadRequest());

        verify(notificationHistoryService, never()).deleteHistory(any(), anyLong());
    }

    @Test
    void 알림_단건_삭제시_존재하지_않거나_본인_알림이_아니면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("알림 기록이 없습니다.")).given(notificationHistoryService).deleteHistory(user, INVALID_HISTORY_ID);

        // when
        mockMvc.perform(delete("/notifications/{id}", INVALID_HISTORY_ID)
                        .with(authentication(auth)))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("알림 기록이 없습니다."));

        verify(notificationHistoryService).deleteHistory(user, INVALID_HISTORY_ID);
    }

    @Test
    void 알림_단건_삭제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/notifications/{id}", HISTORY_ID))

        // then
                .andExpect(status().isForbidden());

        verify(notificationHistoryService, never()).deleteHistory(any(), anyLong());
    }

    // ========== DELETE /notifications ==========

    @Test
    void 알림_전체_삭제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/notifications")
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(notificationHistoryService).deleteAllChargerAlertHistories(user);
    }

    @Test
    void 알림_전체_삭제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/notifications"))

        // then
                .andExpect(status().isForbidden());

        verify(notificationHistoryService, never()).deleteAllChargerAlertHistories(any());
    }
}
