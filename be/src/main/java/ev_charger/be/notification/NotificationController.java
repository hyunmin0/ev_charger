package ev_charger.be.notification;

import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationHistoryService notificationHistoryService;

    // 알림 목록 조회
    @GetMapping("")
    public ResponseEntity<List<NotificationHistory>> getHistory(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificationHistoryService.getHistory(user));
    }

    // 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable long notificationId
    ) {
        notificationHistoryService.markAsRead(user, notificationId);
        return ResponseEntity.ok().build();
    }


}
