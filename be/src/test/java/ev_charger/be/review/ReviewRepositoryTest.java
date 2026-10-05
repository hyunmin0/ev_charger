package ev_charger.be.review;

import ev_charger.be.common.enums.YN;
import ev_charger.be.config.JpaAuditingConfig;
import ev_charger.be.config.TestcontainersConfig;
import ev_charger.be.station.Station;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
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

// 설정은 StationRepositoryImplTest와 동일 (table.sql 스키마 + validate + 따옴표 컬럼명)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// JpaAuditingConfig: 리뷰 created_at, updated_at 자동 입력
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager em;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static final int RATING = 5;
    private static final int SECOND_RATING = 3;
    private static final int OTHER_STATION_RATING = 1;

    private User user;
    private User secondUser;
    private Station station;
    private Station otherStation;
    private Station noReviewStation;

    @BeforeEach
    void setUp() {
        StationOperator operator = em.persist(StationOperator.builder()
                .busiId("ME")
                .busiNm("기후에너지환경부")
                .busiCall("02-1234-5678")
                .build());

        user = em.persist(User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build());
        secondUser = em.persist(User.builder()
                .nickname("다른유저")
                .email("other@example.com")
                .provider(Provider.KAKAO)
                .providerId("kakao-5678")
                .build());

        station = em.persist(station("TS000001", operator));
        otherStation = em.persist(station("TS000002", operator));
        noReviewStation = em.persist(station("TS000003", operator));

        // station: 리뷰 2개 (5점, 3점), otherStation: 리뷰 1개 (1점)
        em.persist(Review.builder().user(user).station(station).rating(RATING).content("충전 속도가 빨라요").build());
        em.persist(Review.builder().user(secondUser).station(station).rating(SECOND_RATING).content("주차 공간이 좁아요").build());
        em.persist(Review.builder().user(user).station(otherStation).rating(OTHER_STATION_RATING).content("고장이 잦아요").build());

        em.flush();
    }

    @Test
    void 해당_충전소의_별점만_조회한다() {
        // when
        List<Integer> ratings = reviewRepository.findRatingsByStation(station);

        // then
        assertThat(ratings).containsExactlyInAnyOrder(RATING, SECOND_RATING); // otherStation의 1점은 제외
    }

    @Test
    void 리뷰가_없는_충전소면_빈_리스트를_반환한다() {
        // when
        List<Integer> ratings = reviewRepository.findRatingsByStation(noReviewStation);

        // then
        assertThat(ratings).isEmpty();
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
}
