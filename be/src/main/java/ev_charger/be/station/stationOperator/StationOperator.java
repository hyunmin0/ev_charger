package ev_charger.be.station.stationOperator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@Table(name ="station_operator")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StationOperator {
    @Id
    @Column(nullable = false, length = 2)
    String busiId;

    @Column(nullable = false, length = 50)
    String busiNm;

    @Column(nullable = false, length = 50)
    String busiCall;
}
