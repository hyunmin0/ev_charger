package ev_charger.be.charger_alert;


import ev_charger.be.charger_alert.dto.request.ChargerStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/charger-alerts")
@RequiredArgsConstructor
public class ChargerAlertController {

    private final ChargerAlertService chargerAlertService;

    @Value("${internal.secret-key}")
    private String internalSecretKey;

    @PostMapping("/notify")
    public ResponseEntity<Void> notifyChargerAlert(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestBody List<ChargerStatusRequest> chargers
    ) {
        // 파이썬 서버에서만 접근 가능(인증 실패: 403)
        if (!internalKey.equals(internalSecretKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        chargerAlertService.notifyWaitingChargers(chargers);

        return ResponseEntity.ok().build();
    }
}
