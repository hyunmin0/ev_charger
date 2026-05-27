package ev_charger.be.user.car;


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
            columnNames = {"model", "user_id"})})
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class UserCar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID carId;

    @JoinColumn(name="user_id", nullable = false)
    @ManyToOne(fetch=FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private String model;

    @Column(name="battery_capacity", nullable = false)
    private float batteryCapacity;

    @Builder
    public UserCar(User user, String model, float batteryCapacity) {
        this.user = user;
        this.model = model;
        this.batteryCapacity = batteryCapacity;
    }


}
