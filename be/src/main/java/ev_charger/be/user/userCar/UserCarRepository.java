package ev_charger.be.user.userCar;

import ev_charger.be.car.Car;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCarRepository extends JpaRepository<UserCar, Long> {
    // findByCarid() == findById()이며 findById()는 jpa가 제공함
        // Id == PK

    List<UserCar> findByUser(User user);

    Optional<UserCar> findByUserAndUserCarId(User user, Long userCarId);

    Boolean existsByUserAndCar(User User, Car car);

    // 챗봇: 요청에 실린 carId(car 테이블 id)가 이 유저의 차량인지
    boolean existsByUserAndCar_CarId(User user, long carId);

    int countByUser(User user);

    @Query("select uc from UserCar uc where uc.user= :user")
    // :user와 @Param("user")가 서로 매핑
    List<UserCar> findCarByUser(@Param("user") User user);
}


