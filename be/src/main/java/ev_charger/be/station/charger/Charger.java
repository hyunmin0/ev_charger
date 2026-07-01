package ev_charger.be.station.charger;

import ev_charger.be.station.Station;
import ev_charger.be.station.charger.enums.ChargingMethod;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="charger")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@IdClass(ChargerId.class)
public class Charger {
    // 복합키: statId + chgerId
    @Id
    @Column(name = "statId", nullable = false, length = 8)
    private String statId; // statId 실제 값 관리용

    @Id
    @Column(name = "chgerId", nullable = false, length = 2)
    private String chgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statId", insertable = false, updatable = false)
    // statId와 같은 컴럼, Station 객체로 접근하기 위해 별도로 매핑
    private Station station; // 읽기 전용(statId FK 조회용)

    // @Enumerated: converter가 없는 경우 + enum이 db 값과 같은 경우 필요
    @Column(name = "chgerType", nullable = false, length = 2)
    private ChgerType chgerType;

    @Column(name = "stat", nullable = false, length = 1)
    private ChgerStat chgerStat;

    @Column(name = "statUpdDt", nullable = false, length = 14)
    private String statUpdDt;

    @Column(name = "lastTsdt", length = 14)
    private String lastTsdt;
    @Column(name = "lastTedt", length = 14)
    private String lastTedt;
    @Column(length = 20)
    private String output;
    @Column(length = 10)
    private ChargingMethod method;

}
