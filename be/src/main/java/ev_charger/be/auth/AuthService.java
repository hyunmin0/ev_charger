package ev_charger.be.auth;

import ev_charger.be.auth.client.OAuthApiClient;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import ev_charger.be.auth.client.KakaoApiClient;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.Provider;
import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final List<OAuthApiClient> apiClients; // API 호출
    private final JwtProvider jwtProvider; // JWT 토큰 생성 및 검증
    private final UserRepository userRepository; // DB에서 유저 찾기
    private final RedisTemplate<String, String> redisTemplate;


    /**
     * 회원가입 및 로그인
     * @param accessToken 로그인할 토큰
     * @param provider 카카오/구글
     * @return
     */
    @Transactional
    public SocialLoginResponse socialLogin(String accessToken, Provider provider) {

        // Google/Kakao 확인
        OAuthApiClient client = apiClients.stream() // list 순회
                .filter(c -> c.getProvider() == provider)
                .findFirst() // 첫 번쨰 일치하는 클라이언트 반환(Optional)
                .orElseThrow(); // 없으면 예외처리

        UserInfo userInfo = client.getUserInfo(accessToken); // 유저 정보 받아오기 id, email, nickname

        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, userInfo.id()); //우리 DB에 이 사람이 이미 있는지 확인 provider랑 id 조합으로 찾음
        if (existing.isPresent()){ // DB에 있으면 기존유저 없으면 신규유저
            //기존 유저
            User user = existing.get();
            String newAccessToken = jwtProvider.generateAccessToken(user.getUserId()); //JWT 토큰 새로 발급
            String newRefreshToken = jwtProvider.generateRefreshToken(user.getUserId());

            user.updateRefreshToken(newRefreshToken); // DB에 새 refreshToken 저장

            return SocialLoginResponse.builder() // SUCCESS랑 토큰 2개 반환
                    .status("SUCCESS")
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } else {
            // 신규유저
            // 닉네임을 아직 설정 안했으니까 프로필 설정이 필요하다고 알려주고
            // 임시 토큰으로 카카오id를 그냥 사용
            return SocialLoginResponse.builder()
                    .status("NEED_PROFILE_SELECT")
                    .tempToken(userInfo.id())
                    .build();
        }
    }

    /**
     * 로그아웃
     * @param accessToken
     * @param refreshToken
     */
    @Transactional // db 저장 중 오류 나면 롤백
    public void logout(String accessToken, String refreshToken) {
        // refreshToken으로 db에서 유저 조회
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 refresh token"));
        // null로 업데이트
        user.updateRefreshToken(null);

        // accessToken의 만료시간 계산
        long remaining = jwtProvider.getAccessExpiration(accessToken);
        // redis blacklist에 accessToken 등록
        if (remaining > 0) {
            redisTemplate.opsForValue()
                    // blacklist를 토큰값으로 구분
                    // remaining: TTL 값, TimeUnit.MILLISECONDS: TTL 단뒤(밀리초)
                    // 즉, remaining이 0이 되면 자동 삭제
                    .set("blacklist:" + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);
        }


    }

    /**
     * accessToken, refreshToken 재발급
     * @param refreshToken accessToken 발급용 refreshToken
     * @return newAccessToken, newRefreshToken
     */
    @Transactional // db 저장 중 오류 나면 롤백
    public ReissueResponse reissue(String refreshToken) {
        // 서명/만료 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 refresh token");
        }

        // db에서 유저 조회
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 refresh token"));

        // 새 access token 발급
        String newAccessToken = jwtProvider.generateAccessToken(user.getUserId());
        // 새 refresh token 발급(refresh token rotation)
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getUserId());

        // db 갱신
        user.updateRefreshToken(newRefreshToken);

        // 새 access token + refresh token 반환
        return new ReissueResponse(newAccessToken, newRefreshToken);
    }

}
