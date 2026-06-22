package ev_charger.be.notification;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.notice.Notice;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findByUserAndNoticeIn(User user, List<Notice> notices);

    List<NotificationHistory> findByUserAndAlertIn(User user, List<ChargerAlert> alerts);

    boolean existsByUserAndNotice(User user, Notice notice);
    boolean existsByAlert(ChargerAlert alert);
}
