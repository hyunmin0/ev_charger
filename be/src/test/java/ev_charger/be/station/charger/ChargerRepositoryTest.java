package ev_charger.be.station.charger;

import ev_charger.be.common.enums.YN;
import ev_charger.be.config.JpaAuditingConfig;
import ev_charger.be.config.TestcontainersConfig;
import ev_charger.be.station.Station;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

// 설정은 StationRepositoryImplTest와 동일 (table.sql 스키마 + validate + 따옴표 컬럼명)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
class ChargerRepositoryTest {

    @Autowired
    private ChargerRepository chargerRepository;

    @Autowired
    private TestEntityManager em;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static final String BUSI_NM = "기후에너지환경부";

    private static final String FIRST_STAT_ID = "TS000001";
    private static final String SECOND_STAT_ID = "TS000002";
    private static final String THIRD_STAT_ID = "TS000003";
    private static final String CHGER_ID = "01";
    private static final String SECOND_CHGER_ID = "02";
    private static final String STAT_UPD_DT = "20260930120000";

    @BeforeEach
    void setUp() {
        StationOperator operator = em.persist(StationOperator.builder()
                .busiId("ME")
                .busiNm(BUSI_NM)
                .busiCall("02-1234-5678")
                .build());

        // 충전소 3개: 첫 번째 2대, 두 번째 1대, 세 번째 1대
        Station first = em.persist(station(FIRST_STAT_ID, operator));
        Station second = em.persist(station(SECOND_STAT_ID, operator));
        Station third = em.persist(station(THIRD_STAT_ID, operator));

        em.persist(charger(first, CHGER_ID));
        em.persist(charger(first, SECOND_CHGER_ID));
        em.persist(charger(second, CHGER_ID));
        em.persist(charger(third, CHGER_ID));

        em.flush();
        // 영속성 컨텍스트를 비워서 조회 시 DB에서 새로 읽도록 (fetch join 여부 확인용)
        em.clear();
    }

    @Test
    void statIds에_해당하는_충전소의_충전기만_조회한다() {
        // given
        List<String> statIds = List.of(FIRST_STAT_ID, SECOND_STAT_ID);

        // when
        List<Charger> chargers = chargerRepository.findByStatIdInWithStation(statIds);

        // then
        assertThat(chargers)
                .extracting(Charger::getStatId, Charger::getChgerId)
                // 세 번째 충전소의 충전기는 제외
                .containsExactlyInAnyOrder(
                        tuple(FIRST_STAT_ID, CHGER_ID),
                        tuple(FIRST_STAT_ID, SECOND_CHGER_ID),
                        tuple(SECOND_STAT_ID, CHGER_ID)
                );
    }

    @Test
    void 충전소와_운영기관을_함께_조회한다() {
        // given
        List<String> statIds = List.of(FIRST_STAT_ID);

        // when
        List<Charger> chargers = chargerRepository.findByStatIdInWithStation(statIds);

        // then
        // fetch join이 빠지면 station, stationOperator가 프록시(초기화 안 됨)로 남음
        assertThat(chargers).isNotEmpty().allSatisfy(charger -> {
            assertThat(Hibernate.isInitialized(charger.getStation())).isTrue();
            assertThat(Hibernate.isInitialized(charger.getStation().getStationOperator())).isTrue();
            assertThat(charger.getStation().getStationOperator().getBusiNm()).isEqualTo(BUSI_NM);
        });
    }

    @Test
    void 해당하는_충전소가_없으면_빈_리스트를_반환한다() {
        // given
        List<String> statIds = List.of("NOTEXIST");

        // when
        List<Charger> chargers = chargerRepository.findByStatIdInWithStation(statIds);

        // then
        assertThat(chargers).isEmpty();
    }

    private Station station(String statId, StationOperator operator) {
        return Station.builder()
                .statId(statId)
                .statNm("테스트 충전소 " + statId)
                .addr("서울특별시 중구 세종대로 110")
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(126.9780, 37.5665)))
                .useTime("24시간 이용가능")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.PUBLIC)
                .parkingFree(YN.Y)
                .limitYn(YN.N)
                .floorType(FloorType.F)
                .build();
    }

    private Charger charger(Station station, String chgerId) {
        return Charger.builder()
                .station(station)
                .chgerId(chgerId)
                .chgerType(ChgerType.DC_COMBO)
                .chgerStat(ChgerStat.WAITING)
                .statUpdDt(STAT_UPD_DT)
                .output("100")
                .build();
    }
}
