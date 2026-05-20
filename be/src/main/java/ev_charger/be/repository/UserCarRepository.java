package ev_charger.be.repository;

import ev_charger.be.domain.UserCar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCarRepository extends JpaRepository<UserCar, UUID> {
    // findByCarid() == findById()이며 findById()는 jpa가 제공함
        // Id == PK

    List<UserCar> findByUserid(UUID userid);
    Optional<UserCar> deleteByCarid(UUID carid);
}


