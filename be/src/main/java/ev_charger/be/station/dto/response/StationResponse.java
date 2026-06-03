package ev_charger.be.station.dto.response;

public record StationResponse(
        String statId,
        String statNm,
        String addr,
        double lat,
        double lng,
        String useTime,
        Integer totalCount,
        Integer availableCount,
        double distance // m
) {
}
