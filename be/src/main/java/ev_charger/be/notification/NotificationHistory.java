package ev_charger.be.notification;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.notice.Notice;
import ev_charger.be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name="notification_history")
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name="alert_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private ChargerAlert alert;

    @JoinColumn(name="notice_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Notice notice;

    @JoinColumn(name="user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false, name = "is_read")
    private boolean isRead = false; // 기본값: false

    @CreatedDate
    @Column(name="create_at", nullable = false, updatable = false)
    private LocalDateTime creatAt;

    @Builder(builderMethodName = "alertBuilder")
    public NotificationHistory(ChargerAlert alert, User user) {
        this.alert = alert;
        this.user = user;
    }

    @Builder(builderMethodName = "noticeBuilder")
    public NotificationHistory(Notice notice, User user) {
        this.notice = notice;
        this.user = user;
        this.isRead = true; // 읽었을 때만 저장
    }

    public void updateIsRead() {
        this.isRead = true;
    }
}
