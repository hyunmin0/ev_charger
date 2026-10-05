package ev_charger.be.user.userCar;

import ev_charger.be.car.Car;
import ev_charger.be.config.JpaAuditingConfig;
import ev_charger.be.config.TestcontainersConfig;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
class UserCarRepositoryTest {

    @Autowired
    private UserCarRepository userCarRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private User secondUser;
    private User noCarUser;
    private Car ioniq;
    private Car ev6;

    @BeforeEach
    void setUp() {
        user = em.persist(user("테스터", "test@example.com", "google-1234"));
        secondUser = em.persist(user("다른유저", "other@example.com", "google-5678"));
        noCarUser = em.persist(user("차없음", "nocar@example.com", "google-9999"));

        ioniq = em.persist(car("아이오닉5", 84.0f));
        ev6 = em.persist(car("EV6", 77.4f));

        // user: 차 2대, secondUser: 차 1대 (user와 같은 차종)
        em.persist(UserCar.builder().user(user).car(ioniq).batteryCapacity(84.0f).build());
        em.persist(UserCar.builder().user(user).car(ev6).batteryCapacity(77.4f).build());
        em.persist(UserCar.builder().user(secondUser).car(ioniq).batteryCapacity(84.0f).build());

        em.flush();
    }

    @Test
    void 해당_유저의_차량만_조회한다() {
        // when
        List<UserCar> userCars = userCarRepository.findCarByUser(user);

        // then
        assertThat(userCars)
                .extracting(userCar -> userCar.getCar().getModel())
                .containsExactlyInAnyOrder("아이오닉5", "EV6"); // secondUser의 아이오닉5는 제외
        assertThat(userCars)
                .allSatisfy(userCar -> assertThat(userCar.getUser().getUserId()).isEqualTo(user.getUserId()));
    }

    @Test
    void 등록한_차량이_없으면_빈_리스트를_반환한다() {
        // when
        List<UserCar> userCars = userCarRepository.findCarByUser(noCarUser);

        // then
        assertThat(userCars).isEmpty();
    }

    private User user(String nickname, String email, String providerId) {
        return User.builder()
                .nickname(nickname)
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
    }

    // car 테이블은 (brand, model, battery_type, ...) unique라 모델명을 다르게
    private Car car(String model, float batteryCapacity) {
        return Car.builder()
                .brand("현대")
                .model(model)
                .batteryType("NCM")
                .modelYear(2024)
                .driveType("2WD") // check: 2WD, 4WD만 허용
                .wheelSize(19)
                .batteryCapacity(batteryCapacity)
                .build();
    }
}
