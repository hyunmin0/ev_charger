package ev_charger.be.user.userCar.dto.response;


public record UserCarResponse(
        Long userCarId,
        long carId,
        String carName,
        float batteryCapacity
) {}