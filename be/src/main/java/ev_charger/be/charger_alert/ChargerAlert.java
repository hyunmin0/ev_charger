package ev_charger.be.charger_alert;

import ev_charger.be.station.Station;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // created 자동 시간 측정
@Table(name="favorite", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "statId", "chgerId"})})
public class ChargerAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="alert_id")
    private Long alertId;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "statId", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Station station;

    @JoinColumn(name = "chgerId", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Charger charger;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChargerAlert(User user, Station station, Charger charger) {
        this.user = user;
        this.station = station;
        this.charger = charger;
    }
}
