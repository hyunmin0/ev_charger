package ev_charger.be.station.dto.response;

import java.util.List;

public record NearbyStationPageResponse(
        List<StationResponse> stations,
        String nextCursor // 마지막 id, distance 인코딩
) {
}
