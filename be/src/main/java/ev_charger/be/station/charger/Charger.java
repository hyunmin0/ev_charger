package ev_charger.be.station.charger;

import ev_charger.be.station.Station;
import ev_charger.be.station.charger.enums.ChargingMethod;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
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
    @JdbcTypeCode(SqlTypes.CHAR) // table.sql: char
    @Column(name = "chgerType", nullable = false, length = 2)
    private ChgerType chgerType;

    @JdbcTypeCode(SqlTypes.CHAR) // table.sql: char
    @Column(name = "stat", nullable = false, length = 1)
    private ChgerStat chgerStat;

    @JdbcTypeCode(SqlTypes.CHAR) // table.sql: char
    @Column(name = "statUpdDt", nullable = false, length = 14)
    private String statUpdDt;

    @JdbcTypeCode(SqlTypes.CHAR) // table.sql: char
    @Column(name = "lastTsdt", length = 14)
    private String lastTsdt;
    @JdbcTypeCode(SqlTypes.CHAR) // table.sql: char
    @Column(name = "lastTedt", length = 14)
    private String lastTedt;
    @Column(length = 20)
    private String output;
    @Column(length = 10)
    private ChargingMethod method;

    // 테스트/데이터 생성용
    // station은 insertable = false라 statId(복합키)를 직접 채워야 insert 시 PK가 null이 안 됨
    @Builder
    public Charger(Station station, String chgerId, ChgerType chgerType, ChgerStat chgerStat,
                   String statUpdDt, String lastTsdt, String lastTedt, String output, ChargingMethod method) {
        this.station = station;
        this.statId = station.getStatId();
        this.chgerId = chgerId;
        this.chgerType = chgerType;
        this.chgerStat = chgerStat;
        this.statUpdDt = statUpdDt;
        this.lastTsdt = lastTsdt;
        this.lastTedt = lastTedt;
        this.output = output;
        this.method = method;
    }

}
