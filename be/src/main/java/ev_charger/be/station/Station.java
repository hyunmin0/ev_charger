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
    @Column(length = 8, nullable = false)
    private String statId;

    @Column(length = 100, nullable = false)
    private String statNm;

    @Column(length = 150, nullable = false)
    private String addr;

    @Column(length = 200)
    private String addrDetail;

    @Column(columnDefinition = "geography(Point, 4326)", nullable = false)
    private Point location;

    @Column(length = 50, nullable = false)
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

    @Column(length = 4)
    private String kindDetail;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private YN parkingFree;

    @Column(length = 200)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private YN limitYn;

    @Column(length = 100)
    private String limitDetail;

    @Column(length = 50)
    private String floorNum;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private FloorType floorType;
}
