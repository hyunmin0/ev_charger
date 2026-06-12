package ev_charger.be.station.charger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerRepository extends JpaRepository<Charger, ChargerId> {
    List<Charger> findByStatId(String statId);
}
