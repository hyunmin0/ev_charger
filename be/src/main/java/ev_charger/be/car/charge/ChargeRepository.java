package ev_charger.be.car.charge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByBrandAndModelAndBatteryTypeAndModelYear(
            String brand, String model, String batteryType, int modelYear);
}