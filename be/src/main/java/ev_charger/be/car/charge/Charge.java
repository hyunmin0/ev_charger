package ev_charger.be.car.charge;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="charge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Charge {
    @Id
    @Column(name="charge_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long chargeId;

    @Column(name="brand", nullable = false, length = 50)
    private String brand;

    @Column(name="model", nullable = false, length = 50)
    private String model;

    @Column(name="battery_type", nullable = false, length = 20)
    private String batteryType;

    @Column(name="model_year", nullable = false)
    private int modelYear;

    @Column(name="charger_type", nullable = false, length = 50)
    private String chargerType;

    @Column(name="charger_output")
    private Integer chargerOutput;

    @Column(name="minutes", nullable = false)
    private int minutes;

    @Builder
    public Charge(String brand, String model, String batteryType, int modelYear,
                  String chargerType, Integer chargerOutput, int minutes) {
        this.brand = brand;
        this.model = model;
        this.batteryType = batteryType;
        this.modelYear = modelYear;
        this.chargerType = chargerType;
        this.chargerOutput = chargerOutput;
        this.minutes = minutes;
    }
}