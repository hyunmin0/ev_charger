package ev_charger.be.station.congestion;


import ev_charger.be.station.Station;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface CongestionRepository extends Repository<Congestion,Long> {
    // 혼잡도 조회(최신순)
    List<Congestion> findByStationOrderByPredictedAtDesc(Station station);

}
