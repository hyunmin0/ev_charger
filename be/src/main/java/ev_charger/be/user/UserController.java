package ev_charger.be.user;

import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.user.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 프로필 조회
    // input : 헤더에 jwtAccessToken
    // output: UserResponse { nickname, email, imageUrl, cars }
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) { //로그인한 유저 자동으로 받아옴
        return ResponseEntity.ok(userService.getProfile(userDetails.getUser())); //현재 로그인한 유저 꺼내서 넘겨줌
    }

    // 닉네임 수정
    // input : newName(String)
    // output: 없음 (200 OK)
    @PatchMapping("/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails, //로그인한 유저
            @RequestParam String newName) { // url에서 새 닉네임 받음
        userService.updateNickname(userDetails.getUser(), newName);
        return ResponseEntity.ok().build();
    }
    // 프로필 사진 수정
    @PatchMapping("/profile-image")
    public ResponseEntity<String> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @RequestParam Integer profileImageId) { // url에서 이미지 id 받음
        return ResponseEntity.ok(userService.updateProfileImage(userDetails.getUser(), profileImageId));
    }
}