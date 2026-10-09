package ev_charger.be.station.dto.request;

public record StationSearchRequest(
        String keyword,
        double lat,
        double lng
) {
}