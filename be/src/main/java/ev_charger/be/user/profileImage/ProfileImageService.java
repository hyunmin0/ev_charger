package ev_charger.be.user.profileImage;

import ev_charger.be.user.profileImage.dto.response.ProfileImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileImageService {

    private final ProfileImageRepository profileImageRepository;

    // 회원가입 시 선택 가능한 프로필 이미지 전체 목록
    public List<ProfileImageResponse> getProfileImageList() {
        return profileImageRepository.findAll().stream()
                .map(ProfileImageResponse::from)
                .toList();
    }
}
