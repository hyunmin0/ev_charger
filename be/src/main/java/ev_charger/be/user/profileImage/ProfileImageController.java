package ev_charger.be.user.profileImage;

import ev_charger.be.user.profileImage.dto.response.ProfileImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/profile-images")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    // 회원가입 화면에서 고를 프로필 이미지 목록 (로그인 전이라 인증 불필요)
    // output: List<ProfileImageResponse> { id, imageUrl, name }
    @GetMapping
    public ResponseEntity<List<ProfileImageResponse>> getProfileImageList() {
        return ResponseEntity.ok(profileImageService.getProfileImageList());
    }
}
