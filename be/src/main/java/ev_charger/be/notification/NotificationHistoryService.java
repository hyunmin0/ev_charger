package ev_charger.be.notification;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.notice.Notice;
import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    // save(charger) - ChargerAlertService에서 이미 ChargerAlert를 들고 있을 거기 때문에 객체를 받는 게 효율적임
    @Transactional
    public void save(ChargerAlert alert) {
        notificationHistoryRepository.save(
                NotificationHistory.alertBuilder()
                .alert(alert)
                .user(alert.getUser())
                        .build());
    }

    // notice는 User가 없으므로 User가 필요함
    @Transactional
    public void save(User user, Notice notice) {
        notificationHistoryRepository.save(
                NotificationHistory.noticeBuilder()
                        .notice(notice)
                        .user(user)
                        .build());
    }

    // 사용자 알림 목록 조회(프론트의 안읽음/읽음 구분용)
    public List<NotificationHistory> getHistory(User user) {
        return notificationHistoryRepository.findByUser(user);
    }

    // 읽음 처리
    @Transactional
    public void markAsRead(User user, long notificationId) {
        NotificationHistory history = notificationHistoryRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("유효한 공지가 아닙니다."));

        if (!history.getUser().getUserId().equals(user.getUserId())){
            throw new IllegalArgumentException("해당 사용자의 공지가 아닙니다.");
        }

        history.markAsRead();
    }

}
