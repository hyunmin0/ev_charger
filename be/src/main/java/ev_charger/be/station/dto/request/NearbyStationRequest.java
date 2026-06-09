package ev_charger.be.station.dto.request;

import ev_charger.be.station.StationFilter;


public record NearbyStationRequest(
        double lat,
        double lng,
        Integer range,
        String cursor, // 인코딩된 distance(nullable)
        StationFilter filter
) {
}
