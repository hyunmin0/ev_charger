package ev_charger.be.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationHistoryScheduler {
    private final NotificationHistoryService notificationHistoryService;

    /**
     * 알림 기록 삭제(매일 5시에 실행)
     */
    @Scheduled(cron = "0 0 5 * * *") // "초 분 시 일 월 요일"
    public void deleteExpiredAlertHistories() {
        notificationHistoryService.deleteExpiredAlertHistories();
    }
}
