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

    // 알림 목록 조회
    // input : 없음 (로그인한 유저 기준)
    // output: List<NotificationHistoryResponse> [{ id, statId, chgerId, createdAt, isRead }, ...]
    @GetMapping
    public ResponseEntity<List<NotificationHistoryResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails // 로그인한 유저
    ) {
        return ResponseEntity.ok(notificationHistoryService.getChargerAlertHistories(userDetails.getUser()));
    }

    // 알림 읽음 처리
    // input : id(Long) - 알림 기록 id
    // output: 없음 (200 OK)
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @PathVariable Long id                                   // URL에서 알림 기록 id 받음
    ) {
        notificationHistoryService.markAsRead(userDetails.getUser(), id);
        return ResponseEntity.ok().build();
    }

    // 알림 단건 삭제
    // input : id(Long) - 알림 기록 id
    // output: 없음 (200 OK)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @PathVariable Long id                                   // URL에서 알림 기록 id 받음
    ) {
        notificationHistoryService.deleteHistory(userDetails.getUser(), id);
        return ResponseEntity.ok().build();
    }

    // 알림 전체 삭제
    // input : 없음 (로그인한 유저 기준)
    // output: 없음 (200 OK)
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails // 로그인한 유저
    ) {
        notificationHistoryService.deleteAllChargerAlertHistories(userDetails.getUser());
        return ResponseEntity.ok().build();
    }
}