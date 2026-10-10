package ev_charger.be.station;

import ev_charger.be.common.enums.YN;
import ev_charger.be.config.JpaAuditingConfig;
import ev_charger.be.config.TestcontainersConfig;
import ev_charger.be.favorite.Favorite;
import ev_charger.be.review.Review;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.congestion.Congestion;
import ev_charger.be.station.congestion.CongestionLevel;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.RegionSummaryResponse;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.within;

// 스키마는 table.sql로 생성 (TestcontainersConfig)
// validate: 엔티티와 table.sql이 다르면 테스트 시작 시 실패
// PhysicalNamingStrategyStandardImpl + globally_quoted_identifiers: @Column 이름("statId")을 대소문자 그대로 사용 (application.yml과 동일하게 유지)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        // columnDefinition("geography(Point, 4326)")은 따옴표 대상에서 제외
        "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// JpaAuditingConfig: 리뷰, 즐겨찾기의 created_at(@CreatedDate) 자동 입력 (@DataJpaTest는 자동으로 안 불러옴)
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
class StationRepositoryImplTest {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TestEntityManager em;

    // ===== 샘플 데이터 =====
    // 서울시청을 기준 위치로, 반경 3km 안 충전소 5개 + 반경 밖 1개 (sql/sample_data.sql과 같은 구성)
    //  충전소   거리    주차/개방/층          시설       충전기 (타입코드 상태 출력kW)          리뷰          혼잡도
    //  서울시청 ~0m    무료/개방/지상        공공(A0)   01 대기 100, 02 충전중 7              2개(평균 4.5) 보통
    //  덕수궁   ~270m  유료/개방/지하        주차(B0)   04 대기 200, 04 대기 200              없음          예측 2개 중 최신값 여유
    //  시청역   ~400m  유료/개방/지하        근린(I0)   02 미확인 7                           없음          없음
    //  광화문   ~1km   무료/이용제한/지상    상업(E0)   02 충전중 7, 07 충전중 22 (급속 없음) 1개(3점)      없음
    //  을지로   ~1.1km 무료/개방/지상        공공(A0)   01 점검중 50, 04 운영중지 100         없음          행은 있지만 null이어야 함
    //  강남역   ~9km   반경 밖
    //  충전기 타입: 01 DC차데모, 02 AC완속, 04 DC콤보, 07 AC3상 (02/07/08은 급속 아님)

    // 위치 좌표 생성용 (SRID 4326 = 위경도)
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // 조회 기준 위치 (서울시청)
    private static final double USER_LAT = 37.5665;
    private static final double USER_LNG = 126.9780;
    // 조회 반경(m)
    private static final int RANGE = 3000;
    // 지도 영역 조회 상한 (상한에 걸리지 않는 값)
    private static final int MAX_COUNT = 500;

    // 충전소 운영기관
    private static final String BUSI_ID = "ME";
    private static final String BUSI_NM = "기후에너지환경부";
    private static final String BUSI_CALL = "02-1234-5678";

    // 충전소 공통 값
    private static final String USE_TIME = "24시간 이용가능";
    private static final String ZCODE = "11";

    // 서울시청 충전소 (기준 위치와 같은 좌표 → 거리 0m)
    private static final String CITY_HALL_STAT_ID = "TS000001";
    private static final String CITY_HALL_STAT_NM = "서울시청 충전소";
    private static final String CITY_HALL_ADDR = "서울특별시 중구 세종대로 110";
    // 덕수궁 충전소 (약 270m)
    private static final String DEOKSUGUNG_STAT_ID = "TS000002";
    private static final String DEOKSUGUNG_STAT_NM = "덕수궁 공영주차장 충전소";
    private static final String DEOKSUGUNG_ADDR = "서울특별시 중구 세종대로 99";
    private static final double DEOKSUGUNG_LAT = 37.5658;
    private static final double DEOKSUGUNG_LNG = 126.9751;
    // 광화문 충전소 (약 1km)
    private static final String GWANGHWAMUN_STAT_ID = "TS000003";
    private static final String GWANGHWAMUN_STAT_NM = "광화문 충전소";
    private static final String GWANGHWAMUN_ADDR = "서울특별시 종로구 세종대로 172";
    private static final double GWANGHWAMUN_LAT = 37.5759;
    private static final double GWANGHWAMUN_LNG = 126.9769;
    // 을지로 충전소 (약 1.1km)
    private static final String EULJIRO_STAT_ID = "TS000004";
    private static final String EULJIRO_STAT_NM = "을지로 충전소";
    private static final String EULJIRO_ADDR = "서울특별시 중구 을지로 100";
    private static final double EULJIRO_LAT = 37.5660;
    private static final double EULJIRO_LNG = 126.9910;
    // 시청역 충전소 (약 400m)
    private static final String CITY_HALL_EXIT_STAT_ID = "TS000005";
    private static final String CITY_HALL_EXIT_STAT_NM = "시청역 충전소";
    private static final String CITY_HALL_EXIT_ADDR = "서울특별시 중구 을지로 12";
    private static final double CITY_HALL_EXIT_LAT = 37.5700;
    private static final double CITY_HALL_EXIT_LNG = 126.9800;
    // 강남역 충전소 (약 9km, 반경 밖)
    private static final String GANGNAM_STAT_ID = "TS000006";
    private static final String GANGNAM_STAT_NM = "강남역 충전소";
    private static final String GANGNAM_ADDR = "서울특별시 강남구 강남대로 396";
    private static final double GANGNAM_LAT = 37.4979;
    private static final double GANGNAM_LNG = 127.0276;

    // 충전기 (charger)
    private static final String CHGER_ID = "01";
    private static final String SECOND_CHGER_ID = "02";
    private static final String STAT_UPD_DT = "20260930120000";
    private static final String SLOW_OUTPUT = "7";
    private static final String AC3_OUTPUT = "22";
    private static final String FAST_OUTPUT = "100";
    private static final String LOW_FAST_OUTPUT = "50";
    private static final String HIGH_FAST_OUTPUT = "200";

    // 리뷰 작성 유저
    private static final String NICKNAME = "테스터";
    private static final String EMAIL = "test@example.com";
    private static final String PROVIDER_ID = "google-1234";
    private static final String SECOND_NICKNAME = "다른유저";
    private static final String SECOND_EMAIL = "other@example.com";
    private static final String SECOND_PROVIDER_ID = "kakao-5678";

    // 리뷰
    private static final int RATING = 5;
    private static final String CONTENT = "충전 속도가 빨라요";
    private static final int SECOND_RATING = 4;
    private static final String SECOND_CONTENT = "주차 공간이 좁아요";
    private static final int THIRD_RATING = 3;
    private static final String THIRD_CONTENT = "항상 사람이 많아요";

    // 혼잡도 (targetTime = 1: 1시간 뒤 예측)
    private static final int ONE_HOUR = 1;

    // 운영기관 (station.busiId FK 대상)
    private StationOperator operator;
    // 리뷰, 즐겨찾기 작성 유저
    private User user;
    private User secondUser;
    // 충전소
    private Station cityHallStation;
    private Station deoksugungStation;
    private Station gwanghwamunStation;
    private Station euljiroStation;
    private Station cityHallExitStation;
    private Station gangnamStation;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        // ----- 운영기관, 유저 -----
        operator = em.persist(StationOperator.builder()
                .busiId(BUSI_ID)
                .busiNm(BUSI_NM)
                .busiCall(BUSI_CALL)
                .build());

        user = em.persist(User.builder()
                .nickname(NICKNAME)
                .email(EMAIL)
                .provider(Provider.GOOGLE)
                .providerId(PROVIDER_ID)
                .build());
        secondUser = em.persist(User.builder()
                .nickname(SECOND_NICKNAME)
                .email(SECOND_EMAIL)
                .provider(Provider.KAKAO)
                .providerId(SECOND_PROVIDER_ID)
                .build());

        // ----- 충전소 -----
        // Coordinate(경도, 위도) 순서 주의
        cityHallStation = em.persist(Station.builder()
                .statId(CITY_HALL_STAT_ID)
                .statNm(CITY_HALL_STAT_NM)
                .addr(CITY_HALL_ADDR)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(USER_LNG, USER_LAT)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(ZCODE)
                .kind(Kind.PUBLIC)
                .parkingFree(YN.Y)
                .limitYn(YN.N) // 개방
                .floorType(FloorType.F)
                .build());
        deoksugungStation = em.persist(Station.builder()
                .statId(DEOKSUGUNG_STAT_ID)
                .statNm(DEOKSUGUNG_STAT_NM)
                .addr(DEOKSUGUNG_ADDR)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(DEOKSUGUNG_LNG, DEOKSUGUNG_LAT)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(ZCODE)
                .kind(Kind.PARKING)
                .parkingFree(YN.N)
                .limitYn(YN.N)
                .floorType(FloorType.B)
                .build());
        gwanghwamunStation = em.persist(Station.builder()
                .statId(GWANGHWAMUN_STAT_ID)
                .statNm(GWANGHWAMUN_STAT_NM)
                .addr(GWANGHWAMUN_ADDR)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(GWANGHWAMUN_LNG, GWANGHWAMUN_LAT)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(ZCODE)
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.Y) // 이용제한
                .floorType(FloorType.F)
                .build());
        euljiroStation = em.persist(Station.builder()
                .statId(EULJIRO_STAT_ID)
                .statNm(EULJIRO_STAT_NM)
                .addr(EULJIRO_ADDR)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(EULJIRO_LNG, EULJIRO_LAT)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(ZCODE)
                .kind(Kind.PUBLIC)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build());
        cityHallExitStation = em.persist(Station.builder()
                .statId(CITY_HALL_EXIT_STAT_ID)
                .statNm(CITY_HALL_EXIT_STAT_NM)
                .addr(CITY_HALL_EXIT_ADDR)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(CITY_HALL_EXIT_LNG, CITY_HALL_EXIT_LAT)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(ZCODE)
                .kind(Kind.NEIGHBORHOOD)
                .parkingFree(YN.N)
                .limitYn(YN.N)
                .floorType(FloorType.B)
                .build());
        gangnamStation = em.persist(Station.builder()
                .statId(GANGNAM_STAT_ID)
                .statNm(GANGNAM_STAT_NM)
                .addr(GANGNAM_ADDR)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(GANGNAM_LNG, GANGNAM_LAT)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(ZCODE)
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build());

        // ----- 충전기 -----
        // 서울시청: 급속 충전대기 + 완속 충전중
        em.persist(Charger.builder().station(cityHallStation).chgerId(CHGER_ID)
                .chgerType(ChgerType.DC_DEMO).chgerStat(ChgerStat.WAITING)
                .statUpdDt(STAT_UPD_DT).output(FAST_OUTPUT).build());
        em.persist(Charger.builder().station(cityHallStation).chgerId(SECOND_CHGER_ID)
                .chgerType(ChgerType.AC_SLOW).chgerStat(ChgerStat.CHARGING)
                .statUpdDt(STAT_UPD_DT).output(SLOW_OUTPUT).build());
        // 덕수궁: 급속 2대 모두 충전대기
        em.persist(Charger.builder().station(deoksugungStation).chgerId(CHGER_ID)
                .chgerType(ChgerType.DC_COMBO).chgerStat(ChgerStat.WAITING)
                .statUpdDt(STAT_UPD_DT).output(HIGH_FAST_OUTPUT).build());
        em.persist(Charger.builder().station(deoksugungStation).chgerId(SECOND_CHGER_ID)
                .chgerType(ChgerType.DC_COMBO).chgerStat(ChgerStat.WAITING)
                .statUpdDt(STAT_UPD_DT).output(HIGH_FAST_OUTPUT).build());
        // 광화문: 완속 + AC3상 (둘 다 hasFast 계산에서 제외), 모두 충전중
        em.persist(Charger.builder().station(gwanghwamunStation).chgerId(CHGER_ID)
                .chgerType(ChgerType.AC_SLOW).chgerStat(ChgerStat.CHARGING)
                .statUpdDt(STAT_UPD_DT).output(SLOW_OUTPUT).build());
        em.persist(Charger.builder().station(gwanghwamunStation).chgerId(SECOND_CHGER_ID)
                .chgerType(ChgerType.AC3).chgerStat(ChgerStat.CHARGING)
                .statUpdDt(STAT_UPD_DT).output(AC3_OUTPUT).build());
        // 을지로: 점검중 + 운영중지 → allUnavailable
        em.persist(Charger.builder().station(euljiroStation).chgerId(CHGER_ID)
                .chgerType(ChgerType.DC_DEMO).chgerStat(ChgerStat.INSPECTION)
                .statUpdDt(STAT_UPD_DT).output(LOW_FAST_OUTPUT).build());
        em.persist(Charger.builder().station(euljiroStation).chgerId(SECOND_CHGER_ID)
                .chgerType(ChgerType.DC_COMBO).chgerStat(ChgerStat.SUSPENDED)
                .statUpdDt(STAT_UPD_DT).output(FAST_OUTPUT).build());
        // 시청역: 상태미확인 → allUnknown
        em.persist(Charger.builder().station(cityHallExitStation).chgerId(CHGER_ID)
                .chgerType(ChgerType.AC_SLOW).chgerStat(ChgerStat.UNCONFIRMED)
                .statUpdDt(STAT_UPD_DT).output(SLOW_OUTPUT).build());
        // 강남역: 반경 밖
        em.persist(Charger.builder().station(gangnamStation).chgerId(CHGER_ID)
                .chgerType(ChgerType.DC_COMBO).chgerStat(ChgerStat.WAITING)
                .statUpdDt(STAT_UPD_DT).output(FAST_OUTPUT).build());

        // ----- 리뷰 -----
        // 서울시청: 충전기 2대 x 리뷰 2개 (중복 집계 확인용)
        em.persist(Review.builder().user(user).station(cityHallStation).rating(RATING).content(CONTENT).build());
        em.persist(Review.builder().user(secondUser).station(cityHallStation).rating(SECOND_RATING).content(SECOND_CONTENT).build());
        // 광화문: 충전기 2대 x 리뷰 1개
        em.persist(Review.builder().user(user).station(gwanghwamunStation).rating(THIRD_RATING).content(THIRD_CONTENT).build());

        // ----- 혼잡도 -----
        // 서울시청: 1시간 예측
        em.persist(Congestion.builder().station(cityHallStation).targetTime(ONE_HOUR)
                .congestionLevel(CongestionLevel.NORMAL).congestionScore(0.5).predictedAt(now).build());
        // 덕수궁: 예전 예측(혼잡) + 최신 예측(여유) → 최신값인 여유가 나와야 함
        em.persist(Congestion.builder().station(deoksugungStation).targetTime(ONE_HOUR)
                .congestionLevel(CongestionLevel.CONGESTED).congestionScore(0.9).predictedAt(now.minusHours(1)).build());
        em.persist(Congestion.builder().station(deoksugungStation).targetTime(ONE_HOUR)
                .congestionLevel(CongestionLevel.SPACIOUS).congestionScore(0.2).predictedAt(now).build());
        // 을지로: 충전기가 전부 고장이라 혼잡도 행이 있어도 null이 나와야 함
        em.persist(Congestion.builder().station(euljiroStation).targetTime(ONE_HOUR)
                .congestionLevel(CongestionLevel.CONGESTED).congestionScore(0.8).predictedAt(now).build());

        // persist는 insert를 미뤄두므로 flush로 DB에 바로 반영 (네이티브 쿼리가 읽을 수 있도록)
        em.flush();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== findNearbyStationsWithFilter ==========

    @Test
    void 반경_내_충전소만_거리순으로_조회한다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // 각 결과에서 statId 뽑기(간단하게)
                .containsExactly(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID, GWANGHWAMUN_STAT_ID, EULJIRO_STAT_ID); // 뽑는 값이 정확히 거리순인지 확인
    }

    @Test
    void 커서가_있으면_커서_거리보다_먼_충전소만_조회한다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        Double cursorDistance = 300.0; // 덕수궁(270m), 서울시청(0m) 제외

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // 각 결과에서 statId 뽑기(간단하게)
                .containsExactly(CITY_HALL_EXIT_STAT_ID, GWANGHWAMUN_STAT_ID, EULJIRO_STAT_ID);
    }

    @Test
    void 빈_필터면_조건_없이_전체_조회한다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // 각 결과에서 statId 뽑기(간단하게)
                .containsExactly(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID, GWANGHWAMUN_STAT_ID, EULJIRO_STAT_ID);
    }

    @Test
    void 필터의_리스트가_빈_리스트면_조건을_적용하지_않는다() {
        // given
        StationFilter  filter = new StationFilter(null, null, null, null, null, List.of(), List.of(), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // 각 결과에서 statId 뽑기(간단하게)
                .containsExactly(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID, GWANGHWAMUN_STAT_ID, EULJIRO_STAT_ID);
    }

    @Test
    void parkingFree가_true면_무료주차_충전소만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(null, true, null, null, null, List.of(), List.of(), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // 각 결과에서 statId 뽑기(간단하게)
                .containsExactly(CITY_HALL_STAT_ID, GWANGHWAMUN_STAT_ID, EULJIRO_STAT_ID);
    }

    @Test
    void limitYn이_true면_개방_충전소만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(null, null, true, null, null, List.of(), List.of(), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // 각 결과에서 statId 뽑기(간단하게)
                .containsExactly(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID, EULJIRO_STAT_ID);
    }

    @Test
    void 출력_범위_필터를_주면_범위_안의_충전기만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(null, null, null, 50, 100, List.of(), List.of(), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId, StationResponse::totalCount) // id, 충전기 수
                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, 1), // 100만 포함 (7은 제외)
                        tuple(EULJIRO_STAT_ID, 2) // 50, 100 둘 다 포함 (경계값)
                );
    }

    @Test
    void chgerTypes_필터를_주면_해당_충전기_타입만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(null, null, null, null, null, List.of("02", "07"), List.of(), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId, StationResponse::totalCount) // id, 충전기 수
                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, 1),
                        tuple(CITY_HALL_EXIT_STAT_ID, 1),
                        tuple(GWANGHWAMUN_STAT_ID, 2)
                );
    }

    @Test
    void kinds_필터를_주면_해당_시설_충전소만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(null, null, null, null, null, List.of(), List.of("A0"), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // id
                .containsExactly(CITY_HALL_STAT_ID, EULJIRO_STAT_ID);
    }

    @Test
    void floorTypes_필터를_주면_해당_층_충전소만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(null, null, null, null, null, List.of(), List.of(), List.of("B"));
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // id
                .containsExactly(DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID);
    }

    @Test
    void availableOnly가_true면_사용_가능한_충전기가_있는_충전소만_조회한다() {
        // given
        StationFilter  filter = new StationFilter(true, null, null, null, null, List.of(), List.of(), List.of());
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, filter);

        Double cursorDistance = null;

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        // then
        assertThat(stationResponses) // 검사 대상: 조회 결과 리스트
                .extracting(StationResponse::statId) // id
                .containsExactly(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID);
    }

    @Test
    void 충전기_상태별_개수와_여부를_집계한다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses)
                .extracting(
                        StationResponse::statId,
                        StationResponse::totalCount,
                        StationResponse::availableCount, // 충전 가능 개수
                        StationResponse::hasCharging, // 충전 중 개수
                        StationResponse::allUnknown, // 전부 알 수 없는가?
                        StationResponse::allUnavailable // 전부 사용 불가인가?
                )

                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, 2, 1, true, false, false),
                        tuple(DEOKSUGUNG_STAT_ID, 2, 2, false, false, false),
                        tuple(CITY_HALL_EXIT_STAT_ID, 1, 0, false, true, false),
                        tuple(GWANGHWAMUN_STAT_ID, 2, 0, true, false, false),
                        tuple(EULJIRO_STAT_ID, 2, 0, false, false, true)
                );
    }

    @Test
    void 급속_충전기가_있으면_hasFast가_true다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses)
                .extracting(
                        StationResponse::statId,
                        StationResponse::totalCount,
                        StationResponse::hasFast // 1개 이상이 급속
                )

                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, 2, true),
                        tuple(DEOKSUGUNG_STAT_ID, 2, true),
                        tuple(CITY_HALL_EXIT_STAT_ID, 1, false),
                        tuple(GWANGHWAMUN_STAT_ID, 2, false),
                        tuple(EULJIRO_STAT_ID, 2, true)
                );
    }

    @Test
    void 충전기와_리뷰가_여러_개여도_개수가_중복_집계되지_않는다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses)
                .filteredOn(s -> s.statId().equals(CITY_HALL_STAT_ID) || s.statId().equals(GWANGHWAMUN_STAT_ID))
                .extracting(
                        StationResponse::statId,
                        StationResponse::totalCount,
                        StationResponse::availableCount, // 충전 가능
                        StationResponse::reviewCount, // 리뷰 개수
                        StationResponse::averageRating // 평점
                )

                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, 2, 1, 2, 4.5), // 충전기 2, 리뷰 2
                        tuple(GWANGHWAMUN_STAT_ID, 2, 0, 1, 3.0) // 충전기 2, 리뷰 1
                );
    }

    @Test
    void 리뷰가_없으면_평균_별점은_null이고_리뷰_개수는_0이다() {
        // given
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses)
                // 리뷰 없는 충전소만(덕수궁, 서울시청, 을지로)
                .filteredOn(s ->
                        s.statId().equals(DEOKSUGUNG_STAT_ID)
                                || s.statId().equals(CITY_HALL_EXIT_STAT_ID)
                                || s.statId().equals(EULJIRO_STAT_ID))
                .extracting(
                        StationResponse::statId,
                        StationResponse::averageRating, // 평점: null
                        StationResponse::reviewCount // 리뷰 개수: 0
                )

                .containsExactly(
                        tuple(DEOKSUGUNG_STAT_ID, null, 0),
                        tuple(CITY_HALL_EXIT_STAT_ID, null, 0),
                        tuple(EULJIRO_STAT_ID, null, 0)
                );
    }

    @Test
    void 혼잡도는_가장_최근에_예측한_1시간_뒤_값을_반환한다() {
        // given
        // 서울시청에 2시간 뒤 예측(targetTime=2)을 가장 최근 시각으로 추가
        // → 1시간 뒤 값만 골라야 하므로 무시돼야 함
        em.persist(Congestion.builder().station(cityHallStation).targetTime(2)
                .congestionLevel(CongestionLevel.CONGESTED).congestionScore(0.9)
                .predictedAt(LocalDateTime.now().plusMinutes(10)).build());
        em.flush();

        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses)
                .filteredOn(s ->
                        s.statId().equals(CITY_HALL_STAT_ID) // 1시간, 2시간 뒤 혼잡도 값
                                || s.statId().equals(DEOKSUGUNG_STAT_ID) // 1시간 뒤 혼잡도 값 2번 입력
                )
                .extracting(
                        StationResponse::statId,
                        StationResponse::nextHourCongestionLevel // 1시간 뒤 혼잡도
                )

                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, CongestionLevel.NORMAL), // 1시간 뒤 예측(보통)
                        tuple(DEOKSUGUNG_STAT_ID, CongestionLevel.SPACIOUS) // 최신 예측(여유)
                );
    }

    @Test
    void 대기_충전중_예약중인_충전기가_없으면_혼잡도는_null이다() {
        // given
        // 을지로: 충전기가 점검중, 운영중지뿐 (대기 02, 충전중 03, 예약중 06 없음)
        // 혼잡도 행(혼잡)은 setUp에서 넣어둠 → 행이 있어도 null이 나와야 함
        NearbyStationRequest request = new NearbyStationRequest(USER_LAT, USER_LNG, RANGE, null, StationFilter.empty());

        // when
        List<StationResponse> stationResponses = stationRepository.findNearbyStationsWithFilter(request, null);

        // then
        assertThat(stationResponses)
                .filteredOn(s ->
                        s.statId().equals(CITY_HALL_STAT_ID) // 비교용: 대기 충전기 있음 → 혼잡도 값 있음
                                || s.statId().equals(EULJIRO_STAT_ID) // 전부 고장 → 혼잡도 행이 있어도 null
                )
                .extracting(
                        StationResponse::statId,
                        StationResponse::nextHourCongestionLevel // 1시간 뒤 혼잡도
                )

                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, CongestionLevel.NORMAL),
                        tuple(EULJIRO_STAT_ID, null)
                );
    }

    // ========== findStationsInBoundsWithFilter ==========

    @Test
    void 지도_영역_안의_충전소만_조회한다() {
        // 위도 37.5650~37.5710, 경도 126.9740~126.9850
        // 포함: 서울시청, 덕수궁, 시청역
        // 제외: 광화문(위도 37.5759, 북쪽 밖), 을지로(경도 126.9910, 동쪽 밖), 강남역
        MapBoundsRequest request = new MapBoundsRequest(
                37.5650, 37.5710, // minLat, maxLat
                126.9740, 126.9850, // minLng, maxLng
                USER_LAT, USER_LNG, // 유저 위치 (서울시청)
                StationFilter.empty());

        // when
        List<StationResponse> responses = stationRepository.findStationsInBoundsWithFilter(request, MAX_COUNT);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId) // statId
                .containsExactly(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID); // 영역 안 3개만, 거리순
    }

    @Test
    void 거리는_유저_위치_기준으로_계산한다() {
        // 유저를 을지로 충전소 위치로 이동 (영역 밖이어도 상관없음)
        // 을지로 기준 거리: 시청역 ~1.07km < 서울시청 ~1.15km < 덕수궁 ~1.40km
        MapBoundsRequest request = new MapBoundsRequest(
                37.5650, 37.5710,
                126.9740, 126.9850,
                EULJIRO_LAT, EULJIRO_LNG,
                StationFilter.empty());

        // when
        List<StationResponse> responses = stationRepository.findStationsInBoundsWithFilter(request, MAX_COUNT);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId)
                // 을지로 기준 거리순: 시청역(~1.07km) → 서울시청(~1.15km) → 덕수궁(~1.40km)
                // 위 테스트와 순서가 달라야 함
                .containsExactly(CITY_HALL_EXIT_STAT_ID, CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID);
    }

    @Test
    void 지도_영역_조회에도_필터가_적용된다() {
        // 같은 영역 + 지하(B)만 → 서울시청(지상) 제외
        StationFilter filter = new StationFilter(null, null, null, null, null, List.of(), List.of(), List.of("B"));
        MapBoundsRequest request = new MapBoundsRequest(
                37.5650, 37.5710,
                126.9740, 126.9850,
                USER_LAT, USER_LNG,
                filter);

        // when
        List<StationResponse> responses = stationRepository.findStationsInBoundsWithFilter(request, MAX_COUNT);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId)
                .containsExactly(DEOKSUGUNG_STAT_ID, CITY_HALL_EXIT_STAT_ID); // 영역 안 3개 중 지하(B)만, 서울시청(지상) 제외
    }

    @Test
    void 지도_영역_조회는_화면_중심에서_가까운_곳만_상한까지_남기고_유저_거리순으로_정렬한다() {
        // 화면 중심이 광화문 근처 (위도 37.5750, 경도 126.9775)
        // 유저(서울시청)와 가장 가까운 곳이 아니라 화면 중심과 가장 가까운 광화문이 남아야 함
        MapBoundsRequest request = new MapBoundsRequest(
                37.5650, 37.5850,
                126.9700, 126.9850,
                USER_LAT, USER_LNG,
                StationFilter.empty());

        // when: 상한 1곳
        List<StationResponse> responses = stationRepository.findStationsInBoundsWithFilter(request, 1);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId)
                .containsExactly(GWANGHWAMUN_STAT_ID);
    }

    @Test
    void 지도_영역_조회는_영역_경계_바로_밖_충전소를_넣지_않는다() {
        // 을지로(경도 126.9910)는 동쪽 경계(126.9850)에서 약 500m 밖
        // 위치 인덱스용 && 조건(곡면 기준)은 이 정도 크기의 영역에서 을지로까지 후보로 잡아서, ST_Within으로 다시 걸러야 함
        MapBoundsRequest request = new MapBoundsRequest(
                37.5400, 37.5900,
                126.9400, 126.9850,
                USER_LAT, USER_LNG,
                StationFilter.empty());

        // when
        List<StationResponse> responses = stationRepository.findStationsInBoundsWithFilter(request, MAX_COUNT);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId)
                .containsExactlyInAnyOrder(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID, GWANGHWAMUN_STAT_ID, CITY_HALL_EXIT_STAT_ID)
                .doesNotContain(EULJIRO_STAT_ID);
    }

    @Test
    void 지도_영역_조회는_상한_안에서_유저_거리순으로_정렬한다() {
        // 영역 안 4곳(서울시청, 덕수궁, 시청역, 광화문) 중 화면 중심(위도 37.5710, 경도 126.9795)에서 가장 먼 덕수궁이 빠짐
        MapBoundsRequest request = new MapBoundsRequest(
                37.5650, 37.5770,
                126.9740, 126.9850,
                USER_LAT, USER_LNG,
                StationFilter.empty());

        // when
        List<StationResponse> limited = stationRepository.findStationsInBoundsWithFilter(request, 3);
        List<StationResponse> all = stationRepository.findStationsInBoundsWithFilter(request, MAX_COUNT);

        // then: 남은 3곳은 유저(서울시청) 거리순
        assertThat(all).hasSize(4);
        assertThat(limited)
                .extracting(StationResponse::statId)
                .containsExactly(CITY_HALL_STAT_ID, CITY_HALL_EXIT_STAT_ID, GWANGHWAMUN_STAT_ID);
    }

    // ========== findRegionSummaries ==========

    @Test
    void 시도별_충전소_수와_이용_가능_충전소_수를_집계한다() {
        // 샘플 충전소 6곳 모두 서울(11)
        // 충전대기 충전기가 있는 곳: 서울시청, 덕수궁, 강남역 -> 3곳
        // 충전기가 여러 대여도 충전소는 한 번만 셈

        // when
        List<RegionSummaryResponse> regions = stationRepository.findRegionSummaries();

        // then
        assertThat(regions).hasSize(1);
        RegionSummaryResponse seoul = regions.get(0);
        assertThat(seoul.code()).isEqualTo(ZCODE);
        assertThat(seoul.name()).isEqualTo("서울");
        assertThat(seoul.stationCount()).isEqualTo(6);
        assertThat(seoul.availableStationCount()).isEqualTo(3);
        // 표시 위치는 샘플 충전소들 사이 (중앙값)
        assertThat(seoul.lat()).isBetween(37.49, 37.58);
        assertThat(seoul.lng()).isBetween(126.97, 127.03);
    }

    // ========== findCitySummaries ==========

    @Test
    void 시군_요약은_도_안의_시를_나누고_광역시와_시의_구는_나누지_않는다() {
        // given: 서울 샘플 6곳 + 경기 수원시 2개 구 + 전남광주의 광주 북구, 순천시
        // 주소 표기가 섞여 있어도("경기도"/"경기") 코드로 묶임
        persistStationWithCharger("TS100001", "경기도 수원시 장안구 정자로 1", "41", "41111", 37.30, 127.01, ChgerStat.WAITING);
        persistStationWithCharger("TS100002", "경기 수원시 권선구 권선로 1", "41", "41113", 37.26, 127.03, ChgerStat.CHARGING);
        persistStationWithCharger("TS100003", "전남광주통합특별시 북구 용봉로 1", "12", "12300", 35.17, 126.91, ChgerStat.WAITING);
        persistStationWithCharger("TS100004", "전남광주통합특별시 순천시 중앙로 1", "12", "12150", 34.95, 127.49, ChgerStat.WAITING);
        em.flush();

        // when
        List<RegionSummaryResponse> regions = stationRepository.findCitySummaries();

        // then
        assertThat(regions)
                .extracting(RegionSummaryResponse::code, RegionSummaryResponse::name,
                        RegionSummaryResponse::stationCount, RegionSummaryResponse::availableStationCount)
                .containsExactlyInAnyOrder(
                        tuple(ZCODE, "서울", 6L, 3L), // 광역시는 구로 나누지 않음
                        tuple("4111", "수원시", 2L, 1L), // 장안구·권선구가 수원시 하나로
                        tuple("12-gwangju", "광주", 1L, 1L), // 전남광주 안의 광주 구는 "광주"
                        tuple("1215", "순천시", 1L, 1L));
    }

    private void persistStationWithCharger(String statId, String addr, String zcode, String zscode,
                                           double lat, double lng, ChgerStat stat) {
        Station station = em.persist(Station.builder()
                .statId(statId)
                .statNm(statId)
                .addr(addr)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat)))
                .useTime(USE_TIME)
                .stationOperator(operator)
                .zcode(zcode)
                .zscode(zscode)
                .kind(Kind.PUBLIC)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build());
        em.persist(Charger.builder().station(station).chgerId(CHGER_ID)
                .chgerType(ChgerType.DC_COMBO).chgerStat(stat)
                .statUpdDt(STAT_UPD_DT).output(FAST_OUTPUT).build());
    }

    @Test
    void 지도_영역_조회도_충전기_리뷰_혼잡도를_집계한다() {
        // given
        // select 절이 findNearbyStationsWithFilter와 따로 복사되어 있어서 별도로 검증
        // 위도 37.5650~37.5710, 경도 126.9740~126.9950 → 을지로까지 포함 (광화문, 강남역 제외)
        MapBoundsRequest request = new MapBoundsRequest(
                37.5650, 37.5710,
                126.9740, 126.9950,
                USER_LAT, USER_LNG,
                StationFilter.empty());

        // when
        List<StationResponse> responses = stationRepository.findStationsInBoundsWithFilter(request, MAX_COUNT);

        // then
        assertThat(responses)
                .extracting(
                        StationResponse::statId,
                        StationResponse::totalCount,
                        StationResponse::availableCount,
                        StationResponse::hasCharging,
                        StationResponse::allUnknown,
                        StationResponse::allUnavailable,
                        StationResponse::hasFast,
                        StationResponse::reviewCount,
                        StationResponse::averageRating,
                        StationResponse::nextHourCongestionLevel
                )

                .containsExactly(
                        tuple(CITY_HALL_STAT_ID, 2, 1, true, false, false, true, 2, 4.5, CongestionLevel.NORMAL), // 충전기 2 x 리뷰 2 중복 없이
                        tuple(DEOKSUGUNG_STAT_ID, 2, 2, false, false, false, true, 0, null, CongestionLevel.SPACIOUS), // 최신 예측값
                        tuple(CITY_HALL_EXIT_STAT_ID, 1, 0, false, true, false, false, 0, null, null), // 혼잡도 행 없음
                        tuple(EULJIRO_STAT_ID, 2, 0, false, false, true, true, 0, null, null) // 전부 고장 → 혼잡도 행이 있어도 null
                );
    }

    // ========== findFavoriteStations ==========

    @Test
    void 해당_유저의_즐겨찾기_충전소만_조회한다() {
        // given
        em.persist(Favorite.builder().user(user).station(cityHallStation).build());
        em.persist(Favorite.builder().user(user).station(deoksugungStation).build());
        em.persist(Favorite.builder().user(secondUser).station(gwanghwamunStation).build());
        em.flush();

        // when
        List<StationResponse> responses = stationRepository.findFavoriteStations(user.getUserId(), USER_LAT, USER_LNG);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId)
                // user의 즐겨찾기(서울시청, 덕수궁)만, secondUser의 광화문은 제외
                .containsExactlyInAnyOrder(CITY_HALL_STAT_ID, DEOKSUGUNG_STAT_ID);
    }

    @Test
    void 즐겨찾기_충전소는_최근에_등록한_순으로_조회한다() {
        // given
        Favorite oldest = em.persist(Favorite.builder().user(user).station(cityHallStation).build());
        Favorite middle = em.persist(Favorite.builder().user(user).station(gwanghwamunStation).build());
        Favorite newest = em.persist(Favorite.builder().user(user).station(deoksugungStation).build());
        em.flush();

        // created_at의 등록 순서의 차이를 크게 하기 위해 SQL 직접 수정
        // 등록 최신순: 덕수궁 -> 광화문 -> 서울시청
        LocalDateTime now = LocalDateTime.now();
        em.getEntityManager().createNativeQuery("update favorite set created_at = ? where id = ?")
                .setParameter(1, now.minusDays(2)).setParameter(2, oldest.getId()).executeUpdate(); // 2일 전
        em.getEntityManager().createNativeQuery("update favorite set created_at = ? where id = ?")
                .setParameter(1, now.minusDays(1)).setParameter(2, middle.getId()).executeUpdate(); // 1일 전
        em.getEntityManager().createNativeQuery("update favorite set created_at = ? where id = ?")
                .setParameter(1, now).setParameter(2, newest.getId()).executeUpdate(); // 지금
        em.flush();

        // when
        List<StationResponse> responses = stationRepository.findFavoriteStations(user.getUserId(), USER_LAT, USER_LNG);

        // then
        assertThat(responses)
                .extracting(StationResponse::statId)
                .containsExactly(DEOKSUGUNG_STAT_ID, GWANGHWAMUN_STAT_ID, CITY_HALL_STAT_ID);
    }

    @Test
    void 즐겨찾기가_없으면_빈_리스트를_반환한다() {
        // given
        // 다른 유저의 즐겨찾기만 존재 (user는 즐겨찾기 없음)
        em.persist(Favorite.builder().user(secondUser).station(cityHallStation).build());
        em.flush();

        // when
        List<StationResponse> responses = stationRepository.findFavoriteStations(user.getUserId(), USER_LAT, USER_LNG);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    void 즐겨찾기_조회도_충전기_리뷰_혼잡도를_집계한다() {
        // given
        // select 절이 findNearbyStationsWithFilter와 따로 복사되어 있고 favorite 조인이 추가되어 있어서 별도로 검증
        em.persist(Favorite.builder().user(user).station(cityHallStation).build());
        em.persist(Favorite.builder().user(user).station(deoksugungStation).build());
        em.persist(Favorite.builder().user(user).station(euljiroStation).build());
        // 다른 유저도 서울시청을 즐겨찾기 → 개수가 중복 집계되면 안 됨
        em.persist(Favorite.builder().user(secondUser).station(cityHallStation).build());
        em.flush();

        // when
        List<StationResponse> responses = stationRepository.findFavoriteStations(user.getUserId(), USER_LAT, USER_LNG);

        // then
        assertThat(responses)
                .extracting(
                        StationResponse::statId,
                        StationResponse::totalCount,
                        StationResponse::availableCount,
                        StationResponse::hasCharging,
                        StationResponse::allUnavailable,
                        StationResponse::hasFast,
                        StationResponse::reviewCount,
                        StationResponse::averageRating,
                        StationResponse::nextHourCongestionLevel)

                // 등록 시각이 거의 같아 순서는 보장되지 않음 (순서는 등록순 테스트에서 검증)
                .containsExactlyInAnyOrder(
                        tuple(CITY_HALL_STAT_ID, 2, 1, true, false, true, 2, 4.5, CongestionLevel.NORMAL), // 충전기 2 x 리뷰 2 x 즐겨찾기 2 중복 없이
                        tuple(DEOKSUGUNG_STAT_ID, 2, 2, false, false, true, 0, null, CongestionLevel.SPACIOUS), // 최신 예측값
                        tuple(EULJIRO_STAT_ID, 2, 0, false, true, true, 0, null, null) // 전부 고장 → 혼잡도 행이 있어도 null
                );
    }
}
