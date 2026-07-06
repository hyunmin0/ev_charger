package ev_charger.be.notification;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.notice.Notice;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findByUserAndNoticeIn(User user, List<Notice> notices);

    Optional<NotificationHistory> findByIdAndUser(Long id, User user);

    boolean existsByUserAndNotice(User user, Notice notice);

    // 충전기 알림 중에 일주일이 지난 기록 삭제
    void deleteByChgerIdIsNotNullAndCreatedAtBefore(LocalDateTime createdAt);
}
