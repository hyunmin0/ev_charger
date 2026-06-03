package ev_charger.be.auth;

import ev_charger.be.auth.dto.request.RegisterRequest;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;

import ev_charger.be.user.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService;//실제 로직은 service에서 처리 controller는 요청만 받아서 넘겨줌
    //input: accessToken(String), provider(Provider)
    //output: SocialLoginResponse{ status, jwtAccessToken, jwtRefreshToken, tempToken }
    @PostMapping("/login") //POST /auth/login으로 요청이 오면 실행
    public ResponseEntity<SocialLoginResponse> login(
            @RequestParam String accessToken,
            @RequestParam Provider provider) {
        return ResponseEntity.ok(authService.socialLogin(accessToken, provider)); //service에서 처리 결과를 200 OK 응답으로 반환
    }
    //회원가입
    // input : RegisterRequest { tempToken, nickname, profileImageId }
    // output: SocialLoginResponse { status: SUCCESS, jwtAccessToken, jwtRefreshToken }
    @PostMapping("/register")
    public ResponseEntity<SocialLoginResponse> register(
            @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    //로그아웃
    // input : accessToken(String), refreshToken(String)
    // output: 없음 (200 OK)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestParam String accessToken,
            @RequestParam String refreshToken) {
            authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok().build();
    }
    //토큰 재발급
    // input : refreshToken(String)
    // output: ReissueResponse { jwtAccessToken, jwtRefreshToken }
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(
            @RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.reissue(refreshToken));
    }
}
