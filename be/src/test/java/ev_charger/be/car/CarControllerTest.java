package ev_charger.be.car;

import ev_charger.be.car.charge.ChargeRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CarController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // CarController는 서비스 없이 리포지토리를 직접 사용
    @MockitoBean
    private CarRepository carRepository;

    // 충전 시간 조회(/cars/{carId}/charges)에서 사용
    @MockitoBean
    private ChargeRepository chargeRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 검색어 (브랜드 또는 모델명에 포함)
    private static final String KEYWORD = "아이오닉";
    // 검색 결과가 없는 검색어
    private static final String NOT_FOUND_KEYWORD = "없는차량";

    // 검색 결과에 들어갈 차량 정보
    private static final long CAR_ID = 1L;
    private static final String BRAND = "현대";
    private static final String MODEL = "아이오닉 5";
    private static final String TRIM = "롱레인지";
    private static final int MODEL_YEAR = 2024;
    private static final float BATTERY_CAPACITY = 84.0f;
    // trim이 없는 차량
    private static final long SECOND_CAR_ID = 2L;
    private static final String SECOND_MODEL = "아이오닉 6";
    private static final int SECOND_MODEL_YEAR = 2023;
    private static final float SECOND_BATTERY_CAPACITY = 77.4f;

    // 검색 결과 (trim 있는 차량, trim 없는 차량)
    private List<Car> cars;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();

        Car car = Car.builder()
                .brand(BRAND)
                .model(MODEL)
                .trim(TRIM)
                .modelYear(MODEL_YEAR)
                .batteryCapacity(BATTERY_CAPACITY)
                .batteryType("NCM")
                .driveType("2WD")
                .wheelSize(19)
                .build();
        ReflectionTestUtils.setField(car, "carId", CAR_ID);

        Car secondCar = Car.builder()
                .brand(BRAND)
                .model(SECOND_MODEL)
                .modelYear(SECOND_MODEL_YEAR)
                .batteryCapacity(SECOND_BATTERY_CAPACITY)
                .batteryType("NCM")
                .driveType("2WD")
                .wheelSize(18)
                .build();
        ReflectionTestUtils.setField(secondCar, "carId", SECOND_CAR_ID);

        cars = List.of(car, secondCar);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== GET /cars ==========

    @Test
    void 차량_검색_성공시_200과_차량_목록을_반환한다() throws Exception {
        // given
        given(carRepository.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(KEYWORD, KEYWORD)).willReturn(cars);

        // when
        mockMvc.perform(get("/cars")
                .param("keyword", KEYWORD))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(cars.size()))
                .andExpect(jsonPath("$[0].carId").value(CAR_ID))
                .andExpect(jsonPath("$[0].brand").value(BRAND))
                .andExpect(jsonPath("$[0].model").value(MODEL))
                .andExpect(jsonPath("$[0].trim").value(TRIM))
                .andExpect(jsonPath("$[1].carId").value(SECOND_CAR_ID))
                .andExpect(jsonPath("$[1].model").value(SECOND_MODEL));

        verify(carRepository).findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(KEYWORD, KEYWORD);
    }

    @Test
    void 차량_검색시_trim이_없는_차량은_trim을_null로_반환한다() throws Exception {
        // given
        given(carRepository.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(KEYWORD, KEYWORD)).willReturn(cars);

        // when
        mockMvc.perform(get("/cars")
                        .param("keyword", KEYWORD))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trim").value(TRIM))
                .andExpect(jsonPath("$[1].trim").isEmpty());

        verify(carRepository).findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(KEYWORD, KEYWORD);
    }

    @Test
    void 차량_검색시_검색_결과가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(carRepository.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(NOT_FOUND_KEYWORD, NOT_FOUND_KEYWORD)).willReturn(List.of());

        // when
        mockMvc.perform(get("/cars")
                        .param("keyword", NOT_FOUND_KEYWORD))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(carRepository).findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(NOT_FOUND_KEYWORD, NOT_FOUND_KEYWORD);
    }

    @Test
    void 차량_검색시_keyword_파라미터가_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/cars"))

                // then
                .andExpect(status().isBadRequest());

        verify(carRepository, never()).findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(any(), any());
    }
}
