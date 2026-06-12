package ev_charger.be.station.stationOperator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationOperatorRepository extends JpaRepository<StationOperator,Long> {
}
