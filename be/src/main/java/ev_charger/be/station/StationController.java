package ev_charger.be.station;

import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.NearbyStationPageResponse;
import ev_charger.be.station.dto.response.StationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    // 가까운 충전소 찾기 (내 위치 기준 반경 내)
    // input : NearbyStationRequest { lat, lng, range, cursor, availableOnly }
    // output: NearbyStationPageResponse { stations, nextCursor }
    @GetMapping("/nearby")
    public ResponseEntity<NearbyStationPageResponse> getNearbyStations(
            @ModelAttribute NearbyStationRequest request) { // URL 파라미터를 DTO로 한번에 받음
        return ResponseEntity.ok(stationService.getNearbyStations(request));
    }

    // 지도 화면 내의 충전소 찾기
    // input : MapBoundsRequest { minLat, maxLat, minLng, maxLng, userLat, userLng, availableOnly }
    // output: List<StationResponse>
    @GetMapping("/bounds")
    public ResponseEntity<List<StationResponse>> getStationsInBounds(
            @ModelAttribute MapBoundsRequest request) { // URL 파라미터를 DTO로 한번에 받음
        return ResponseEntity.ok(stationService.getStationsInBounds(request));
    }
}