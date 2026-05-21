package ev_charger.be.user.car.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public record UserCarResponse(
        UUID carid,
        String model,
        float batteryCapacity
) {}
