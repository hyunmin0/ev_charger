package ev_charger.be.station.dto.request;

public record MapBoundsRequest(
        double minLat,
        double maxLat,
        double minLng,
        double maxLng,
        double userLat,
        double userLng,
        boolean availableOnly
) {
}
