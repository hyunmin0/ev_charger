package ev_charger.be.user.profileImage;

import ev_charger.be.user.profileImage.dto.response.ProfileImageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock
    private ProfileImageRepository profileImageRepository;

    private ProfileImageService profileImageService;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        profileImageService = new ProfileImageService(profileImageRepository);
    }

    @Test
    void 프로필_이미지_목록_조회_성공() {
        // given
        ProfileImage profileImage = mock(ProfileImage.class);
        given(profileImage.getId()).willReturn(1);
        given(profileImage.getImageUrl()).willReturn("https://image.url/1");
        given(profileImage.getName()).willReturn("profile-1");

        given(profileImageRepository.findAll()).willReturn(List.of(profileImage));

        // when
        List<ProfileImageResponse> responses = profileImageService.getProfileImageList();

        // then
        assertThat(responses).hasSize(1);
        ProfileImageResponse response = responses.get(0);
        assertThat(response.id()).isEqualTo(1);
        assertThat(response.imageUrl()).isEqualTo("https://image.url/1");
        assertThat(response.name()).isEqualTo("profile-1");
    }

    @Test
    void 이미지가_없으면_빈_리스트_반환() {
        // given
        given(profileImageRepository.findAll()).willReturn(List.of());

        // when
        List<ProfileImageResponse> responses = profileImageService.getProfileImageList();

        // then
        assertThat(responses).isEmpty();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
