package ev_charger.be.auth.client;

import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.user.Provider;
import org.springframework.stereotype.Component;

@Component
public class GoogleApiClient implements OAuthApiClient{
    @Override
    public UserInfo getUserInfo(String accessToken) {
        //...
        //return UserInfo(id, email, nickname);
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }
}
