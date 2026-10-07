package ev_charger.be.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoCodeLoginRequest(@NotBlank String code) {
}
