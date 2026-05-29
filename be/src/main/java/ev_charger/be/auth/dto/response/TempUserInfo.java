package ev_charger.be.auth.dto.response;

import ev_charger.be.user.enums.Provider;

public record TempUserInfo(Provider provider, String providerId, String email) {
}
