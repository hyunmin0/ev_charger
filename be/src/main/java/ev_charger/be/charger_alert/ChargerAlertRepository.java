package ev_charger.be.charger_alert;

import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargerAlertRepository extends JpaRepository<ChargerAlert,Long> {
    int countByUser(User user);
}
