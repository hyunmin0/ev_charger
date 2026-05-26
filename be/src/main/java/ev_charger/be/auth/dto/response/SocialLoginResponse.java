package ev_charger.be.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SocialLoginResponse {
    private String status; // SUCCESS / NEED_PROFILE_SELECT
    private String accessToken;
    private String refreshToken;
    private String tempToken;
}
