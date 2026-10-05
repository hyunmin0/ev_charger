package ev_charger.be.charger_alert;


import ev_charger.be.charger_alert.dto.request.ChargerStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/charger-alerts") // internal: client에서 접근 x
@RequiredArgsConstructor
public class ChargerAlertController {

    private final ChargerAlertService chargerAlertService;

    @Value("${internal.secret-key}")
    private String internalSecretKey;

    /**
     * 업데이트된 사용가능한 충전기 정보를 받아 해당하는 user에게 알림 발송
     * client에서 접근 x (파이썬 서버 -> 백엔드)
     * @param internalKey 파이썬 서버를 확인하는 key
     * @param chargers charStat = 02(=waiting)인 (statId, chgerId)의 리스트
     */
    @PostMapping("/notify")
    public ResponseEntity<Void> notifyChargerAlert(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestBody List<ChargerStatusRequest> chargers
    ) {
        // 파이썬 서버에서만 접근 가능(인증 실패: 403)
        if (!internalKey.equals(internalSecretKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // chargerAlert에 해당하는 user에서 fcm 발송
        chargerAlertService.notifyWaitingChargers(chargers);

        return ResponseEntity.ok().build();
    }
}
