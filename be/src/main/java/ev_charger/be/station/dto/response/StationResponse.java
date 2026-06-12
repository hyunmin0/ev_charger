package ev_charger.be.station.dto.response;

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
        boolean hasFast, // 1개 이상이 급속
        String busiNm,
        Integer totalCount,
        Integer availableCount, // stat = 2인 개수
        boolean hasCharging, // stat = 3이 1개 이상
        boolean allUnknown, // 전체가 stat in (0, 9)
        boolean allUnavailable, // 전체가 stat in (1, 4, 5, 6)
        Double averageRating, // 리뷰 없을 땐 null
        int reviewCount,
        double distance // m
) {
}
