package ev_charger.be.auth.dto.request;

public record RegisterRequest(String tempToken, String nickname, Integer profileImageId) {
}
