package ev_charger.be.charger_alert;

import ev_charger.be.charger_alert.dto.response.UserChargerAlertResponse;
import ev_charger.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/charger-alerts")
@RequiredArgsConstructor
public class UserChargerAlertController {

    private final ChargerAlertService chargerAlertService;

    // 내 충전기 알림 목록 조회
    // output: List<UserChargerAlertResponse>
    @GetMapping
    public ResponseEntity<List<UserChargerAlertResponse>> getMyAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chargerAlertService.getChargerAlertsByUser(userDetails.getUser()));
    }

    // 충전기 알림 추가
    // input: statId(path), chgerId(path)
    @PostMapping("/{statId}/{chgerId}")
    public ResponseEntity<Void> addAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String statId,
            @PathVariable String chgerId) {
        chargerAlertService.addChargerAlert(userDetails.getUser(), statId, chgerId);
        return ResponseEntity.ok().build();
    }

    // 충전기 알림 해제
    // input: statId(path), chgerId(path)
    @DeleteMapping("/{statId}/{chgerId}")
    public ResponseEntity<Void> deleteAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String statId,
            @PathVariable String chgerId) {
        chargerAlertService.deleteChargerAlert(userDetails.getUser(), statId, chgerId);
        return ResponseEntity.ok().build();
    }
}