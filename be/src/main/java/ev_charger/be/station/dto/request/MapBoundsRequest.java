package ev_charger.be.station.dto.request;

import ev_charger.be.station.StationFilter;


public record MapBoundsRequest(
        double minLat,
        double maxLat,
        double minLng,
        double maxLng,
        double userLat,
        double userLng,
        StationFilter filter
) {
}
