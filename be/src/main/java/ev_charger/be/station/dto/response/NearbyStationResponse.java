package ev_charger.be.station.dto.response;

public record NearbyStationResponse(
        String statId,
        String statNm,
        String addr,
        double lat,
        double lng,
        Integer totalCount,
        Integer availableCount,
        double distance // m
) {
}
