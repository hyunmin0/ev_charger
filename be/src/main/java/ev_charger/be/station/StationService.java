package ev_charger.be.station;

import ev_charger.be.charger_alert.ChargerAlertRepository;
import ev_charger.be.common.CursorUtils;
import ev_charger.be.common.enums.YN;
import ev_charger.be.favorite.FavoriteRepository;
import ev_charger.be.review.Review;
import ev_charger.be.review.ReviewRepository;
import ev_charger.be.review.ReviewService;
import ev_charger.be.review.dto.response.StationReviewResponse;
import ev_charger.be.review.dto.response.StationReviewsSummary;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.station.charger.ChargerRepository;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.NearbyStationPageResponse;
import ev_charger.be.station.dto.response.StationDetailResponse;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        return stationRepository.findStationsInBoundsWithFilter(request);
    }


    public StationDetailResponse getStationDetail(@Nullable User user, String statId) {
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 충전소입니다."));
        StationOperator operator = station.getStationOperator();

        List<Charger> chargers = chargerRepository.findByStatId(statId);
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

        return  new StationDetailResponse(
                station.getStatId(),
                station.getStatNm(),
                station.getAddr(),
                station.getAddrDetail(),
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
                reviews
        );

    }
}
