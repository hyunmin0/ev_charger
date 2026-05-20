package ev_charger.be.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name="user_car")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class UserCar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID carid;

    private UUID userid;
    private String model;
    private float batteryCapacity;

    @Builder
    public UserCar(UUID userid, String model, float batteryCapacity) {
        this.userid = userid;
        this.model = model;
        this.batteryCapacity = batteryCapacity;
    }
}
