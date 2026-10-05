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

    @Column(name="brand", nullable = false, length = 50)
    private String brand;

    @Column(name="model", nullable = false, length = 50)
    private String model;

    @Column(name="battery_type", nullable = false, length = 20)
    private String batteryType;

    @Column(name="model_year", nullable = false)
    private int modelYear;

    @Column(name="drive_type", nullable = false, length = 3)
    private String driveType;

    @Column(name="wheel_size", nullable = false)
    private int wheelSize;

    @Column(name="battery_capacity", nullable = false)
    private float batteryCapacity;

    @Column(name="trim", length = 50)
    private String trim;

    @Column(name="combined")
    private Float combined;

    @Column(name="city")
    private Float city;

    @Column(name="highway")
    private Float highway;

    @Builder
    public Car(String brand, String model, String batteryType, int modelYear,
               String driveType, int wheelSize, float batteryCapacity, String trim,
               Float combined, Float city, Float highway) {
        this.brand = brand;
        this.model = model;
        this.batteryType = batteryType;
        this.modelYear = modelYear;
        this.driveType = driveType;
        this.wheelSize = wheelSize;
        this.batteryCapacity = batteryCapacity;
        this.trim = trim;
        this.combined = combined;
        this.city = city;
        this.highway = highway;
    }

    public String getDisplayName() {
        return (trim == null || trim.isBlank())
                ? brand + " " + model
                : brand + " " + model + " " + trim;
    }
}