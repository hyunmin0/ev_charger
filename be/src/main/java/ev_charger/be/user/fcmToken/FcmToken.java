package ev_charger.be.user.fcmToken;
// 알림 처리용

import ev_charger.be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate; // createdAt 자동 시간 측정
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Table(name="fcm_token")
@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name="user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private String token;

    @CreatedDate
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public FcmToken(User user, String token) {
        this.user = user;
        this.token = token;
    }
}
