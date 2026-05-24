package ev_charger.be.user;

import ev_charger.be.user.dto.response.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // 닉네임 수정
    public void updateNickname(String newName) {
    }

    // 사진 수정 ****=> 수정하기*******
    public String updateProfile(int profileImageId) {
        // profileimage에 해당 id가 있는지 확인
        // users.profileImageId에 저장 후 url 값 반환
    }

    public MemberResponse getProfile() {
        // users: nickname, email(있으면)
        // profileImage: users의 profileImageId의 imageUrl
        // usercar: model
    }

}
