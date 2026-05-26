package ev_charger.be.auth.dto.response;

public record SocialLoginResponse(String status, String accessToken, String refreshToken, String tempToken) {
    // status: SUCCESS / NEED_PROFILE_SELECT
}
