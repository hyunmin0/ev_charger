package ev_charger.be.station;

import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.StationResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class StationRepositoryImpl implements StationRepositoryCustom {

    // jpa 기본 제공 객체: 직접 네이티브 쿼리를 실행
    private final EntityManager em;

    /**
     * 내 위치 기준 반경 내 충전소 조회
     * @param request 위경도, 반경, 필터
     * @param cursorDistance 커서 기반 페이징용 마지막 거리 (null이면 첫 페이지)
     * @return 충전소 목록
     */
    @Override
    public List<StationResponse> findNearbyStationsWithFilter(NearbyStationRequest request, Double cursorDistance) {

        // 기본 select
        // 두 쿼리 모두 동일한 컬럼 구조 (toResponse의 Object[] 인덱스 순서와 일치해야 함)
        StringBuilder sql = new StringBuilder("""
            select s.statId,
                   s.statNm,
                   s.addr,
                   ST_Y(s.location::geometry) lat,
                   ST_X(s.location::geometry) lng,
                   s.useTime,
                   count(c.chgerId) totalCount,
                   count(c.chgerId) filter (where c.stat = '2') availableCount, -- stat이 2인, 즉, 충전기의 상태가 waiting일 경우
                   ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) distance -- 충전소 위치와 현 위치의 거리
            from station s join charger c on c.statId = s.statId
            where ST_DWithin(s.location, ST_MakePoint(:lng, :lat)::geography, :range) -- range(반경) 안에 존재하는 경우
            """);

        // 커서가 있으면 해당 거리 이후의 충전소만 조회 (페이징)
        // findNearbyStationWithFilter에만 있는 조건이라 공통 헬퍼 밖에 위치
        if (cursorDistance != null)  sql.append(" and ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) > :cursorDistance \n");

        // 공통 필터 조건 + 그룹핑, 순서 추가
        appendFilterWithGroupBy(sql, request.filter());

        // sql(=문자열) 작성 후 쿼리 객체 생성
        Query query = em.createNativeQuery(sql.toString());

        // 이 메서드 고유 파라미터 바인딩
        // (sql 문자열 안의 :lat, :range 같은 값에 실제 값을 채워 넣음)
        // sql 인젝션 방지를 위해 직접 문자열을 넣지 않음
        query.setParameter("lat", request.lat());
        query.setParameter("lng", request.lng());
        query.setParameter("range", request.range());

        // 커서가 있을 때만 바인딩
        if (cursorDistance != null) query.setParameter("cursorDistance", cursorDistance);

        // 공통 필터 파라미터 바인딩
        bindFilterParams(query, request.filter());

        return toResponse(query.getResultList());

    }

    /**
     * 현재 지도 화면(bounds) 안의 충전소 조회
     * @param request 최대최소 위경도, 유저 위치, 필터
     * @return 충전소 목록
     */
    @Override
    public List<StationResponse> findStationsInBoundsWithFilter(MapBoundsRequest request) {

        StringBuilder sql = new StringBuilder("""
            select s.statId,
                s.statNm,
                s.addr,
                ST_Y(s.location::geometry) lat,
                ST_X(s.location::geometry) lng,
                s.useTime,
                count(c.chgerId) totalCount,
                count(c.chgerId) filter (where c.stat = '2') availableCount, -- stat이 2인, 즉, 충전기의 상태가 waiting일 경우
                ST_Distance(s.location, ST_MakePoint(:userLng, :userLat)::geography) distance -- 충전소 위치와 현 위치의 거리
            from station s join charger c on s.statId = c.statId
            where ST_Within(s.location::geometry, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)) -- 위경도 최대최소 안에 존재하는 경우
            """);

        // 공통 필터 조건
        appendFilterWithGroupBy(sql, request.filter());

        Query query = em.createNativeQuery(sql.toString());

        // 이 메서드 고유 파라미터 바인딩
        query.setParameter("userLat", request.userLat());
        query.setParameter("userLng", request.userLng());
        query.setParameter("minLat", request.minLat());
        query.setParameter("maxLat", request.maxLat());
        query.setParameter("minLng", request.minLng());
        query.setParameter("maxLng", request.maxLng());

        // 공통 필터 파라미터 바인딩
        bindFilterParams(query, request.filter());

        return toResponse(query.getResultList());
    }

    /**
     * 리스트가 null이거나 비어있으면 false -> sql 조건 및 파라미터 바인딩 스킵
     */
    private boolean hasValues(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * 네이티브 쿼리 결과(Object[]) -> StationResponse 변환
     * select 컬럼 순서대로 인덱스 접근
     * Number로 받는 이유: long, double, bigDecimal 등 어떤 타입이 와도 Number의 자식이므로 안전하게 처리 가능
     */
    private List<StationResponse> toResponse(List<?> rows) {
        return rows.stream().map(r -> {
            Object[] row = (Object[]) r;
            return new StationResponse(
                    (String)  row[0], // statId
                    (String)  row[1], // statNm
                    (String)  row[2], // addr
                    ((Number) row[3]).doubleValue(), // lat
                    ((Number) row[4]).doubleValue(), // lng
                    (String)  row[5], // useTime
                    ((Number) row[6]).intValue(), // totalCount
                    ((Number) row[7]).intValue(), // availableCount
                    ((Number) row[8]).doubleValue() // distance
            );
        }).toList();
    }

    /**
     * 공통 필터 where + group by / having / order by
     * 필터 값이 있읆 때만 해당 조건을 sql에 붙임
     */
    private void appendFilterWithGroupBy(StringBuilder sql, StationFilter filter) {
        // station 조건
        if (filter.parkingFree()) sql.append(" AND s.parkingFree = 'Y'\n");
        // NUMERIC으로 변환 후 비교
        // NUMERIC: 소수점 포함 순자 타입
        if (filter.minOutput() != null) sql.append(" AND CAST(c.output AS NUMERIC) >= :minOutput\n");
        if (filter.maxOutput() != null) sql.append(" AND CAST(c.output AS NUMERIC) <= :maxOutput\n");

        // charger 조건
        if (hasValues(filter.chgerTypes())) sql.append(" AND c.chgerType IN (:chgerTypes)\n");
        if (hasValues(filter.kinds())) sql.append(" AND s.kind IN (:kinds)\n");
        if (hasValues(filter.floorTypes())) sql.append(" AND s.floorType IN (:floorTypes)\n");

        // 집계 및 정렬
        // availableOnly = true면 사용 가능한 충전기(stat='2')가 1개 이상인 충전소만 반환
        sql.append("""
            group by s.statId, s.statNm, s.addr, s.location, s.useTime
            having (:availableOnly = false or count(c.chgerId) filter (where c.stat = '2') > 0) -- availableOnly = false면 전체 조회, true면 충전 가능한 충전소만 조회
            order by distance -- 거리순
            """);
    }

    /**
     * 공통 필터 파라미터 바인딩
     * sql에 추가된 조건에 대응하는 값만 바인딩
     * 리스트 파라미터(in 절)는 hibernate 전용 setParameterList 사용
     */
    private void bindFilterParams(Query query, StationFilter filter) {
        // availableOnly는 항상 존재하므로 항상 바인딩
        query.setParameter("availableOnly", filter.availableOnly());

        if (filter.minOutput() != null) query.setParameter("minOutput", filter.minOutput());
        if (filter.maxOutput() != null) query.setParameter("maxOutput", filter.maxOutput());

        // setParameterList: jpa 기본 query로는 리스트 바인딩 불가 -> hibernate nativeQuery로 unwrap 후 사용
        // (unwrap: jpa query에 숨어있는 실제 구현체(hibernate nativeQuery)를 꺼내는 메서드)
        // hasvalues()로 리스트의 null, 빈리스트 체크
        if (hasValues(filter.chgerTypes())) query.unwrap(NativeQuery.class).setParameterList("chgerTypes", filter.chgerTypes());
        if (hasValues(filter.kinds())) query.unwrap(NativeQuery.class).setParameterList("kinds", filter.kinds());
        if (hasValues(filter.floorTypes())) query.unwrap(NativeQuery.class).setParameterList("floorTypes", filter.floorTypes());
    }
}