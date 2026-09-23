package ev_charger.be.car;

import ev_charger.be.car.charge.Charge;
import ev_charger.be.car.charge.ChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarRepository carRepository;
    private final ChargeRepository chargeRepository;

    record CarResponse(long carId, String brand, String model, String trim, int modelYear, float batteryCapacity) {}
    record ChargeResponse(String chargerType, Integer chargerOutput, int minutes) {}

    @GetMapping
    public ResponseEntity<List<CarResponse>> searchCars(@RequestParam String keyword) {
        List<CarResponse> results = carRepository
                .findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(c -> new CarResponse(c.getCarId(), c.getBrand(), c.getModel(), c.getTrim(), c.getModelYear(), c.getBatteryCapacity()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{carId}/charges")
    public ResponseEntity<List<ChargeResponse>> getCarCharges(@PathVariable long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));
        List<ChargeResponse> result = chargeRepository
                .findByBrandAndModelAndBatteryTypeAndModelYear(
                        car.getBrand(), car.getModel(), car.getBatteryType(), car.getModelYear())
                .stream()
                .map(c -> new ChargeResponse(c.getChargerType(), c.getChargerOutput(), c.getMinutes()))
                .toList();
        return ResponseEntity.ok(result);
    }
}