package ev_charger.be.integration;

import ev_charger.be.car.Car;
import ev_charger.be.common.enums.YN;
import ev_charger.be.config.IntegrationTestSupport;
import ev_charger.be.station.Station;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.userCar.UserCarRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 로그인한 상태로 여러 API를 이어서 호출하는 시나리오
// 컨트롤러 -> 서비스 -> 리포지토리 -> DB까지 실제로 거쳐서 결과가 다음 API에 반영되는지 검증
class LoggedInUserIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserCarRepository userCarRepository;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // 서울시청 위치 (충전소 위치 + 즐겨찾기 목록 조회 시 내 위치)
    private static final double LAT = 37.5665;
    private static final double LNG = 126.9780;
    private static final String STAT_ID = "TS000001";

    private User user;
    private User secondUser;
    private Car car;

    @BeforeEach
    void setUp() {
        user = persistUser("테스터", "google-1234");
        secondUser = persistUser("다른유저", "google-5678");

        StationOperator operator = StationOperator.builder()
                .busiId("ME")
                .busiNm("기후에너지환경부")
                .busiCall("02-1234-5678")
                .build();
        em.persist(operator);
        Station station = Station.builder()
                .statId(STAT_ID)
                .statNm("서울시청 충전소")
                .addr("서울특별시 중구 세종대로 110")
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(LNG, LAT)))
                .useTime("24시간 이용가능")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.PUBLIC)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();
        em.persist(station);
        // 충전소 목록 쿼리(즐겨찾기 목록 등)는 charger와 inner join이라 충전기가 1대 이상 있어야 조회됨
        em.persist(Charger.builder()
                .station(station)
                .chgerId("01")
                .chgerType(ChgerType.DC_COMBO)
                .chgerStat(ChgerStat.WAITING)
                .statUpdDt("20261005120000")
                .build());

        car = Car.builder()
                .brand("현대")
                .model("아이오닉5")
                .batteryType("NCM")
                .modelYear(2024)
                .driveType("2WD")
                .wheelSize(19)
                .batteryCapacity(84.0f)
                .build();
        em.persist(car);

        em.flush();
    }

    // ===== 리뷰 =====

    @Test
    void 리뷰를_작성하면_충전소_별점_요약과_내_리뷰_목록에_반영된다() throws Exception {
        // given
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(5, "충전 속도가 빨라요")))
                .andExpect(status().isOk());

        // when
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                        .header("Authorization", bearer(secondUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(4, "주차 공간이 좁아요")))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/reviews/station/{statId}/summary", STAT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2));

        mockMvc.perform(get("/reviews/my")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // secondUser의 리뷰는 제외
                .andExpect(jsonPath("$[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void 같은_충전소에_리뷰를_두번_작성하면_실패한다() throws Exception {
        // given
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(5, "충전 속도가 빨라요")))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(3, "다시 와보니 별로예요")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록한 리뷰가 있습니다."));
    }

    // ===== 즐겨찾기 =====

    @Test
    void 즐겨찾기를_추가하면_목록에_나오고_삭제하면_사라진다() throws Exception {
        // given
        mockMvc.perform(post("/favorites/{statId}", STAT_ID)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/favorites")
                        .header("Authorization", bearer(user))
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].statId").value(STAT_ID));

        // when
        mockMvc.perform(delete("/favorites/{statId}", STAT_ID)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/favorites")
                        .header("Authorization", bearer(user))
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void 다른_유저의_즐겨찾기는_내_목록에_나오지_않는다() throws Exception {
        // given
        mockMvc.perform(post("/favorites/{statId}", STAT_ID)
                        .header("Authorization", bearer(secondUser)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/favorites")
                        .header("Authorization", bearer(user))
                        .param("lat", String.valueOf(LAT))
                        .param("lng", String.valueOf(LNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ===== 내 차량 =====

    @Test
    void 차량을_등록하면_내_차량_목록에_나온다() throws Exception {
        // given

        // when
        mockMvc.perform(post("/user/cars")
                        .header("Authorization", bearer(user))
                        .param("carId", String.valueOf(car.getCarId()))
                        .param("batteryCapacity", "84.0"))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/user/cars")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].carId").value(car.getCarId()))
                .andExpect(jsonPath("$[0].carName").value("현대 아이오닉5"));
    }

    @Test
    void 같은_차량을_두번_등록하면_실패한다() throws Exception {
        // given
        mockMvc.perform(post("/user/cars")
                        .header("Authorization", bearer(user))
                        .param("carId", String.valueOf(car.getCarId()))
                        .param("batteryCapacity", "84.0"))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/user/cars")
                        .header("Authorization", bearer(user))
                        .param("carId", String.valueOf(car.getCarId()))
                        .param("batteryCapacity", "84.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록된 차량입니다."));
    }

    @Test
    void 다른_유저의_차량은_삭제할_수_없다() throws Exception {
        // given
        mockMvc.perform(post("/user/cars")
                        .header("Authorization", bearer(secondUser))
                        .param("carId", String.valueOf(car.getCarId()))
                        .param("batteryCapacity", "84.0"))
                .andExpect(status().isOk());
        Long secondUserCarId = userCarRepository.findByUser(secondUser).get(0).getUserCarId();

        // when & then
        mockMvc.perform(delete("/user/cars/{userCarId}", secondUserCarId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않거나 본인 차량이 아닙니다."));

        mockMvc.perform(get("/user/cars")
                        .header("Authorization", bearer(secondUser)))
                .andExpect(jsonPath("$", hasSize(1))); // 삭제되지 않고 그대로 남아 있음
    }

    private User persistUser(String nickname, String providerId) {
        User newUser = User.builder()
                .nickname(nickname)
                .email(providerId + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        em.persist(newUser);
        return newUser;
    }

    private String reviewJson(int rating, String content) {
        return """
                {"rating": %d, "content": "%s"}
                """.formatted(rating, content);
    }
}
