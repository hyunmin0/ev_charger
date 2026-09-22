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
    public MapBoundsRequest {
        // filter.* 파라미터가 하나도 없으면 null로 바인딩되므로 빈 필터로 대체
        if (filter == null) filter = StationFilter.empty();
    }
}
