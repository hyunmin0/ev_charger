package ev_charger.be.auth.client;

import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.user.Provider;
import org.springframework.stereotype.Repository;

public interface OAuthApiClient {
    UserInfo getUserInfo(String accessToken);
    Provider getProvider();
}
