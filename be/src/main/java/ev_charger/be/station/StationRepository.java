package ev_charger.be.station;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, String> {

    // range 반경 안에 충전소 구하기
    // availableOnly = true: 충전 가능한 충전소만 조회
    @Query(value = """
    select s.statId,
           s.statNm,
           s.addr,
           ST_Y(s.location::geometry) lat,
           ST_X(s.location::geometry) lng,
           count(c.chgerId) totalCount,
           count(c.chgerId) filter (where c.stat = '2') availableCount, -- stat이 2인, 즉, 충전기의 상태가 waiting일 경우
           ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) distance -- 충전소 위치와 현 위치의 거리
    from station s join charger c on c.statId = s.statId
    where ST_DWithin(s.location, ST_MakePoint(:lng, :lat)::geography, :range) -- range(반경) 안에 존재하는 경우
        and (:cursorDistance is null -- 커서가 없으면 처음부터
             or ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) > :cursorDistance -- 이전 항목보다 거리가 먼 충전소
             or (ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) = :cursorDistance
                 and s.statId > :cursorStatId)) -- 거리가 동일한 충전소이자 statId가 더 큰 충전소
                                                -- 같은 거리 충전소일 때 중복/누락 방지
    group by s.statId, s.statNm, s.addr, s.location
    having (:availableOnly = false or count(c.chgerId) filter (where c.stat = '2') > 0) -- availableOnly = false면 전체 조회, true면 충전 가능한 충전소만 조회
    order by distance, s.statId -- 거리순 정렬, 거리가 동일하면 statId 기준(같은 거리 충전소일 때 중복/누락 방지)
    limit :pageSize
    """, nativeQuery = true)
    List<StationProjection> findNearbyStationsWithStats(
            double lat,
            double lng,
            Integer range,
            boolean availableOnly,
            Double cursorDistance,
            String cursorStatId,
            Integer pageSize
    );

}
