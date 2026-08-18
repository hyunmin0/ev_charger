package ev_charger.be.station;

import ev_charger.be.station.congestion.CongestionLevel;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


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
        StringBuilder sql = new StringBuilder("""
            select s.statId,
                s.statNm,
                s.addr,
                ST_Y(s.location::geometry) lat,
                ST_X(s.location::geometry) lng,
                s.useTime,
                s.parkingFree,
                s.limitYn,
                s.kind,
                s.floorType,
                count(c.chgerType) filter (where c.chgerType not in ('02', '07', '08')) > 0 hasFast,
                so.busiNm,
                count(c.chgerId) totalCount,
                count(c.chgerId) filter (where c.stat = '2') availableCount,
                count(c.chgerId) filter (where c.stat = '3') > 0 hasCharging,
                count(c.chgerId) filter (where c.stat in ('0', '1', '9')) = count(c.chgerId) allUnknown,
                count(c.chgerId) filter (where c.stat in ('4', '5', '6')) = count(c.chgerId) allUnavailable,
                round(avg(r.rating)::numeric, 1) averageRating,
                count(r.review_id) reviewCount,
                ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) distance, -- 충전소 위치와 현 위치의 거리
                case when count(c.chgerId) filter (where c.stat in ('2', '3', '6')) = 0 then null else cg.congestionLevel end nextHourCongestionLevel -- 충전대기, 충전중, 예약중이 아니면 null
            from station s
                join charger c on s.statId = c.statId
                join station_operator so on s.busiId = so.busiId
                left join review r on r.statId = s.statId
                left join lateral ( -- lateral join: 바깥 값 참조 가능, 바깥 테이블의 각 행마다 재실행
                    select cg.congestionLevel
                    from congestion cg
                    where cg.statId = s.statId and cg.targetTime = 1
                    order by cg.predictedAt desc
                    limit 1
                    ) cg on true -- 조인 조건이 없음을 의미
            where ST_DWithin(s.location, ST_MakePoint(:lng, :lat)::geography, :range) -- range(반경) 안에 존재하는 경우
            """);

        // 커서가 있으면 해당 거리 이후의 충전소만 조회 (페이징)
        // findNearbyStationWithFilter에만 있는 조건이라 공통 헬퍼 밖에 위치
        if (cursorDistance != null)  sql.append(" and ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) > :cursorDistance \n");

        // 공통 필터 조건 + 그룹핑, 순서 추가
        appendFilterWithGroupBy(sql, request.filter());

        // sql(=문자열) 작성 후 쿼리 객체 생성
        Query query = em.createNativeQuery(sql.toString(), Tuple.class);

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
            s.parkingFree,
            s.limitYn,
            s.kind,
            s.floorType,
            count(c.chgerType) filter (where c.chgerType not in ('02', '07', '08')) > 0 hasFast,
            so.busiNm,
            count(c.chgerId) totalCount,
            count(c.chgerId) filter (where c.stat = '2') availableCount,
            count(c.chgerId) filter (where c.stat = '3') > 0 hasCharging,
            count(c.chgerId) filter (where c.stat in ('0', '1', '9')) = count(c.chgerId) allUnknown,
            count(c.chgerId) filter (where c.stat in ('4', '5', '6')) = count(c.chgerId) allUnavailable,
            round(avg(r.rating)::numeric, 1) averageRating,
            count(r.review_id) reviewCount,
            ST_Distance(s.location, ST_MakePoint(:userLng, :userLat)::geography) distance, -- 충전소 위치와 현 위치의 거리
            case when count(c.chgerId) filter (where c.stat in ('2', '3', '6')) = 0 then null else cg.congestionLevel end nextHourCongestionLevel -- 충전대기, 충전중, 예약중이 아니면 null
            from station s
                join charger c on s.statId = c.statId
                join station_operator so on s.busiId = so.busiId
                left join review r on r.statId = s.statId
                left join lateral ( -- lateral join: 바깥 값 참조 가능, 바깥 테이블의 각 행마다 재실행
                    select cg.congestionLevel
                    from congestion cg
                    where cg.statId = s.statId and cg.targetTime = 1
                    order by cg.predictedAt desc
                    limit 1
                    ) cg on true -- 조인 조건이 없음을 의미
            where ST_Within(s.location::geometry, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)) -- 위경도 최대최소 안에 존재하는 경우
            """);

        // 공통 필터 조건
        appendFilterWithGroupBy(sql, request.filter());

        Query query = em.createNativeQuery(sql.toString(), Tuple.class);

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


    @Override
    public List<StationResponse> findFavoriteStations(UUID userId, double lat, double lng) {
        String sql = """
            select s.statId,
            s.statNm,
            s.addr,
            ST_Y(s.location::geometry) lat,
            ST_X(s.location::geometry) lng,
            s.useTime,
            s.parkingFree,
            s.limitYn,
            s.kind,
            s.floorType,
            count(c.chgerType) filter (where c.chgerType not in ('02', '07', '08')) > 0 hasFast,
            so.busiNm,
            count(c.chgerId) totalCount,
            count(c.chgerId) filter (where c.stat = '2') availableCount,
            count(c.chgerId) filter (where c.stat = '3') > 0 hasCharging,
            count(c.chgerId) filter (where c.stat in ('0', '1', '9')) = count(c.chgerId) allUnknown,
            count(c.chgerId) filter (where c.stat in ('4', '5', '6')) = count(c.chgerId) allUnavailable,
            round(avg(r.rating)::numeric, 1) averageRating,
            count(r.review_id) reviewCount,
            ST_Distance(s.location, ST_MakePoint(:userLng, :userLat)::geography) distance, -- 충전소 위치와 현 위치의 거리
            case when count(c.chgerId) filter (where c.stat in ('2', '3', '6')) = 0 then null else cg.congestionLevel end nextHourCongestionLevel -- 충전대기, 충전중, 예약중이 아니면 null
            from station s
                join charger c on s.statId = c.statId
                join station_operator so on s.busiId = so.busiId
                join favorite f on f.statId = s.statId
                left join review r on r.statId = s.statId
                left join lateral ( -- lateral join: 바깥 값 참조 가능, 바깥 테이블의 각 행마다 재실행
                    select cg.congestionLevel
                    from congestion cg
                    where cg.statId = s.statId and cg.targetTime = 1
                    order by cg.predictedAt desc
                    limit 1
                    ) cg on true -- 조인 조건이 없음을 의미
            where f.user_id = :userId
            group by s.statId, s.statNm, s.addr, s.location, s.useTime, s.parkingFree, s.limitYn, s.kind, s.floorType, so.busiNm, cg.congestionLevel
            order by f.created_at desc
            """;

        Query query = em.createNativeQuery(sql, Tuple.class);
        query.setParameter("userId", userId);
        query.setParameter("userLat", lat);
        query.setParameter("userLng", lng);

        return toResponse(query.getResultList());
    }

    /**
     * 리스트가 null이거나 비어있으면 false -> sql 조건 및 파라미터 바인딩 스킵
     */
    private boolean hasValues(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * 네이티브 쿼리 결과(Tuple) -> StationResponse 변환
     * Number로 받는 이유: 정수형은 db에서 bigint, numeric으로 올 수 있어서 Integer.class로 바로 받으면 예외날 수 있음. -> Number로 받고 .intValue()로 변환
     * Double은 db의 float8이나 double precision이랑 타입이 맞아서 바로 받아도 됨
     * List<?>: 타입을 모르는 리스트를 받을 때 쓰는 와일드카드(Tuple로 받으면 경고)
     */
    private List<StationResponse> toResponse(List<?> rows) {
        return rows.stream().map(r -> {
            Tuple row = (Tuple) r;
            String parkingFree = row.get("parkingFree", String.class);
            String limitYn = row.get("limitYn", String.class);
            return new StationResponse(
                    row.get("statId", String.class),
                    row.get("statNm", String.class),
                    row.get("addr", String.class),
                    row.get("lat", Double.class),
                    row.get("lng", Double.class),
                    row.get("useTime", String.class),
                    parkingFree != null ? "Y".equals(parkingFree) : null,
                    limitYn != null ? "N".equals(limitYn) : null,
                    Kind.descriptionOf(row.get("kind", String.class)), // 시설명 문자열로
                    FloorType.descriptionOf(row.get("floorType", String.class)), // 지상/지하
                    row.get("hasFast", Boolean.class),
                    row.get("busiNm", String.class),
                    row.get("totalCount", Number.class).intValue(),
                    row.get("availableCount", Number.class).intValue(),
                    row.get("hasCharging", Boolean.class),
                    row.get("allUnknown", Boolean.class),
                    row.get("allUnavailable", Boolean.class),
                    row.get("averageRating", Double.class),
                    row.get("reviewCount", Number.class).intValue(),
                    row.get("distance", Double.class),
                    row.get("nextHourCongestionLevel", CongestionLevel.class)
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
        if (filter.limitYn()) sql.append(" AND s.limitYn = 'N'\n");
        // NUMERIC으로 변환 후 비교
        // NUMERIC: 소수점 포함 순자 타입
        if (filter.minOutput() != null) sql.append(" AND CAST(c.output AS NUMERIC) >= :minOutput\n");
        if (filter.maxOutput() != null) sql.append(" AND CAST(c.output AS NUMERIC) <= :maxOutput\n");

        // charger 조건
        if (hasValues(filter.chgerTypes())) sql.append(" AND c.chgerType IN (:chgerTypes)\n");
        if (hasValues(filter.kinds())) sql.append(" AND s.kind IN (:kinds)\n");
        if (hasValues(filter.floorTypes())) sql.append(" AND s.floorType IN (:floorTypes)\n");

        // 집계
        sql.append("""
            group by s.statId, s.statNm, s.addr, s.location, s.useTime, s.parkingFree, s.limitYn, s.kind, s.floorType, so.busiNm, cg.congestionLevel
            """);

        // availableOnly = true면 사용 가능한 충전기(stat='2')가 1개 이상인 충전소만 반환
        if (filter.availableOnly()) sql.append(" having count(c.chgerId) filter (where c.stat = '2') > 0 -- availableOnly = false면 전체 조회, true면 충전 가능한 충전소만 조회\n");

        // 정렬
        sql.append(" order by distance -- 거리순");
    }

    /**
     * 공통 필터 파라미터 바인딩
     * sql에 추가된 조건에 대응하는 값만 바인딩
     * 리스트 파라미터(in 절)는 hibernate 전용 setParameterList 사용
     */
    private void bindFilterParams(Query query, StationFilter filter) {

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