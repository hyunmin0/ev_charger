package ev_charger.be.user.userCar;

import ev_charger.be.car.Car;
import ev_charger.be.car.CarRepository;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.userCar.dto.response.UserCarResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCarServiceTest {

    @Mock
    private UserCarRepository userCarRepository;
    @Mock
    private CarRepository carRepository;

    private UserCarService userCarService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        userCarService = new UserCarService(userCarRepository, carRepository);

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
    }

    @Test
    void 차량_추가_성공() {
        // given
        long carId = 1L;
        float batteryCapacity = 77.4f;
        Car car = Car.builder()
                .brand("현대")
                .model("아이오닉5")
                .build();

        given(carRepository.findById(carId)).willReturn(Optional.of(car));
        given(userCarRepository.existsByUserAndCar(user, car)).willReturn(false);

        // when
        userCarService.addUserCar(user, carId, batteryCapacity);

        // then
        ArgumentCaptor<UserCar> captor = ArgumentCaptor.forClass(UserCar.class);
        verify(userCarRepository).save(captor.capture());

        UserCar saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getCar()).isEqualTo(car);
        assertThat(saved.getBatteryCapacity()).isEqualTo(batteryCapacity);
    }

    @Test
    void 배터리_용량이_0이하면_추가시_예외_발생() {
        // given
        long carId = 1L;
        float batteryCapacity = 0f;

        // when & then
        assertThatThrownBy(() -> userCarService.addUserCar(user, carId, batteryCapacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("배터리 용량은 0보다 커야 합니다.");

        verify(carRepository, never()).findById(any());
        verify(userCarRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_차량이면_추가시_예외_발생() {
        // given
        long carId = 1L;
        float batteryCapacity = 77.4f;

        given(carRepository.findById(carId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userCarService.addUserCar(user, carId, batteryCapacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 차량입니다.");

        verify(userCarRepository, never()).save(any());
    }

    @Test
    void 이미_등록된_차량이면_추가시_예외_발생() {
        // given
        long carId = 1L;
        float batteryCapacity = 77.4f;
        Car car = Car.builder()
                .brand("현대")
                .model("아이오닉5")
                .build();

        given(carRepository.findById(carId)).willReturn(Optional.of(car));
        given(userCarRepository.existsByUserAndCar(user, car)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userCarService.addUserCar(user, carId, batteryCapacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 차량입니다.");

        verify(userCarRepository, never()).save(any());
    }

    @Test
    void 차량_목록_조회_성공() {
        // given
        UUID userCarId = UUID.randomUUID();
        Car car = mock(Car.class);
        given(car.getDisplayName()).willReturn("현대 아이오닉5");

        UserCar userCar = mock(UserCar.class);
        given(userCar.getUserCarId()).willReturn(userCarId);
        given(userCar.getCar()).willReturn(car);
        given(userCar.getBatteryCapacity()).willReturn(77.4f);

        given(userCarRepository.findCarByUser(user)).willReturn(List.of(userCar));

        // when
        List<UserCarResponse> responses = userCarService.getUserCarList(user);

        // then
        assertThat(responses).hasSize(1);
        UserCarResponse response = responses.get(0);
        assertThat(response.userCarId()).isEqualTo(userCarId);
        assertThat(response.carName()).isEqualTo("현대 아이오닉5");
        assertThat(response.batteryCapacity()).isEqualTo(77.4f);
    }

    @Test
    void 차량_삭제_성공() {
        // given
        UUID userCarId = UUID.randomUUID();
        UserCar userCar = mock(UserCar.class);

        given(userCarRepository.findByUserAndUserCarId(user, userCarId)).willReturn(Optional.of(userCar));

        // when
        userCarService.deleteUserCar(user, userCarId);

        // then
        verify(userCarRepository).delete(userCar);
    }

    @Test
    void 존재하지_않거나_본인_차량이_아니면_삭제시_예외_발생() {
        // given
        UUID userCarId = UUID.randomUUID();

        given(userCarRepository.findByUserAndUserCarId(user, userCarId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userCarService.deleteUserCar(user, userCarId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않거나 본인 차량이 아닙니다.");

        verify(userCarRepository, never()).delete(any());
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
