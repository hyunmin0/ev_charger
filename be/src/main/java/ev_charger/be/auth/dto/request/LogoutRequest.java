package ev_charger.be.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

// accessToken은 Authorization 헤더로 받음
public record LogoutRequest(@NotBlank String refreshToken) {
}
