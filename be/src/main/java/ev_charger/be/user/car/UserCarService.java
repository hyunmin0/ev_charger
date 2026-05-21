package ev_charger.be.user.car;

import ev_charger.be.user.car.dto.response.UserCarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCarService {
    private final UserCarRepository userCarRepository;

    // 차 추가
    public UUID addUserCar(String model, float batteryCapacity) {
        // jwt에서 userid를 꺼내서 쓰기
        // db에 차 저장
        // carid 반환 - 삭제할 때 바로 사용 가능해서 유용함
    }

    // 차 목록
    public List<UserCarResponse> getUserCarList() {
         // jwt에서 userid 가져와서 db에서 리스트업
        // return model, batterCapacity, carid(차 삭제할 때 필요)
    }

    // 차 삭제
    public void deleteUserCar(UUID carid) {
        // carid 차가 현재 user의 차가 맞는지 확인
        // 이후 삭제
    }

}
