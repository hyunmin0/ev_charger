package ev_charger.be.station;

import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.StationResponse;

import java.util.List;
import java.util.UUID;

public interface StationRepositoryCustom {
    List<StationResponse> findNearbyStationsWithFilter(NearbyStationRequest request, Double cursorDistance);

    List<StationResponse> findStationsInBoundsWithFilter(MapBoundsRequest request);

    List<StationResponse> findFavoriteStations(UUID userId, double lat, double lng);
}
