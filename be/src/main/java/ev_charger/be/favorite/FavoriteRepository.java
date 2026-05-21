package ev_charger.be.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserid(UUID userid);

    // 즐겨찾기 내에 해당 충전소 존재 여부
    boolean existsByUseridAndStatId(UUID userid, int statId);

    void deleteByUseridAndStatId(UUID userid, int statId);

}
