package ev_charger.be.user.userCar;

import ev_charger.be.car.Car;
import ev_charger.be.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCarRepository extends JpaRepository<UserCar, Long> {
    // findByCarid() == findById()이며 findById()는 jpa가 제공함
        // Id == PK

    List<UserCar> findByUser(User user);

    Optional<UserCar> findByUserAndUserCarId(User user, long userCarId);

    Boolean existsByUserAndCar(User User, Car car);

    @Query ("select uc.car from UserCar uc where uc.user= :user")
    // :user와 @Param("user")가 서로 매핑
    List<UserCar> findCarByUser(@Param("user") User user);
}


