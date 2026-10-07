package ev_charger.be.auth.dto.request;

import ev_charger.be.user.enums.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// token: 카카오는 accessToken, 구글은 idToken
public record SocialLoginRequest(@NotBlank String token, @NotNull Provider provider) {
}
