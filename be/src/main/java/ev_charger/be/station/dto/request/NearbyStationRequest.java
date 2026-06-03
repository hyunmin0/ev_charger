package ev_charger.be.station.dto.request;

public record NearbyStationRequest(
        double lat,
        double lng,
        Integer range,
        boolean availableOnly, // true = waiting만
        String cursor // 인코딩된 distance(nullable)
) {
}
