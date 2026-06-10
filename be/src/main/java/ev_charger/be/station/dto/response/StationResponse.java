package ev_charger.be.station.dto.response;

import java.util.List;

public record StationResponse(
        String statId,
        String statNm,
        String addr,
        double lat,
        double lng,
        String useTime,
        boolean parkingFree,
        boolean limitYn,
        String kind,
        String floorType,
        List<String> chgerTypes,
        Integer totalCount,
        Integer unknownCount, // stat in (0, 9)
        Integer availableCount, // stat = 2
        Integer inUseCount, // stat = 3
        Integer unavailableCount, // stat in (1, 4, 5, 6)
        double distance // m
) {
}
