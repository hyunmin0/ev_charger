package ev_charger.be.user.dto.response;

import ev_charger.be.user.userCar.dto.response.UserCarResponse;

import java.util.List;

public record UserResponse (
        String nickname,
        String email,
        String imageUrl,
        List<UserCarResponse> userCars){
}
