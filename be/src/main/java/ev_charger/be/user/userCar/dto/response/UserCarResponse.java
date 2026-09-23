package ev_charger.be.user.userCar.dto.response;

import java.util.UUID;

public record UserCarResponse(
        UUID userCarId,
        long carId,
        String carName,
        float batteryCapacity
) {}