package ev_charger.be.user.userCar.dto.response;

import java.util.UUID;

public record UserCarResponse(
        UUID userCarId,
        String carName,
        float batteryCapacity
) {}