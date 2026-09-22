package ev_charger.be.car;

import ev_charger.be.car.dto.response.CarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarRepository carRepository;

    @GetMapping
    public ResponseEntity<List<CarResponse>> searchCars(@RequestParam String keyword) {
        return ResponseEntity.ok(carRepository.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(CarResponse::from)
                .toList());
    }
}