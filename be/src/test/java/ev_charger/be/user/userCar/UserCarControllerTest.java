package ev_charger.be.user.userCar;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.userCar.dto.response.UserCarResponse;
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
import static org.mockito.ArgumentMatchers.anyFloat;
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
        controllers = UserCarController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class UserCarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCarService userCarService;

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

    // 차량 추가 요청 파라미터
    private static final long CAR_ID = 1L;
    private static final float BATTERY_CAPACITY = 77.4f;
    // DB에 없는 차량 id
    private static final long INVALID_CAR_ID = 999L;
    // 0 이하의 배터리 용량
    private static final float INVALID_BATTERY_CAPACITY = 0f;
    // long/float 파라미터에 문자열을 넣는 타입 불일치 케이스
    private static final String NOT_NUMBER_VALUE = "abc";

    // 차량 목록 조회 응답에 들어갈 차량 정보
    private static final UUID USER_CAR_ID = UUID.randomUUID();
    private static final String CAR_NAME = "아이오닉 5";
    private static final UUID SECOND_USER_CAR_ID = UUID.randomUUID();
    private static final long SECOND_CAR_ID = 2L;
    private static final String SECOND_CAR_NAME = "EV6";
    private static final float SECOND_BATTERY_CAPACITY = 84.0f;

    // 차량 삭제: 존재하지 않거나 본인 차량이 아닌 id
    private static final UUID INVALID_USER_CAR_ID = UUID.randomUUID();
    // UUID PathVariable에 UUID 형식이 아닌 값을 넣는 타입 불일치 케이스
    private static final String NOT_UUID_USER_CAR_ID = "not-uuid";

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 차량 목록 조회 응답
    private List<UserCarResponse> userCarResponses;

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

        userCarResponses = List.of(
                new UserCarResponse(USER_CAR_ID, CAR_ID, CAR_NAME, BATTERY_CAPACITY),
                new UserCarResponse(SECOND_USER_CAR_ID, SECOND_CAR_ID, SECOND_CAR_NAME, SECOND_BATTERY_CAPACITY)
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== POST /user/cars ==========

    @Test
    void 차량_추가_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/cars")
                .with(authentication(auth))
                .param("carId", String.valueOf(CAR_ID))
                .param("batteryCapacity", String.valueOf(BATTERY_CAPACITY)))

        // then
                .andExpect(status().isOk());

        verify(userCarService).addUserCar(user, CAR_ID, BATTERY_CAPACITY);
    }

    @Test
    void 차량_추가시_carId_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("batteryCapacity", String.valueOf(BATTERY_CAPACITY)))

        // then
                .andExpect(status().isBadRequest());

        verify(userCarService, never()).addUserCar(any(), anyLong(), anyFloat());
    }

    @Test
    void 차량_추가시_batteryCapacity_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("carId", String.valueOf(CAR_ID)))

        // then
                .andExpect(status().isBadRequest());

        verify(userCarService, never()).addUserCar(any(), anyLong(), anyFloat());
    }

    @Test
    void 차량_추가시_carId가_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("carId", NOT_NUMBER_VALUE)
                        .param("batteryCapacity", String.valueOf(BATTERY_CAPACITY)))

        // then
                .andExpect(status().isBadRequest());

        verify(userCarService, never()).addUserCar(any(), anyLong(), anyFloat());
    }

    @Test
    void 차량_추가시_batteryCapacity가_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("carId", String.valueOf(CAR_ID))
                        .param("batteryCapacity", NOT_NUMBER_VALUE))

        // then
                .andExpect(status().isBadRequest());

        verify(userCarService, never()).addUserCar(any(), anyLong(), anyFloat());
    }

    @Test
    void 차량_추가시_배터리_용량이_0_이하면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("배터리 용량은 0보다 커야 합니다.")).given(userCarService).addUserCar(user, CAR_ID, INVALID_BATTERY_CAPACITY);

        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("carId", String.valueOf(CAR_ID))
                        .param("batteryCapacity", String.valueOf(INVALID_BATTERY_CAPACITY)))

        // then
                .andExpect(status().isBadRequest());

        verify(userCarService).addUserCar(user, CAR_ID, INVALID_BATTERY_CAPACITY);
    }

    @Test
    void 차량_추가시_존재하지_않는_차량_id면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("유효하지 않은 차량입니다.")).given(userCarService).addUserCar(user, INVALID_CAR_ID, BATTERY_CAPACITY);

        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("carId", String.valueOf(INVALID_CAR_ID))
                        .param("batteryCapacity", String.valueOf(BATTERY_CAPACITY)))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 차량입니다."));

        verify(userCarService).addUserCar(user, INVALID_CAR_ID, BATTERY_CAPACITY);
    }

    @Test
    void 차량_추가시_이미_등록된_차량이면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("이미 등록된 차량입니다.")).given(userCarService).addUserCar(user, CAR_ID, BATTERY_CAPACITY);

        // when
        mockMvc.perform(post("/user/cars")
                        .with(authentication(auth))
                        .param("carId", String.valueOf(CAR_ID))
                        .param("batteryCapacity", String.valueOf(BATTERY_CAPACITY)))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록된 차량입니다."));

        verify(userCarService).addUserCar(user, CAR_ID, BATTERY_CAPACITY);
    }

    @Test
    void 차량_추가시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/user/cars")
                        .param("carId", String.valueOf(CAR_ID))
                        .param("batteryCapacity", String.valueOf(BATTERY_CAPACITY)))

        // then
                .andExpect(status().isForbidden());

        verify(userCarService, never()).addUserCar(any(), anyLong(), anyFloat());
    }

    // ========== GET /user/cars ==========

    @Test
    void 차량_목록_조회_성공시_200과_차량_목록을_반환한다() throws Exception {
        // given
        given(userCarService.getUserCarList(user)).willReturn(userCarResponses);

        // when
        mockMvc.perform(get("/user/cars")
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(userCarResponses.size()))
                .andExpect(jsonPath("$[0].userCarId").value(USER_CAR_ID.toString()))
                .andExpect(jsonPath("$[0].carName").value(CAR_NAME))
                .andExpect(jsonPath("$[0].batteryCapacity").value(BATTERY_CAPACITY))
                .andExpect(jsonPath("$[1].userCarId").value(SECOND_USER_CAR_ID.toString()))
                .andExpect(jsonPath("$[1].carName").value(SECOND_CAR_NAME))
                .andExpect(jsonPath("$[1].batteryCapacity").value(SECOND_BATTERY_CAPACITY));

        verify(userCarService).getUserCarList(user);
    }

    @Test
    void 차량_목록_조회시_등록된_차량이_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(userCarService.getUserCarList(user)).willReturn(List.of());

        // when
        mockMvc.perform(get("/user/cars")
                        .with(authentication(auth)))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userCarService).getUserCarList(user);
    }

    @Test
    void 차량_목록_조회시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/user/cars"))

        // then
                .andExpect(status().isForbidden());

        verify(userCarService, never()).getUserCarList(any());
    }

    // ========== DELETE /user/cars/{userCarId} ==========

    @Test
    void 차량_삭제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/user/cars/{userCarId}", USER_CAR_ID.toString())
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(userCarService).deleteUserCar(user, USER_CAR_ID);
    }

    @Test
    void 차량_삭제시_userCarId가_UUID_형식이_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/user/cars/{userCarId}", NOT_UUID_USER_CAR_ID)
                        .with(authentication(auth)))

        // then
                .andExpect(status().isBadRequest());

        verify(userCarService, never()).deleteUserCar(any(), any());
    }

    @Test
    void 차량_삭제시_존재하지_않거나_본인_차량이_아니면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("존재하지 않거나 본인 차량이 아닙니다.")).given(userCarService).deleteUserCar(user, INVALID_USER_CAR_ID);

        // when
        mockMvc.perform(delete("/user/cars/{userCarId}", INVALID_USER_CAR_ID.toString())
                        .with(authentication(auth)))

        // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않거나 본인 차량이 아닙니다."));

        verify(userCarService).deleteUserCar(user, INVALID_USER_CAR_ID);
    }

    @Test
    void 차량_삭제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/user/cars/{userCarId}", USER_CAR_ID.toString()))

                // then
                .andExpect(status().isForbidden());

        verify(userCarService, never()).deleteUserCar(any(), any());
    }
}
