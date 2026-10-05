package ev_charger.be.station.congestion;

import ev_charger.be.station.Station;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "congestion")
@Immutable // 읽기 전용
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Getter
public class Congestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "statId", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Station station;

    @Column(name = "targetTime")
    private int targetTime;

    @Column(name = "congestionLevel")
    private CongestionLevel congestionLevel;

    @Column(name = "congestionScore")
    private Double congestionScore;

    @Column(name = "predictedAt")
    private LocalDateTime predictedAt;

    // @Immutable이라도 insert는 가능 (update만 막힘)
    @Builder
    public Congestion(Station station, int targetTime, CongestionLevel congestionLevel,
                      Double congestionScore, LocalDateTime predictedAt) {
        this.station = station;
        this.targetTime = targetTime;
        this.congestionLevel = congestionLevel;
        this.congestionScore = congestionScore;
        this.predictedAt = predictedAt;
    }
}
