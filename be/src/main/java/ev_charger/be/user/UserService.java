package ev_charger.be.user;

import ev_charger.be.charger_alert.ChargerAlertRepository;
import ev_charger.be.review.ReviewRepository;
import ev_charger.be.user.userCar.UserCarRepository;
import ev_charger.be.user.dto.response.UserResponse;
import ev_charger.be.user.profileImage.ProfileImage;
import ev_charger.be.user.profileImage.ProfileImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true) // 조회를 기본값으로(메모리 사용 감소 등 최적화)
public class UserService {
    private final UserRepository userRepository;
    private final ProfileImageRepository profileImageRepository;
    private final UserCarRepository userCarRepository;
    private final ReviewRepository reviewRepository;
    private final ChargerAlertRepository chargerAlertRepository;

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
     * @return nickname, email(없으면 null), profileImageUrl(없으면 null), 내 차량 수, 즐겨찾기 수, 충전기 알림 수
     */
    public UserResponse getProfile(User user) {
        // 인증 필터에서 조회된 user는 이미 세션이 닫힌 detached 상태라
        // LAZY 연관관계(profileImage)에 접근하려면 현재 트랜잭션 안에서 다시 조회해야 함
        User attachedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

        return new UserResponse(
                attachedUser.getNickname(),
                attachedUser.getEmail(),
                attachedUser.getProfileImage() != null ? attachedUser.getProfileImage().getImageUrl():null,
                userCarRepository.countByUser(attachedUser),
                reviewRepository.countByUser(attachedUser),
                chargerAlertRepository.countByUser(attachedUser)
        );
    }
}
