package ev_charger.be.station.dto.request;

public record NearbyStationRequest(
        double lat,
        double lng,
        Integer range,
        boolean availableOnly, // true = waiting만
        String cursor // null이면 첫 페이지, 아니면 인코딩된 "distance:lastId"
) {
}
