package ev_charger.be.station;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.charger_alert.ChargerAlertRepository;
import ev_charger.be.common.CursorUtils;
import ev_charger.be.common.enums.YN;
import ev_charger.be.favorite.FavoriteRepository;
import ev_charger.be.review.ReviewService;
import ev_charger.be.review.dto.response.StationReviewResponse;
import ev_charger.be.review.dto.response.StationReviewsSummary;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.station.charger.ChargerRepository;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.congestion.Congestion;
import ev_charger.be.station.congestion.CongestionLevel;
import ev_charger.be.station.congestion.CongestionRepository;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.NearbyStationPageResponse;
import ev_charger.be.station.dto.response.StationDetailResponse;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StationServiceTest {

    @Mock
    private CursorUtils cursorUtils;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private ChargerRepository chargerRepository;
    @Mock
    private ChargerAlertRepository chargerAlertRepository;
    @Mock
    private ReviewService reviewService;
    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private CongestionRepository congestionRepository;

    private StationService stationService;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        stationService = new StationService(
                cursorUtils,
                stationRepository,
                chargerRepository,
                chargerAlertRepository,
                reviewService,
                favoriteRepository,
                congestionRepository
        );
    }

    @Test
    void 커서_없이_조회하면_첫_페이지를_반환() {
        // given
        StationFilter filter = new StationFilter(
                false, // availableOnly
                false, // parkingFree
                false, // limitYn
                null,  // minOutput
                null,  // maxOutput
                List.of(), // chgerTypes
                List.of(), // kinds
                List.of()  // floorTypes
        );

        NearbyStationRequest request = new NearbyStationRequest(
                37.5665, // lat
                126.9780, // lng
                1000,    // range (m)
                null,    // cursor (첫 페이지)
                filter
        );

        List<StationResponse> stations = List.of(
                new StationResponse(
                        "ST12345", // statId
                        "테스트충전소", // statNm
                        "서울시 강남구 테헤란로 123", // addr
                        37.5665, // lat
                        126.9780, // lng
                        "24시간", // useTime
                        true,  // parkingFree
                        true,  // openToPublic
                        "상업시설", // kind
                        "지상", // floorType
                        true,  // hasFast
                        "테스트운영사", // busiNm
                        4,     // totalCount
                        2,     // availableCount
                        false, // hasCharging
                        false, // allUnknown
                        false, // allUnavailable
                        4.5,   // averageRating
                        10,    // reviewCount
                        350.0, // distance
                        CongestionLevel.SPACIOUS // nextHourCongestionLevel
                )
        );

        given(stationRepository.findNearbyStationsWithFilter(request, null)).willReturn(stations);

        given(cursorUtils.encode(request.range())).willReturn("encoded-cursor");

        // when
        NearbyStationPageResponse response = stationService.getNearbyStations(request);

        // then
        assertThat(response.stations()).isEqualTo(stations);
        assertThat(response.nextCursor()).isEqualTo("encoded-cursor");
    }

    @Test
    void 커서가_있으면_디코딩한_거리_이후_데이터를_조회() {
        // given
        StationFilter filter = new StationFilter(
                false, // availableOnly
                false, // parkingFree
                false, // limitYn
                null,  // minOutput
                null,  // maxOutput
                List.of(), // chgerTypes
                List.of(), // kinds
                List.of()  // floorTypes
        );

        String cursor = "encoded-cursor"; // 요청에 들어온(이전) 커서
        String nextCursor = "encoded-next-cursor"; // 이번 응답이 새로 만들어줄 커서
        Double cursorDistance = 350.0;

        NearbyStationRequest request = new NearbyStationRequest(
                37.5665, // lat
                126.9780, // lng
                1000,    // range (m)
                cursor,    // cursor (첫 페이지)
                filter
        );

        List<StationResponse> stations = List.of(
                new StationResponse(
                        "ST12345", // statId
                        "테스트충전소", // statNm
                        "서울시 강남구 테헤란로 123", // addr
                        37.5665, // lat
                        126.9780, // lng
                        "24시간", // useTime
                        true,  // parkingFree
                        true,  // openToPublic
                        "상업시설", // kind
                        "지상", // floorType
                        true,  // hasFast
                        "테스트운영사", // busiNm
                        4,     // totalCount
                        2,     // availableCount
                        false, // hasCharging
                        false, // allUnknown
                        false, // allUnavailable
                        4.5,   // averageRating
                        10,    // reviewCount
                        350.0, // distance
                        CongestionLevel.SPACIOUS // nextHourCongestionLevel
                )
        );

        given(cursorUtils.decode(request.cursor())).willReturn(cursorDistance);
        given(stationRepository.findNearbyStationsWithFilter(request, cursorDistance)).willReturn(stations);
        given(cursorUtils.encode(request.range())).willReturn(nextCursor);

        // when
        NearbyStationPageResponse response = stationService.getNearbyStations(request);

        // then
        assertThat(response.stations()).isEqualTo(stations);
        assertThat(response.nextCursor()).isEqualTo(nextCursor);
    }

    @Test
    void 지도_범위_내_충전소를_조회() {
        // given
        StationFilter filter = new StationFilter(
                false, // availableOnly
                false, // parkingFree
                false, // limitYn
                null,  // minOutput
                null,  // maxOutput
                List.of(), // chgerTypes
                List.of(), // kinds
                List.of()  // floorTypes
        );

        MapBoundsRequest request = new MapBoundsRequest(
                37.560, // minLat
                37.570, // maxLat
                126.970, // minLng
                126.985, // maxLng
                37.5665, // userLat
                126.9780, // userLng
                filter
        );
        List<StationResponse> stations = List.of(
                new StationResponse(
                        "ST12345", // statId
                        "테스트충전소", // statNm
                        "서울시 강남구 테헤란로 123", // addr
                        37.5665, // lat
                        126.9780, // lng
                        "24시간", // useTime
                        true,  // parkingFree
                        true,  // openToPublic
                        "상업시설", // kind
                        "지상", // floorType
                        true,  // hasFast
                        "테스트운영사", // busiNm
                        4,     // totalCount
                        2,     // availableCount
                        false, // hasCharging
                        false, // allUnknown
                        false, // allUnavailable
                        4.5,   // averageRating
                        10,    // reviewCount
                        350.0, // distance
                        CongestionLevel.SPACIOUS // nextHourCongestionLevel
                )
        );

        given(stationRepository.findStationsInBoundsWithFilter(request)).willReturn(stations);

        // when
       List<StationResponse> response =  stationService.getStationsInBounds(request);

        // then
        assertThat(response).isEqualTo(stations);
    }

    @Test
    void 존재하지_않는_충전소면_예외_발생() {
        // given
        String statId = "ST12345";

        // when & then
        assertThatThrownBy(() -> stationService.getStationDetail(null, statId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 충전소입니다.");
    }

    @Test
    void 로그인_유저면_알림_설정된_충전기Id가_반영() {
        // given
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING);
        given(charger.getOutput()).willReturn("100kW");

        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        List<Charger> chargers = List.of(charger);
        given(chargerRepository.findByStatId(statId)).willReturn(chargers);
        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665)); // lng, lat 순서

        Station station = Station.builder()
                .statId("ST12345")
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .addrDetail(null)
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .zscode(null)
                .kind(Kind.COMMERCIAL) // "주유소"는 실제 Kind enum에 없어서 상업시설로 대체
                .kindDetail(null)
                .parkingFree(YN.Y)
                .note(null)
                .limitYn(YN.N) // openToPublic=true → limitYn=N
                .limitDetail(null)
                .floorNum(null)
                .floorType(FloorType.F) // "지상"
                .build();

        List<StationReviewResponse> reviews = List.of(
                new StationReviewResponse(
                        1L,    // reviewId
                        "리뷰어", // nickname
                        null,  // profileImageUrl
                        5,     // rating
                        "좋아요", // content
                        List.of(), // imageUrls
                        LocalDateTime.now(), // createdAt
                        false, // isMyReview
                        false  // isEdited
                )
        );
        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.ofNullable(station));
        given(chargerAlertRepository.findByUserAndCharger_StatId(user, statId)).willReturn(List.of(alert));
        given(reviewService.getReviewsByStation(user, statId)).willReturn(reviews);
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);

        Congestion congestion1 = mock(Congestion.class);
        given(congestion1.getTargetTime()).willReturn(1);
        given(congestion1.getCongestionLevel()).willReturn(CongestionLevel.SPACIOUS);
        given(congestion1.getCongestionScore()).willReturn(0.85);

        given(congestionRepository.findByStationOrderByPredictedAtDesc(station))
                .willReturn(List.of(congestion1));

        given(favoriteRepository.existsByUserAndStation(user, station)).willReturn(true);

        // when
        StationDetailResponse response = stationService.getStationDetail(user, statId);

        // then
        assertThat(response.hasFast()).isTrue(); // chgerType 판별 로직
        assertThat(response.chargers().get(0).isAlert()).isTrue(); // alertedIds 매칭 로직
        assertThat(response.isFavorite()).isTrue(); // 로그인 분기
        assertThat(response.congestions().oneHour()).isEqualTo(CongestionLevel.SPACIOUS); // congestion 매핑 로직
        assertThat(response.congestions().twoHour()).isNull();
        assertThat(response.congestions().threeHour()).isNull();
    }

    @Test
    void 비로그인_유저면_alertedIds가_빈리스트() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING);
        given(charger.getOutput()).willReturn("100kW");

        List<Charger> chargers = List.of(charger);
        given(chargerRepository.findByStatId(statId)).willReturn(chargers);
        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665)); // lng, lat 순서

        Station station = Station.builder()
                .statId("ST12345")
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .addrDetail(null)
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .zscode(null)
                .kind(Kind.COMMERCIAL) // "주유소"는 실제 Kind enum에 없어서 상업시설로 대체
                .kindDetail(null)
                .parkingFree(YN.Y)
                .note(null)
                .limitYn(YN.N) // openToPublic=true → limitYn=N
                .limitDetail(null)
                .floorNum(null)
                .floorType(FloorType.F) // "지상"
                .build();

        List<StationReviewResponse> reviews = List.of(
                new StationReviewResponse(
                        1L,    // reviewId
                        "리뷰어", // nickname
                        null,  // profileImageUrl
                        5,     // rating
                        "좋아요", // content
                        List.of(), // imageUrls
                        LocalDateTime.now(), // createdAt
                        false, // isMyReview
                        false  // isEdited
                )
        );
        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.ofNullable(station));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);

        Congestion congestion1 = mock(Congestion.class);
        given(congestion1.getTargetTime()).willReturn(1);
        given(congestion1.getCongestionLevel()).willReturn(CongestionLevel.SPACIOUS);
        given(congestion1.getCongestionScore()).willReturn(0.85);

        given(congestionRepository.findByStationOrderByPredictedAtDesc(station))
                .willReturn(List.of(congestion1));

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.hasFast()).isTrue(); // chgerType 판별 로직
        assertThat(response.chargers().get(0).isAlert()).isFalse(); // alertedIds 매칭 로직
        assertThat(response.isFavorite()).isNull(); // 비로그인이면 favoriteRepository 호출 없이 null
        assertThat(response.congestions().oneHour()).isEqualTo(CongestionLevel.SPACIOUS); // congestion 매핑 로직
        assertThat(response.congestions().twoHour()).isNull();
        assertThat(response.congestions().threeHour()).isNull();
    }

    @Test
    void 급속충전기가_있으면_hasFast가_true() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO); // 급속
        given(charger.getChgerStat()).willReturn(ChgerStat.SUSPENDED); // 이용불가 -> congestion 조회 스킵
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.hasFast()).isTrue();
    }

    @Test
    void 완속충전기만_있으면_hasFast가_false() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.AC_SLOW); // 완속
        given(charger.getChgerStat()).willReturn(ChgerStat.SUSPENDED); // 이용불가 -> congestion 조회 스킵
        given(charger.getOutput()).willReturn("7kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.hasFast()).isFalse();
    }

    @Test
    void 이용가능한_충전기가_없으면_congestionDetail이_전부_null() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.SUSPENDED); // WAITING/CHARGING/RESERVED 아님 -> 이용가능한 충전기 없음
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.congestions())
                .isEqualTo(new StationDetailResponse.CongestionDetail(null, null, null, null));
        verify(congestionRepository, never()).findByStationOrderByPredictedAtDesc(any());
    }

    @Test
    void 이용가능한_충전기가_있으면_congestion_데이터를_조회해서_반환() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING); // 이용가능한 충전기 있음
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        Congestion congestion1 = mock(Congestion.class);
        given(congestion1.getTargetTime()).willReturn(1);
        given(congestion1.getCongestionLevel()).willReturn(CongestionLevel.SPACIOUS);
        given(congestion1.getCongestionScore()).willReturn(0.85);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);
        given(congestionRepository.findByStationOrderByPredictedAtDesc(station))
                .willReturn(List.of(congestion1));

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.congestions().accuracy()).isEqualTo(0.85);
        assertThat(response.congestions().oneHour()).isEqualTo(CongestionLevel.SPACIOUS);
    }

    @Test
    void congestionLevel이_null인_데이터는_제외하고_매핑() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING);
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        // targetTime=2, level=NORMAL인 정상 데이터
        Congestion normalCongestion = mock(Congestion.class);
        given(normalCongestion.getTargetTime()).willReturn(2);
        given(normalCongestion.getCongestionLevel()).willReturn(CongestionLevel.NORMAL);

        // level이 null이라 필터에서 걸러짐 -> getTargetTime()까지는 호출되지 않음
        Congestion nullLevelCongestion = mock(Congestion.class);
        given(nullLevelCongestion.getCongestionLevel()).willReturn(null);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);
        given(congestionRepository.findByStationOrderByPredictedAtDesc(station))
                .willReturn(List.of(normalCongestion, nullLevelCongestion));

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.congestions().oneHour()).isNull(); // level null -> 매핑에서 제외
        assertThat(response.congestions().twoHour()).isEqualTo(CongestionLevel.NORMAL);
    }

    @Test
    void 같은_targetTime중_가장_최신_predictedAt_데이터만_반영() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING);
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        // 같은 targetTime=1, predictedAt이 더 최신인 데이터
        Congestion latestCongestion = mock(Congestion.class);
        given(latestCongestion.getTargetTime()).willReturn(1);
        given(latestCongestion.getCongestionLevel()).willReturn(CongestionLevel.NORMAL);

        // 같은 targetTime=1, predictedAt이 더 오래된 데이터
        Congestion olderCongestion = mock(Congestion.class);
        given(olderCongestion.getTargetTime()).willReturn(1);
        given(olderCongestion.getCongestionLevel()).willReturn(CongestionLevel.SPACIOUS);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);
        // predictedAt desc 정렬이므로 최신(latestCongestion)이 먼저 옴
        given(congestionRepository.findByStationOrderByPredictedAtDesc(station))
                .willReturn(List.of(latestCongestion, olderCongestion));

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.congestions().oneHour()).isEqualTo(CongestionLevel.NORMAL); // 최신 값 유지, SPACIOUS 아님
    }

    @Test
    void 로그인_유저면_즐겨찾기_여부가_반영() {
        // given
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.SUSPENDED); // congestion 조회 스킵
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);
        given(favoriteRepository.existsByUserAndStation(user, station)).willReturn(true);

        // when
        StationDetailResponse response = stationService.getStationDetail(user, statId);

        // then
        assertThat(response.isFavorite()).isTrue();
    }

    @Test
    void 비로그인_유저면_즐겨찾기_여부는_null() {
        // given
        String statId = "ST12345";

        Charger charger = mock(Charger.class);
        given(charger.getChgerId()).willReturn("01");
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.SUSPENDED); // congestion 조회 스킵
        given(charger.getOutput()).willReturn("100kW");

        StationOperator operator = StationOperator.builder()
                .busiId("20")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .location(location)
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.COMMERCIAL)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();

        StationReviewsSummary summary = new StationReviewsSummary(4.5, 10);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(chargerRepository.findByStatId(statId)).willReturn(List.of(charger));
        given(reviewService.getStationReviewsSummary(statId)).willReturn(summary);

        // when
        StationDetailResponse response = stationService.getStationDetail(null, statId);

        // then
        assertThat(response.isFavorite()).isNull();
        verify(favoriteRepository, never()).existsByUserAndStation(any(), any());
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과 시간: " + (System.currentTimeMillis()-startTime) + "ms");
    }
}
