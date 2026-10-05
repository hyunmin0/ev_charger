package ev_charger.be.car.dto.response;

import ev_charger.be.car.Car;

public record CarResponse(
        long carId,
        String brand,
        String model,
        String trim, // 없으면 null
        int modelYear,
        float batteryCapacity
) {
    public static CarResponse from(Car car) {
        return new CarResponse(
                car.getCarId(),
                car.getBrand(),
                car.getModel(),
                car.getTrim(),
                car.getModelYear(),
                car.getBatteryCapacity()
        );
    }
}
