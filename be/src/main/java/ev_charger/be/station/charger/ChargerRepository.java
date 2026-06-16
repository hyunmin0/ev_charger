package ev_charger.be.station.charger;

import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargerRepository extends JpaRepository<Charger, ChargerId> {
    List<Charger> findByStatId(String statId);
    Optional<Charger> findByStatIdAndChgerId(String statId, String chgerId);

    @Query("select c from Charger c join fetch c.station s join fetch s.stationOperator where c.statId in :statIds")
    List<Charger> findByStatIdInWithStation(Collection<String> statIds);
}
