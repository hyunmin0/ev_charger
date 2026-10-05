package ev_charger.be.charger_alert;

import ev_charger.be.station.charger.Charger;
import ev_charger.be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(name="charger_alert", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "statId", "chgerId"})})
public class ChargerAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="alert_id")
    private Long alertId;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumns({
        @JoinColumn(name = "chgerId", nullable = false),
        @JoinColumn(name = "statId", nullable = false)
    })
    @ManyToOne(fetch = FetchType.LAZY)
    private Charger charger;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChargerAlert(User user, Charger charger) {
        this.user = user;
        this.charger = charger;
    }
}
