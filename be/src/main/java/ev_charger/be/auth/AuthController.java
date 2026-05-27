package ev_charger.be.auth;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService //실제 로직은 service에서 처리 controller는 요청만 받아서 넘겨줌
    @postMapping("login") //POST /auth/login으로 요청이 오면 실행
    public ResponseEnity<SocialLoginResponse>login(
            @RequestParam String accessToken,
            @RequestParam String provider
    ){
        return
                ResponseEnity.ok(authService.socialLogin(accessToken, provider)) //service에서 처리 결과를 200 OK 응답으로 반환
    }
}
