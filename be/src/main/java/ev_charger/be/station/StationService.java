package ev_charger.be.station;

import ev_charger.be.common.CursorUtils;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.NearbyStationPageResponse;
import ev_charger.be.station.dto.response.NearbyStationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class StationService {
    // 한 번 조회 시 20개
    private static final int PAGE_SIZE = 20;

    private final CursorUtils cursorUtils;
    private final StationRepository stationRepository;

    /**
     * 가까운 충전소 찾기
     * @param request 경도, 위도, 범위(m), 충전가능만(T/F)
     * @return list<충전소id, 충전소이름, 주소, 경도, 위도, 총 충전기 수, 사용 가능 충전기 수, 거리>, 다음 커서
     */
    public NearbyStationPageResponse getNearbyStations(NearbyStationRequest request) {

        // 첫 페이지면 null, 아니면 커서에서 마지막 statId, distance 추출
        Double cursorDistance = null;
        String cursorStatId = null;

        // 커서가 있으면 디코딩해서 마지막 statId, distance 추출
        // 다음 쿼리에서 이 값 이후의 데이터만 가져오기 위해
        if (request.cursor() != null) {
            CursorUtils.CursorData cursorData = cursorUtils.decode(request.cursor());
            cursorDistance = cursorData.distance();
            cursorStatId = cursorData.lastId();
        }

        // range(반경) 안의 충전소를 거리순으로 PAGE_SIZE개만 조회
        // cursorDistance, cursorStatId 이후의 데이터만 가져옴
        List<NearbyStationResponse> stations = stationRepository.findNearbyStationsWithStats(
                request.lat(), request.lng(), request.range(), request.availableOnly(), cursorDistance, cursorStatId, PAGE_SIZE)
                .stream()
                .map(p -> new NearbyStationResponse(
                        p.getStatId(),
                        p.getStatNm(),
                        p.getAddr(),
                        p.getLat(),
                        p.getLng(),
                        p.getTotalCount(),
                        p.getAvailableCount(),
                        p.getDistance()
                ))
                .toList();

        // 조회 결과가 PAGE_SIZE개면 다음 페이지가 존재할 수 있음
        // PAGE_SIZE보다 적으면 마지막 페이지이므로 nextCursor = null
        String nextCursor = null;
        if (stations.size() == PAGE_SIZE) {
            // .get(index)
            NearbyStationResponse last = stations.get(stations.size() - 1);
            // 마지막 statId, distance를 인코딩해서 nextCursor 생성
            nextCursor = cursorUtils.encode(last.statId(), last.distance());
        }

        return new NearbyStationPageResponse(stations, nextCursor);
    }
}
