package ev_charger.be.user.userCar;


import ev_charger.be.car.Car;
import ev_charger.be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name="user_car",
    uniqueConstraints = { @UniqueConstraint(
            columnNames = {"car_id", "user_id"})})
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class UserCar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userCarId;

    @JoinColumn(name="user_id", nullable = false)
    @ManyToOne(fetch=FetchType.LAZY)
    private User user;

    @JoinColumn(name="car_id", nullable = false)
    @ManyToOne(fetch=FetchType.LAZY)
    private Car car;

    @Column(name="batter_capacity", nullable = false)
    private Float batteryCapacity;

    @Builder
    public UserCar(User user, Car car, Float batteryCapacity) {
        this.user = user;
        this.car = car;
        this.batteryCapacity = batteryCapacity;
    }
}
