package ev_charger.be.favorite;

import ev_charger.be.station.Station;
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
@EntityListeners(AuditingEntityListener.class) // created 자동 시간 측정
@Table(name="favorite", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "lastId"})})
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @JoinColumn(name="user_id", nullable=false)
    @ManyToOne(fetch=FetchType.LAZY)
    private User user;

    @JoinColumn(name="lastId", nullable=false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Station station;

    @CreatedDate
    @Column(nullable = false, updatable = false) // 수정을 막기 위해
    private LocalDateTime created;

    @Builder
    public Favorite(User user, Station station) {
        this.user = user;
        this.station = station;
    }
}
