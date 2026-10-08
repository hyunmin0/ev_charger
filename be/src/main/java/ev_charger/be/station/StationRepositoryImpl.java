package ev_charger.be.station;

import ev_charger.be.station.congestion.CongestionLevelConverter;
import ev_charger.be.station.dto.request.MapBoundsRequest;
import ev_charger.be.station.dto.request.NearbyStationRequest;
import ev_charger.be.station.dto.response.RegionSummaryResponse;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.enums.Sido;
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

    // 네이티브 쿼리 결과의 혼잡도 문자열("여유" 등) -> CongestionLevel 변환용
    private static final CongestionLevelConverter CONGESTION_LEVEL_CONVERTER = new CongestionLevelConverter();


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
            select s."statId",
                s."statNm",
                s.addr,
                ST_Y(s.location::geometry) lat,
                ST_X(s.location::geometry) lng,
                s."useTime",
                s."parkingFree",
                s."limitYn",
                s.kind,
                s."floorType",
                count(c."chgerType") filter (where c."chgerType" not in ('02', '07', '08')) > 0 "hasFast",
                so."busiNm",
                count(c."chgerId") "totalCount",
                count(c."chgerId") filter (where c.stat = '2') "availableCount",
                count(c."chgerId") filter (where c.stat = '3') > 0 "hasCharging",
                count(c."chgerId") filter (where c.stat in ('0', '1', '9')) = count(c."chgerId") "allUnknown",
                count(c."chgerId") filter (where c.stat in ('4', '5', '6')) = count(c."chgerId") "allUnavailable",
                rv."averageRating", -- 리뷰 평균 (lateral 서브쿼리에서 미리 계산)
                rv."reviewCount", -- 리뷰 개수 (충전기 수와 곱해지지 않도록 따로 계산)
                ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) distance, -- 충전소 위치와 현 위치의 거리
                case when count(c."chgerId") filter (where c.stat in ('2', '3', '6')) = 0 then null else cg."congestionLevel" end "nextHourCongestionLevel" -- 충전대기, 충전중, 예약중이 아니면 null
            from station s
                join charger c on s."statId" = c."statId"
                join station_operator so on s."busiId" = so."busiId"
                left join lateral ( -- 리뷰는 충전소별로 먼저 집계 (charger와 같이 join하면 충전기 수 x 리뷰 수로 행이 늘어남)
                    select round(avg(r.rating)::numeric, 1) "averageRating", count(*) "reviewCount"
                    from review r
                    where r."statId" = s."statId"
                    ) rv on true -- 리뷰가 없어도 count 0, avg null인 1행이 나옴
                left join lateral ( -- lateral join: 바깥 값 참조 가능, 바깥 테이블의 각 행마다 재실행
                    select cg."congestionLevel"
                    from congestion cg
                    where cg."statId" = s."statId" and cg."targetTime" = 1
                    order by cg."predictedAt" desc
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
    public List<StationResponse> findStationsInBoundsWithFilter(MapBoundsRequest request, int maxCount) {

        StringBuilder sql = new StringBuilder("""
            select s."statId",
            s."statNm",
            s.addr,
            ST_Y(s.location::geometry) lat,
            ST_X(s.location::geometry) lng,
            s."useTime",
            s."parkingFree",
            s."limitYn",
            s.kind,
            s."floorType",
            count(c."chgerType") filter (where c."chgerType" not in ('02', '07', '08')) > 0 "hasFast",
            so."busiNm",
            count(c."chgerId") "totalCount",
            count(c."chgerId") filter (where c.stat = '2') "availableCount",
            count(c."chgerId") filter (where c.stat = '3') > 0 "hasCharging",
            count(c."chgerId") filter (where c.stat in ('0', '1', '9')) = count(c."chgerId") "allUnknown",
            count(c."chgerId") filter (where c.stat in ('4', '5', '6')) = count(c."chgerId") "allUnavailable",
            rv."averageRating", -- 리뷰 평균 (lateral 서브쿼리에서 미리 계산)
            rv."reviewCount", -- 리뷰 개수 (충전기 수와 곱해지지 않도록 따로 계산)
            ST_Distance(s.location, ST_MakePoint(:userLng, :userLat)::geography) distance, -- 충전소 위치와 현 위치의 거리
            case when count(c."chgerId") filter (where c.stat in ('2', '3', '6')) = 0 then null else cg."congestionLevel" end "nextHourCongestionLevel" -- 충전대기, 충전중, 예약중이 아니면 null
            from station s
                join charger c on s."statId" = c."statId"
                join station_operator so on s."busiId" = so."busiId"
                left join lateral ( -- 리뷰는 충전소별로 먼저 집계 (charger와 같이 join하면 충전기 수 x 리뷰 수로 행이 늘어남)
                    select round(avg(r.rating)::numeric, 1) "averageRating", count(*) "reviewCount"
                    from review r
                    where r."statId" = s."statId"
                    ) rv on true -- 리뷰가 없어도 count 0, avg null인 1행이 나옴
                left join lateral ( -- lateral join: 바깥 값 참조 가능, 바깥 테이블의 각 행마다 재실행
                    select cg."congestionLevel"
                    from congestion cg
                    where cg."statId" = s."statId" and cg."targetTime" = 1
                    order by cg."predictedAt" desc
                    limit 1
                    ) cg on true -- 조인 조건이 없음을 의미
            where ST_Within(s.location::geometry, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)) -- 위경도 최대최소 안에 존재하는 경우
            """);

        // 공통 필터 조건
        appendFilterWithGroupBy(sql, request.filter());

        // 화면 중심에서 가까운 maxCount곳만 고른 뒤 다시 유저와의 거리순으로 정렬
        // (공통 조건 끝에 한 줄 주석이 붙어 있어서 줄을 바꾼 뒤 감쌈)
        String limited = """
            select * from (
                select * from (
            """ + sql + """

                ) inner_t
                order by power(inner_t.lat - :centerLat, 2) + power((inner_t.lng - :centerLng) * cos(radians(:centerLat)), 2)
                limit :maxCount
            ) limited_t
            order by limited_t.distance
            """;

        Query query = em.createNativeQuery(limited, Tuple.class);

        // 이 메서드 고유 파라미터 바인딩
        query.setParameter("userLat", request.userLat());
        query.setParameter("userLng", request.userLng());
        query.setParameter("minLat", request.minLat());
        query.setParameter("maxLat", request.maxLat());
        query.setParameter("minLng", request.minLng());
        query.setParameter("maxLng", request.maxLng());
        query.setParameter("centerLat", (request.minLat() + request.maxLat()) / 2);
        query.setParameter("centerLng", (request.minLng() + request.maxLng()) / 2);
        query.setParameter("maxCount", maxCount);

        // 공통 필터 파라미터 바인딩
        bindFilterParams(query, request.filter());

        return toResponse(query.getResultList());
    }

    /**
     * 시·도별 충전소 수와 표시 위치
     * 위치는 평균 대신 중앙값: 좌표가 잘못 찍힌 충전소에 덜 흔들림
     */
    @Override
    public List<RegionSummaryResponse> findRegionSummaries() {
        String sql = """
            select st.zcode,
                count(*) "stationCount",
                count(*) filter (where st.available) "availableStationCount",
                percentile_cont(0.5) within group (order by st.lat) lat,
                percentile_cont(0.5) within group (order by st.lng) lng
            from (
                select s.zcode,
                    ST_Y(s.location::geometry) lat,
                    ST_X(s.location::geometry) lng,
                    bool_or(c.stat = '2') available -- 충전대기 충전기가 1대 이상
                from station s
                    join charger c on s."statId" = c."statId"
                group by s."statId"
            ) st
            group by st.zcode
            order by st.zcode
            """;

        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class).getResultList();
        return rows.stream()
                .map(row -> new RegionSummaryResponse(
                        row.get("zcode", String.class),
                        Sido.nameOf(row.get("zcode", String.class)),
                        row.get("stationCount", Number.class).longValue(),
                        row.get("availableStationCount", Number.class).longValue(),
                        row.get("lat", Number.class).doubleValue(),
                        row.get("lng", Number.class).doubleValue()))
                .toList();
    }

    /**
     * 시·군별 충전소 수와 표시 위치 (도 안의 시·군을 나눠 보여줌)
     * - 광역시·특별시는 구로 나누지 않고 하나로 묶음
     * - 도는 zscode 앞 4자리(시·군)로 묶음. 구가 있는 시(수원시 장안구·권선구 ...)도 시 하나가 됨
     * - 주소가 아니라 코드로 묶음: 주소는 표기가 섞이거나 비어 있는 충전소가 있어 엉뚱한 묶음이 생김
     * - 이름은 묶음 안 주소 두 번째 단어 중 "~시/~군"인 것의 최빈값 (DB에 시·군 코드-이름 표가 없음)
     */
    @Override
    public List<RegionSummaryResponse> findCitySummaries() {
        String sql = """
            select k.region_key "regionKey",
                min(k.zcode) zcode,
                mode() within group (order by k.tok) filter (where k.tok ~ '(시|군)$') "cityName",
                count(*) "stationCount",
                count(*) filter (where k.available) "availableStationCount",
                percentile_cont(0.5) within group (order by k.lat) lat,
                percentile_cont(0.5) within group (order by k.lng) lng
            from (
                select st.*,
                    case when st.zcode in (:metroCodes) then st.zcode
                         when st.zcode = :gwangjuZcode and left(st.zscode, 4) between :gwangjuFrom and :gwangjuTo then :gwangjuKey
                         else left(st.zscode, 4) end region_key
                from (
                    select s.zcode, s.zscode,
                        split_part(trim(s.addr), ' ', 2) tok,
                        ST_Y(s.location::geometry) lat,
                        ST_X(s.location::geometry) lng,
                        bool_or(c.stat = '2') available -- 충전대기 충전기가 1대 이상
                    from station s
                        join charger c on s."statId" = c."statId"
                    group by s."statId"
                ) st
            ) k
            group by k.region_key
            order by k.region_key
            """;

        Query query = em.createNativeQuery(sql, Tuple.class);
        query.setParameter("metroCodes", Sido.METRO_CODES);
        query.setParameter("gwangjuZcode", Sido.GWANGJU_ZCODE);
        query.setParameter("gwangjuFrom", Sido.GWANGJU_ZSCODE_FROM);
        query.setParameter("gwangjuTo", Sido.GWANGJU_ZSCODE_TO);
        query.setParameter("gwangjuKey", Sido.GWANGJU_KEY);

        List<Tuple> rows = query.getResultList();
        return rows.stream()
                .map(row -> {
                    String key = row.get("regionKey", String.class);
                    String zcode = row.get("zcode", String.class);
                    String cityName = row.get("cityName", String.class);
                    String name = Sido.GWANGJU_KEY.equals(key) ? Sido.GWANGJU_NAME
                            : key.equals(zcode) || cityName == null ? Sido.nameOf(zcode)
                            : cityName;
                    return new RegionSummaryResponse(
                            key,
                            name,
                            row.get("stationCount", Number.class).longValue(),
                            row.get("availableStationCount", Number.class).longValue(),
                            row.get("lat", Number.class).doubleValue(),
                            row.get("lng", Number.class).doubleValue());
                })
                .toList();
    }


    @Override
    public List<StationResponse> findFavoriteStations(UUID userId, double lat, double lng) {
        String sql = """
            select s."statId",
            s."statNm",
            s.addr,
            ST_Y(s.location::geometry) lat,
            ST_X(s.location::geometry) lng,
            s."useTime",
            s."parkingFree",
            s."limitYn",
            s.kind,
            s."floorType",
            count(c."chgerType") filter (where c."chgerType" not in ('02', '07', '08')) > 0 "hasFast",
            so."busiNm",
            count(c."chgerId") "totalCount",
            count(c."chgerId") filter (where c.stat = '2') "availableCount",
            count(c."chgerId") filter (where c.stat = '3') > 0 "hasCharging",
            count(c."chgerId") filter (where c.stat in ('0', '1', '9')) = count(c."chgerId") "allUnknown",
            count(c."chgerId") filter (where c.stat in ('4', '5', '6')) = count(c."chgerId") "allUnavailable",
            rv."averageRating", -- 리뷰 평균 (lateral 서브쿼리에서 미리 계산)
            rv."reviewCount", -- 리뷰 개수 (충전기 수와 곱해지지 않도록 따로 계산)
            ST_Distance(s.location, ST_MakePoint(:userLng, :userLat)::geography) distance, -- 충전소 위치와 현 위치의 거리
            case when count(c."chgerId") filter (where c.stat in ('2', '3', '6')) = 0 then null else cg."congestionLevel" end "nextHourCongestionLevel" -- 충전대기, 충전중, 예약중이 아니면 null
            from station s
                join charger c on s."statId" = c."statId"
                join station_operator so on s."busiId" = so."busiId"
                join favorite f on f."statId" = s."statId"
                left join lateral ( -- 리뷰는 충전소별로 먼저 집계 (charger와 같이 join하면 충전기 수 x 리뷰 수로 행이 늘어남)
                    select round(avg(r.rating)::numeric, 1) "averageRating", count(*) "reviewCount"
                    from review r
                    where r."statId" = s."statId"
                    ) rv on true -- 리뷰가 없어도 count 0, avg null인 1행이 나옴
                left join lateral ( -- lateral join: 바깥 값 참조 가능, 바깥 테이블의 각 행마다 재실행
                    select cg."congestionLevel"
                    from congestion cg
                    where cg."statId" = s."statId" and cg."targetTime" = 1
                    order by cg."predictedAt" desc
                    limit 1
                    ) cg on true -- 조인 조건이 없음을 의미
            where f.user_id = :userId
            group by s."statId", s."statNm", s.addr, s.location, s."useTime", s."parkingFree", s."limitYn", s.kind, s."floorType", so."busiNm", cg."congestionLevel", rv."averageRating", rv."reviewCount", f.created_at
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
     * averageRating은 round(...::numeric)라 BigDecimal로 옴 -> Number로 받고 .doubleValue()로 변환
     * parkingFree, limitYn은 길이 1 문자열이라 Character로 옴 -> Object로 받고 문자열로 변환
     * nextHourCongestionLevel은 db 값("여유" 등)이 문자열로 옴 -> converter로 enum 변환
     * List<?>: 타입을 모르는 리스트를 받을 때 쓰는 와일드카드(Tuple로 받으면 경고)
     */
    private List<StationResponse> toResponse(List<?> rows) {
        return rows.stream().map(r -> {
            Tuple row = (Tuple) r;
            String parkingFree = toStringOrNull(row.get("parkingFree"));
            String limitYn = toStringOrNull(row.get("limitYn"));
            Number averageRating = row.get("averageRating", Number.class);
            String congestionLevel = row.get("nextHourCongestionLevel", String.class);
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
                    averageRating != null ? averageRating.doubleValue() : null,
                    row.get("reviewCount", Number.class).intValue(),
                    row.get("distance", Double.class),
                    congestionLevel != null ? CONGESTION_LEVEL_CONVERTER.convertToEntityAttribute(congestionLevel) : null
            );
        }).toList();
    }

    /**
     * Character, String 등 문자 타입 값을 String으로 변환 (null이면 null)
     */
    private String toStringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * 공통 필터 where + group by / having / order by
     * 필터 값이 있읆 때만 해당 조건을 sql에 붙임
     */
    private void appendFilterWithGroupBy(StringBuilder sql, StationFilter filter) {
        // station 조건
        // Boolean.TRUE.equals: true일 때만 조건 추가 (null, false면 조건 없음)
        if (Boolean.TRUE.equals(filter.parkingFree())) sql.append(" AND s.\"parkingFree\" = 'Y'\n");
        if (Boolean.TRUE.equals(filter.limitYn())) sql.append(" AND s.\"limitYn\" = 'N'\n");
        // NUMERIC으로 변환 후 비교
        // NUMERIC: 소수점 포함 순자 타입
        if (filter.minOutput() != null) sql.append(" AND CAST(c.output AS NUMERIC) >= :minOutput\n");
        if (filter.maxOutput() != null) sql.append(" AND CAST(c.output AS NUMERIC) <= :maxOutput\n");

        // charger 조건
        if (hasValues(filter.chgerTypes())) sql.append(" AND c.\"chgerType\" IN (:chgerTypes)\n");
        if (hasValues(filter.kinds())) sql.append(" AND s.kind IN (:kinds)\n");
        if (hasValues(filter.floorTypes())) sql.append(" AND s.\"floorType\" IN (:floorTypes)\n");

        // 집계
        sql.append("""
            group by s."statId", s."statNm", s.addr, s.location, s."useTime", s."parkingFree", s."limitYn", s.kind, s."floorType", so."busiNm", cg."congestionLevel", rv."averageRating", rv."reviewCount"
            """);

        // availableOnly = true면 사용 가능한 충전기(stat='2')가 1개 이상인 충전소만 반환
        if (Boolean.TRUE.equals(filter.availableOnly())) sql.append(" having count(c.\"chgerId\") filter (where c.stat = '2') > 0 -- availableOnly = false면 전체 조회, true면 충전 가능한 충전소만 조회\n");

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