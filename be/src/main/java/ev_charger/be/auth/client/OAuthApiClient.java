package ev_charger.be.auth.client;

import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.user.enums.Provider;

public interface OAuthApiClient {
    // token: 카카오는 accessToken, 구글은 idToken
    UserInfo getUserInfo(String token);
    Provider getProvider();
}
