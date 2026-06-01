package ev_charger.be.favorite;

import ev_charger.be.station.Station;
import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    // 즐겨찾기 조회
    @Query(value = """
        select s.statId statId,
               s.statNm statNm,
               s.addr addr,
               ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) distance, -- intelliJ가 postGIS 함수를 몰라서 경고일 듯?
               f.created created
        from favorite f
        join station s on f.statId = s.statId
        where f.user_id = :userId
        order by created desc -- 최신 순
    """, nativeQuery = true) // PostGis 함수 사용을 위해 네이티브 쿼리 사용
    List<FavoriteProjection> findByUserWithStation(UUID userId, double lat, double lng);

    // 즐겨찾기 내에 해당 충전소 존재 여부
    boolean existsByUserAndStation(User user, Station station);

    void deleteByUserAndStation(User user, Station station);

}
