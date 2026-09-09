package ev_charger.be.notification;

import ev_charger.be.notification.dto.NotificationHistoryResponse;
import ev_charger.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;

    @GetMapping
    public ResponseEntity<List<NotificationHistoryResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(notificationHistoryService.getChargerAlertHistories(userDetails.getUser()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        notificationHistoryService.markAsRead(userDetails.getUser(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        notificationHistoryService.deleteHistory(userDetails.getUser(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationHistoryService.deleteAllChargerAlertHistories(userDetails.getUser());
        return ResponseEntity.ok().build();
    }
}