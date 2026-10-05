package ev_charger.be.station.dto.request;

import ev_charger.be.station.StationFilter;


public record NearbyStationRequest(
        double lat,
        double lng,
        Integer range,
        String cursor, // 인코딩된 distance(nullable)
        StationFilter filter
) {
    public NearbyStationRequest {
        // filter.* 파라미터가 하나도 없으면 null로 바인딩되므로 빈 필터로 대체
        if (filter == null) filter = StationFilter.empty();
    }
}
