package ev_charger.be.car.carCharger;

import ev_charger.be.car.enums.AtomicChargerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="car_charger")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CarCharger {
    @Id
    @Column(name="car_charger_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long carChargerId;

    @Column(name="brand", nullable = false, length = 50)
    private String brand;

    @Column(name="model", nullable = false, length = 50)
    private String model;

    @Column(name="model_year", nullable = false)
    private int modelYear;

    @Column(name="charger_type", nullable = false, length = 2)
    private AtomicChargerType chargerType;

    @Builder
    public CarCharger(String brand, String model, int modelYear, AtomicChargerType chargerType) {
        this.brand = brand;
        this.model = model;
        this.modelYear = modelYear;
        this.chargerType = chargerType;
    }
}