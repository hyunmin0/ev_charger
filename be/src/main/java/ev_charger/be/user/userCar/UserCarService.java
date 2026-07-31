package ev_charger.be.user.userCar;

import ev_charger.be.car.Car;
import ev_charger.be.car.CarRepository;
import ev_charger.be.user.User;
import ev_charger.be.user.userCar.dto.response.UserCarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class UserCarService {
    private final UserCarRepository userCarRepository;
    private final CarRepository carRepository;

    /**
     * 차량 추가
     * @param user
     * @param carId
     * @param batteryCapacity
     */
    @Transactional
    public void addUserCar(User user, long carId, float batteryCapacity) {

        if (batteryCapacity <= 0) {
            throw new IllegalArgumentException("배터리 용량은 0보다 커야 합니다.");
        }

        // car table에서 car 찾기
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 차량입니다."));

        // 이미 등록되 차량인지 확인
        if (userCarRepository.existsByUserAndCar(user, car)) {
            throw new IllegalArgumentException("이미 등록된 차량입니다.");
        }

        // db에 차 저장
        userCarRepository.save(UserCar.builder()
                .user(user)
                .car(car)
                .batteryCapacity(batteryCapacity)
                .build());
    }

    /**
     * 차량 목록
     * @param user
     * @return list(userCarId, carName, batteryCapacity)
     */
    public List<UserCarResponse> getUserCarList(User user) {
        return userCarRepository.findCarByUser(user)
                .stream()
                .map(uc -> new UserCarResponse(
                        uc.getUserCarId(),
                        uc.getCar().getDisplayName(),
                        uc.getBatteryCapacity())
                ).toList();
    }

    /**
     * 차량 삭제
     * @param user
     * @param userCarId
     */
    @Transactional
    public void deleteUserCar(User user, long userCarId) {
        // 검증
        UserCar userCar = userCarRepository.findByUserAndUserCarId(user, userCarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 본인 차량이 아닙니다."));

        userCarRepository.delete(userCar);
    }

}
