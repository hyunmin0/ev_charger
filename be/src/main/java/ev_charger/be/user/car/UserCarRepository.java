package ev_charger.be.user.car;

import ev_charger.be.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCarRepository extends JpaRepository<UserCar, UUID> {
    // findByCarid() == findById()이며 findById()는 jpa가 제공함
        // Id == PK

    List<UserCar> findByUser(User user);
    Optional<UserCar> deleteByCarId(UUID carId);

    @Query ("select uc.model from UserCar uc where uc.user= :user")
    // :user와 @Param("user")가 서로 매핑
    List<String> findModelsByUser(@Param("user") User user);
}


