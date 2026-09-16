package ev_charger.be.car;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarRepository carRepository;

    record CarResponse(long carId, String brand, String model, String trim, int modelYear, float batteryCapacity) {}

    @GetMapping
    public ResponseEntity<List<CarResponse>> searchCars(@RequestParam String keyword) {
        List<CarResponse> results = carRepository
                .findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(c -> new CarResponse(c.getCarId(), c.getBrand(), c.getModel(), c.getTrim(), c.getModelYear(), c.getBatteryCapacity()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }
}