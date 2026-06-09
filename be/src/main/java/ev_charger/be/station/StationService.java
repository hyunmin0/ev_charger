package ev_charger.be.station;

import ev_charger.be.common.CursorUtils;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.NearbyStationPageResponse;
import ev_charger.be.station.dto.response.StationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class StationService {

    private final CursorUtils cursorUtils;
    private final StationRepository stationRepository;

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
}
