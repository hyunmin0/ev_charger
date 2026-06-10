package ev_charger.be.station.dto.response;

public record StationResponse(
        String statId,
        String statNm,
        String addr,
        double lat,
        double lng,
        String useTime,
        Integer totalCount,
        Integer unKnownCount, // stat in (0, 9)
        Integer availableCount, // stat = 2
        Integer inUseCount, // stat = 3
        Integer unavailableCount, // stat in (1, 4, 5, 6)
        double distance // m
) {
}
