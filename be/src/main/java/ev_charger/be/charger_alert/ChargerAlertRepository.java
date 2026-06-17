package ev_charger.be.charger_alert;

import ev_charger.be.station.charger.Charger;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChargerAlertRepository extends JpaRepository<ChargerAlert,Long> {
    int countByUser(User user);

    List<ChargerAlert> findByUserAndCharger_StatId(User user, String statId);

    List<ChargerAlert> findByCharger_StatIdAndCharger_ChgerId(String statId, String chgerId);
    List<ChargerAlert> findByUser(User user);

    void deleteByUserAndCharger(User user, Charger charger);

    boolean existsByUserAndCharger(User user, Charger charger);
}
