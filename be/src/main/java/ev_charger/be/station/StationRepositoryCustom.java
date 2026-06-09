package ev_charger.be.station;

import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.StationResponse;

import java.util.List;

public interface StationRepositoryCustom {
    List<StationResponse> findNearbyStationsWithFilter(NearbyStationRequest request, Double cursorDistance);

    List<StationResponse> findStationsInBoundsWithFilter(MapBoundsRequest request);
}
