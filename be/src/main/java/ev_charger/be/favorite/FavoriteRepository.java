package ev_charger.be.favorite;

import ev_charger.be.station.Station;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 즐겨찾기 내에 해당 충전소 존재 여부
    boolean existsByUserAndStation(User user, Station station);

    void deleteByUserAndStation(User user, Station station);

    int countByUser(User user);
}
