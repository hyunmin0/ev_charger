package ev_charger.be.station;

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
import ev_charger.be.station.dto.response.RegionSummaryResponse;
import ev_charger.be.station.dto.response.StationDetailResponse;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.enums.RegionLevel;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ev_charger.be.station.dto.request.StationSearchRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class StationService {

    private final CursorUtils cursorUtils;
    private final StationRepository stationRepository;
    private final ChargerRepository chargerRepository;
    private final ChargerAlertRepository chargerAlertRepository;
    private final ReviewService reviewService;
    private final FavoriteRepository favoriteRepository;
    private final CongestionRepository congestionRepository;

    // 지도 화면 조회 상한. 줌아웃하면 화면 안 충전소가 수만 곳(수 MB)이 돼서 화면 중심에서 가까운 곳만 보냄
    static final int MAX_BOUNDS_STATIONS = 500;

    // 지역 요약은 전국 집계라 1~2초 걸리고 거의 안 바뀜 -> 충전기 상태 수집 주기(5분)만큼 재사용
    static final Duration REGION_CACHE_TTL = Duration.ofMinutes(5);
    private final Map<RegionLevel, CachedRegions> regionCache = new ConcurrentHashMap<>();

    private record CachedRegions(List<RegionSummaryResponse> regions, Instant cachedAt) {}

    /**
     * 가까운 충전소 찾기
     * @param request
     * @return list<충전소id, 충전소이름, 주소, 경도, 위도, 총 충전기 수, 거리, 필터들>, 다음 커서
     */
    public NearbyStationPageResponse getNearbyStations(NearbyStationRequest request) {

        // 커서가 있으면 디코딩해서 마지막 distance 추출
        // 다음 쿼리에서 이 거리 이후의 데이터만 가져오기 위해 필요(null이면 첫 페이지)
        Double cursorDistance = request.cursor() != null ? cursorUtils.decode(request.cursor()) : null;

        // range(반경) 내이면서 cursorDistance 이후의 데이터만 가져옴
        List<StationResponse> stations = stationRepository.findNearbyStationsWithFilter(request, cursorDistance);

        return new NearbyStationPageResponse(stations, cursorUtils.encode(request.range()));
    }

    /**
     * 현 지도 내의 충전소 찾기
     * @param request
     * @return 충전소id, 이름, 주소, 위경도, 운영시간, 총 충전기수, 거리, 필터들
     */
    public List<StationResponse> getStationsInBounds(MapBoundsRequest request) {
        return stationRepository.findStationsInBoundsWithFilter(request, MAX_BOUNDS_STATIONS);
    }
    public List<StationResponse> searchStations(StationSearchRequest request) {
    return stationRepository.searchStationsByKeyword(request);
}

    /**
     * 지역별 충전소 요약 (지도를 줌아웃했을 때)
     * @param level SIDO: 시·도, CITY: 도 안의 시·군 + 광역시
     * @return 지역 키, 이름, 충전소 수, 이용 가능 충전소 수, 표시 위치
     */
    public List<RegionSummaryResponse> getRegionSummaries(RegionLevel level) {
        CachedRegions cached = regionCache.get(level);
        if (cached == null || Instant.now().isAfter(cached.cachedAt().plus(REGION_CACHE_TTL))) {
            List<RegionSummaryResponse> regions = level == RegionLevel.SIDO
                    ? stationRepository.findRegionSummaries()
                    : stationRepository.findCitySummaries();
            cached = new CachedRegions(regions, Instant.now());
            regionCache.put(level, cached);
        }
        return cached.regions();
    }

    /**
     * 충전소 상세 조회
     * @param user nullable
     * @param statId 충전소id
     * @return StationDetailResponse
     */
    public StationDetailResponse getStationDetail(@Nullable User user, String statId) {
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 충전소입니다."));
        StationOperator operator = station.getStationOperator();

        List<Charger> chargers = chargerRepository.findByStatId(statId);

        // 비로그인 시 null, 로그인 시 알림 설정된 충전기id
        List<String> alertedIds = user != null
                ? chargerAlertRepository.findByUserAndCharger_StatId(user, statId)
                .stream().map(ca -> ca.getCharger().getChgerId())
                .toList()
                : List.of();

        boolean hasFast = chargers.stream()
                .anyMatch(c -> c.getChgerType() != ChgerType.AC_SLOW
                && c.getChgerType() != ChgerType.AC3
                && c.getChgerType() != ChgerType.DC_COMBO_SLOW);

        List<StationDetailResponse.ChgerDetail> chargerDetails = chargers.stream()
                .map(c -> new StationDetailResponse.ChgerDetail(
                        c.getChgerId(),
                        c.getChgerType(),
                        c.getOutput(),
                        c.getChgerStat(),
                        alertedIds.contains(c.getChgerId())
                )).toList();

        List<StationReviewResponse> reviews = reviewService.getReviewsByStation(user, statId);

        StationReviewsSummary summary = reviewService.getStationReviewsSummary(statId);

        boolean noAvailableCharger = chargers.stream().noneMatch(c -> c.getChgerStat() == ChgerStat.WAITING
        || c.getChgerStat() == ChgerStat.CHARGING
        || c.getChgerStat() == ChgerStat.RESERVED);

        StationDetailResponse.CongestionDetail congestionDetail;
        if (noAvailableCharger) {
            congestionDetail = new StationDetailResponse.CongestionDetail(null, null, null, null);
        } else {
            List<Congestion> congestions = congestionRepository.findByStationOrderByPredictedAtDesc(station);

            Map<Integer, CongestionLevel> levelByTargetTime = congestions.stream()
                    .filter(c -> c.getCongestionLevel() != null)
                    .collect(Collectors.toMap(Congestion::getTargetTime, Congestion::getCongestionLevel,
                            (latest, older) -> latest)); // predictedAt desc이므로 먼저 나온 게 최신

            Double accuracy = congestions.isEmpty() ? null :
                    congestions.get(0).getCongestionScore();

            congestionDetail = new StationDetailResponse.CongestionDetail(
                    accuracy,
                    levelByTargetTime.get(1),
                    levelByTargetTime.get(2),
                    levelByTargetTime.get(3)
            );
        }

        return  new StationDetailResponse(
                station.getStatId(),
                station.getStatNm(),
                station.getAddr(),
                station.getAddrDetail(),
                station.getLocation().getY(), // Point: x = 경도, y = 위도
                station.getLocation().getX(),
                station.getUseTime(),
                station.getParkingFree() != null ? YN.Y.equals(station.getParkingFree()) : null,
                station.getNote(),
                station.getLimitYn() != null ? YN.N.equals(station.getLimitYn()) : null,
                station.getLimitDetail(),
                station.getKind() != null ? Kind.descriptionOf(station.getKind().getCode()) : null,
                station.getKindDetail(),
                station.getFloorNum(),
                station.getFloorType() != null ? FloorType.descriptionOf(station.getFloorType().name()) : null,
                hasFast,
                operator.getBusiNm(),
                operator.getBusiCall(),
                summary.averageRating(),
                summary.reviewCount(),
                user != null ? favoriteRepository.existsByUserAndStation(user, station) : null,
                chargerDetails,
                reviews,
                congestionDetail
        );

    }
}
