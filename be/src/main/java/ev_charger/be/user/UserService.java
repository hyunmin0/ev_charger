package ev_charger.be.user;

import ev_charger.be.user.userCar.UserCarRepository;
import ev_charger.be.user.dto.response.UserResponse;
import ev_charger.be.user.profileImage.ProfileImage;
import ev_charger.be.user.profileImage.ProfileImageRepository;
import ev_charger.be.user.userCar.dto.response.UserCarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true) // 조회를 기본값으로(메모리 사용 감소 등 최적화)
public class UserService {
    private final ProfileImageRepository profileImageRepository;
    private final UserCarRepository userCarRepository;

    /**
     * 닉네임 수정
     * @param user
     * @param newName
     */
    @Transactional
    public void updateNickname(User user, String newName) {
        user.updateNickname(newName);
    }

    /**
     * 프로필 사진 수정
     * @param user
     * @param newProfileImageId 업데이트할 이미지 id
     * @return 수정된 사진 url
     */
    @Transactional
    public String updateProfileImage(User user, Integer newProfileImageId) {
        // profileImage에 해당 id가 있는지 확인
        ProfileImage profileImage = profileImageRepository.findById(newProfileImageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로필 이미지"));

        // users.profileImage 업데이트
        user.updateProfileImage(profileImage);

        // url 반환
        return profileImage.getImageUrl();

    }

    /**
     * 프로필 조회
     * @param user
     * @return nickname, email(없으면 null), profileImageUrl(없으면 null), model(없으면 빈 리스트)
     */
    public UserResponse getProfile(User user) {
        List<UserCarResponse> cars = userCarRepository.findByUser(user)
                .stream()
                .map(uc -> new UserCarResponse(
                        uc.getUserCarId(),
                        uc.getCar().getCarName(),
                        uc.getBatteryCapacity()
                )).toList();
        return new UserResponse(
                user.getNickname(),
                user.getEmail(),
                user.getProfileImage() != null ? user.getProfileImage().getImageUrl():null,
                cars
        );
    }

}
