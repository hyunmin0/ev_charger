package ev_charger.be.user;

import ev_charger.be.charger_alert.ChargerAlertRepository;
import ev_charger.be.review.ReviewRepository;
import ev_charger.be.user.dto.response.UserResponse;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.profileImage.ProfileImage;
import ev_charger.be.user.profileImage.ProfileImageRepository;
import ev_charger.be.user.userCar.UserCarRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileImageRepository profileImageRepository;
    @Mock
    private UserCarRepository userCarRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ChargerAlertRepository chargerAlertRepository;

    private UserService userService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        userService = new UserService(
                userRepository,
                profileImageRepository,
                userCarRepository,
                reviewRepository,
                chargerAlertRepository
        );

        // userId는 @GeneratedValue라 builder로는 못 채우고, DB가 넣어준 것처럼 실제 값이 필요한 케이스(getProfile 등)는
        // Mockito.mock(User.class) + given(user.getUserId())로 UUID를 직접 지정해서 쓰면 된다.
        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
    }

    @Test
    void 닉네임_수정_성공() {
        // given
        String newName = "새이름";

        // when
        userService.updateNickname(user, newName);

        // then
        assertThat(user.getNickname()).isEqualTo(newName);
    }

    @Test
    void 프로필_사진_수정_성공() {
        // given
        int newProfileImageId = 2;
        ProfileImage profileImage = ProfileImage.builder()
                .imageUrl("https://image.url/2")
                .name("profile-2")
                .build();
        given(profileImageRepository.findById(newProfileImageId)).willReturn(Optional.of(profileImage));

        // when
        String url = userService.updateProfileImage(user, newProfileImageId);

        // then
        assertThat(url).isEqualTo("https://image.url/2");
    }

    @Test
    void 존재하지_않는_프로필이미지면_수정시_예외_발생() {
        // given
        int newProfileImageId = 2;

        // when & then
        assertThatThrownBy(() -> userService.updateProfileImage(user, newProfileImageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 프로필 이미지");
    }

    @Test
    void 프로필_조회_성공() {
        // given
        given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
        given(userCarRepository.countByUser(user)).willReturn(2);
        given(reviewRepository.countByUser(user)).willReturn(3);
        given(chargerAlertRepository.countByUser(user)).willReturn(1);

        // when
        UserResponse response = userService.getProfile(user);

        // then
        assertThat(response.nickname()).isEqualTo("테스터");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.imageUrl()).isNull(); // 샘플 user는 profileImage를 안 넣어서 null
        assertThat(response.userCarCount()).isEqualTo(2);
        assertThat(response.reviewCount()).isEqualTo(3);
        assertThat(response.chargerAlertCount()).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_유저면_조회시_예외_발생() {
        // given
        given(userRepository.findById(user.getUserId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getProfile(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
