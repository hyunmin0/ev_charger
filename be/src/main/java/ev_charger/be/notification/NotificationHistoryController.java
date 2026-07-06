package ev_charger.be.notification;


import ev_charger.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
            ) {
        notificationHistoryService.markAsRead(userDetails.getUser(), id);
        return ResponseEntity.ok().build();
    }

}
