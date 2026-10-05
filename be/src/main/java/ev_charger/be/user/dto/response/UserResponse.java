package ev_charger.be.user.dto.response;

public record UserResponse (
        String nickname,
        String email,
        String imageUrl,
        int userCarCount,
        int reviewCount,
        int chargerAlertCount){
}