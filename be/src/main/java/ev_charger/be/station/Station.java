package ev_charger.be.station;

import ev_charger.be.common.enums.YN;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import jakarta.persistence.*;
import lombok.*;

import org.locationtech.jts.geom.Point;

@Entity
@Table(name="station")
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Builder // 전체 생성자 자동 생성
@AllArgsConstructor(access = AccessLevel.PRIVATE) // builder와 같이 사용
@Getter
public class Station {

    @Id
    @Column(name = "statId", length = 8, nullable = false)
    private String statId;

    @Column(name = "statNm", length = 100, nullable = false)
    private String statNm;

    @Column(length = 150, nullable = false)
    private String addr;

    @Column(name = "addrDetail", length = 200)
    private String addrDetail;

    @Column(columnDefinition = "geography(Point, 4326)", nullable = false)
    private Point location;

    @Column(name = "useTime", length = 50, nullable = false)
    private String useTime;

    @JoinColumn(name = "busiId")
    @ManyToOne(fetch = FetchType.LAZY)
    private StationOperator stationOperator;

    @Column(length = 2, nullable = false)
    private String zcode;
    @Column(length = 5)
    private String zscode;

    @Column(length = 2)
    private Kind kind;

    @Column(name = "kindDetail", length = 4)
    private String kindDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "parkingFree", length = 1)
    private YN parkingFree;

    @Column(length = 200)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "limitYn", length = 1)
    private YN limitYn;

    @Column(name = "limitDetail", length = 100)
    private String limitDetail;

    @Column(name = "floorNum", length = 50)
    private String floorNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "floorType", length = 2)
    private FloorType floorType;
}
