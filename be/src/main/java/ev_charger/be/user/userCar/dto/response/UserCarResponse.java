package ev_charger.be.user.userCar.dto.response;

public record UserCarResponse(
        long userCarId,
        String carName,
        float batteryCapacity
) {}
