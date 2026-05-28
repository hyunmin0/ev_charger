package ev_charger.be.auth;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService;//실제 로직은 service에서 처리 controller는 요청만 받아서 넘겨줌
    //input: accessToken(String), provider(Provider)
    //output: SocialLoginResponse{ status, jwtAccessToken, jwtRefreshToken, tempToken }
    @postMapping("login") //POST /auth/login으로 요청이 오면 실행
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
    public ResponseEntity<Void> logout() {
            RequestParam String accessToken,
                @RequestParam String refreshToken) {
            authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok().build();
    }
}
