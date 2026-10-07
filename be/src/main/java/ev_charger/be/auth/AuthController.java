package ev_charger.be.auth;

import ev_charger.be.auth.dto.request.KakaoCodeLoginRequest;
import ev_charger.be.auth.dto.request.LogoutRequest;
import ev_charger.be.auth.dto.request.RegisterRequest;
import ev_charger.be.auth.dto.request.ReissueRequest;
import ev_charger.be.auth.dto.request.SocialLoginRequest;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService; //실제 로직은 service에서 처리 controller는 요청만 받아서 넘겨줌

    //input: SocialLoginRequest { token(카카오: accessToken, 구글: idToken), provider }
    //output: SocialLoginResponse{ status, jwtAccessToken, jwtRefreshToken, tempToken }
    // 토큰은 서버/프록시 로그에 남지 않도록 쿼리스트링이 아닌 body로 받음
    @PostMapping("/login") //POST /auth/login으로 요청이 오면 실행
    public ResponseEntity<SocialLoginResponse> login(
            @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.socialLogin(request.token(), request.provider())); //service에서 처리 결과를 200 OK 응답으로 반환
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
    // input : Authorization: Bearer {accessToken} 헤더, LogoutRequest { refreshToken }
    // output: 없음 (200 OK)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody LogoutRequest request) {
        if (!authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 access token");
        }
        authService.logout(authorization.substring(7), request.refreshToken());
        return ResponseEntity.ok().build();
    }
    //토큰 재발급
    // input : ReissueRequest { refreshToken }
    // output: ReissueResponse { jwtAccessToken, jwtRefreshToken }
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(
            @Valid @RequestBody ReissueRequest request) {
        return ResponseEntity.ok(authService.reissue(request.refreshToken()));
    }

    // 카카오 인가코드로 로그인 (WebView redirect에서 받은 code)
    // input : KakaoCodeLoginRequest { code } - 카카오 인가코드
    // output: SocialLoginResponse { status, jwtAccessToken, jwtRefreshToken, tempToken }
    @PostMapping("/login/kakao/code")
    public ResponseEntity<SocialLoginResponse> kakaoCodeLogin(
            @Valid @RequestBody KakaoCodeLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoCodeLogin(request.code()));
    }
}