package ev_charger.be.review;

import ev_charger.be.station.Station;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserAndStation(User user, Station station);

    List<Review> findByUser(User user);

    List<Review> findByStation(Station station);

    int countByUser(User user);

    @Query("select r.rating from Review r where r.station = :station")
    List<Integer> findRatingsByStation(Station station);
}
