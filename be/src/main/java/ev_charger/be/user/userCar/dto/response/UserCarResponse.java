package ev_charger.be.user.userCar.dto.response;

import ev_charger.be.car.Car;
import ev_charger.be.user.userCar.UserCar;

public record UserCarResponse(
        Long userCarId,
        long carId,
        String carName,
        float batteryCapacity, // 사용자가 등록할 때 입력한 값
        String batteryType, // 롱레인지/스탠다드 등, 구분이 없는 차종(DB에 문자열 'None')은 null
        int modelYear
) {
    public static UserCarResponse from(UserCar userCar) {
        Car car = userCar.getCar();
        String batteryType = car.getBatteryType() == null || "None".equals(car.getBatteryType())
                ? null : car.getBatteryType();

        return new UserCarResponse(
                userCar.getUserCarId(),
                car.getCarId(),
                car.getDisplayName(),
                userCar.getBatteryCapacity(),
                batteryType,
                car.getModelYear()
        );
    }
}
