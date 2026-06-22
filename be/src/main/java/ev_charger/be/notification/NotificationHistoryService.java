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
        if (notificationHistoryRepository.existsByAlert(alert)) { // ChargerAlert는 처음부터 user에게 종속되어 있는 1:1 관계이므로 alert로만 확인 가능(user X)
            return; // 이미 읽음 기록 있으면 중복 생성 안 함
        }
        notificationHistoryRepository.save(
                NotificationHistory.alertBuilder()
                .alert(alert)
                .user(alert.getUser())
                        .build());
    }

    // notice는 User가 없으므로 User가 필요함
    @Transactional
    public void save(User user, Notice notice) {
        if (notificationHistoryRepository.existsByUserAndNotice(user, notice)) { // notice는 모든 유저에게 공유되기에 해당 유저에 한해서 읽었는지 확인해야 함
            return; // 이미 읽음 기록 있으면 중복 생성 안 함
        }
        notificationHistoryRepository.save(
                NotificationHistory.noticeBuilder()
                        .notice(notice)
                        .user(user)
                        .build());
    }

    @Transactional
    public void markAsRead(User user, Long id) {
        NotificationHistory history = notificationHistoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("알림 기록이 없습니다."));

        history.updateIsRead();
    }
}
