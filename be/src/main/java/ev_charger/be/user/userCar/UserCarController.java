package ev_charger.be.user.userCar;

import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.user.userCar.dto.response.UserCarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/cars")
@RequiredArgsConstructor

public class UserCarController {

    private final UserCarService userCarService;

    // 차량 추가
    // input : carId(long), batteryCapacity(float)
    // output: 없음 (200 OK)
    @PostMapping
    public ResponseEntity<Void> addUserCar(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @RequestParam long carId,                               // 차 id
            @RequestParam float batteryCapacity) {                  // 배터리 용량
        userCarService.addUserCar(userDetails.getUser(), carId, batteryCapacity);
        return ResponseEntity.ok().build();
    }
}
