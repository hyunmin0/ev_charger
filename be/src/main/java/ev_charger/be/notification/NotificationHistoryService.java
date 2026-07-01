package ev_charger.be.notification;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.notice.Notice;
import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    // save(charger) - ChargerAlertService에서 이미 ChargerAlert를 들고 있을 거기 때문에 객체를 받는 게 효율적임
    @Transactional
    public NotificationHistory save(ChargerAlert alert) {
        return notificationHistoryRepository.save(
                NotificationHistory.alertBuilder()
                .charger(alert.getCharger())
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

    /**
     * 충전기 알림 읽음 처리
     * @param user
     * @param id
     */
    @Transactional
    public void markAsRead(User user, Long id) {
        NotificationHistory history = notificationHistoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("알림 기록이 없습니다."));

        history.updateIsRead();
    }

    /**
     * 충전기 알림 기록 자동 삭제(일주일)
     */
    @Transactional
    public void deleteExpiredAlertHistories() {
        // 일주일 전 시간이랑 비교해서 createAt가 더 작으면 삭제
        notificationHistoryRepository.deleteByChgerIdIsNotNullAndCreatedAtBefore(LocalDateTime.now().minusWeeks(1)); // 현 시각 - 7일
    }
}
