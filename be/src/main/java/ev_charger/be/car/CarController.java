package ev_charger.be.car;

import ev_charger.be.car.charge.ChargeRepository;
import ev_charger.be.car.dto.response.CarResponse;
import ev_charger.be.car.dto.response.ChargeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarRepository carRepository;
    private final ChargeRepository chargeRepository;

    // 차량 검색 (차량 추가 화면)
    // input : keyword(String) - 브랜드, 모델명, 트림 (예: "테슬라 Model Y")
    // output: List<CarResponse> { carId, brand, model, trim, modelYear, batteryCapacity }
    @GetMapping
    public ResponseEntity<List<CarResponse>> searchCars(@RequestParam String keyword) {
        // 앞뒤 공백 제거, 여러 칸 공백은 한 칸으로 (이어 붙인 이름은 한 칸씩 띄워져 있음)
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return ResponseEntity.ok(carRepository
                .searchByName(normalized)
                .stream()
                .map(CarResponse::from)
                .toList());
    }

    // 차량별 충전 시간 조회 (계산기 화면)
    // input : carId(Long) - 차량 id
    // output: List<ChargeResponse> { chargerType, chargerOutput, minutes }
    @GetMapping("/{carId}/charges")
    public ResponseEntity<List<ChargeResponse>> getCarCharges(@PathVariable long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));

        // 같은 브랜드/모델/배터리종류/연식의 충전 정보 조회
        return ResponseEntity.ok(chargeRepository
                .findByBrandAndModelAndBatteryTypeAndModelYear(
                        car.getBrand(), car.getModel(), car.getBatteryType(), car.getModelYear())
                .stream()
                .map(ChargeResponse::from)
                .toList());
    }
}
