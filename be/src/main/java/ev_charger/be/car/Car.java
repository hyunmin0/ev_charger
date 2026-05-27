package ev_charger.be.car;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="car")
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Getter
public class Car {
    @Id
    @Column(name="car_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long carId;

    @Column(name="car_name", nullable = false, unique = true)
    private String carName;

    @Column(name="battery_capacity", nullable = false)
    private float batteryCapacity;

    @Builder
    public Car(String carName, float batteryCapacity) {
        this.carName = carName;
        this.batteryCapacity = batteryCapacity;
    }
}
