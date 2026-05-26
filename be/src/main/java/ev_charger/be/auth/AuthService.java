package ev_charger.be.auth;

import ev_charger.be.auth.client.OAuthApiClient;
import ev_charger.be.auth.dto.request.RegisterRequest;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.Provider;
import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import ev_charger.be.user.profileImage.ProfileImage;
import ev_charger.be.user.profileImage.ProfileImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;
    private final ProfileImageRepository profileImageRepository;


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

            // SUCCESS랑 토큰 2개 반환
            return new SocialLoginResponse("SUCCESS", newAccessToken, newRefreshToken, null);

        } else {
            // 신규유저
            // 닉네임을 아직 설정 안했으니까 프로필 설정이 필요하다고 알려줌
            // 임시 토큰: "provider:providerId"
            String tempToken = provider + ":"+ userInfo.id();
            // email이 없으면 "" 입력
            String emailValue = userInfo.email() != null ? userInfo.email() : "";
            // redis에 임시 토큰 저장(키: temp:provider:providerId, 값: emailValue, TTL: 30분) - json 형태
            redisTemplate.opsForValue().set("temp:" + tempToken, emailValue, 30, TimeUnit.MINUTES);
            return new SocialLoginResponse("NEED_PROFILE_SELECT", null, null, tempToken);
        }
    }

    /**
     * 회원가입(db에 저장)
     * @param registerRequest tempToken, nickname, profileimage
     * @return succss, accessToken, refreshToken
     */
    @Transactional
    public SocialLoginResponse register(RegisterRequest registerRequest) {
        // tempToken, email 추출
        String redisKey = "temp:" + registerRequest.tempToken();

        // redis에 있는지 검증
        if(Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
            throw new IllegalArgumentException("유효하지 않은 temToken");
        }

        // email 값 추출
        // 키: temp:tempToken 값: emailValue
        String email = redisTemplate.opsForValue().get(redisKey);
        // email이 없으면 null
        String emailToSave = (email == null || email.isEmpty()) ? null : email;

        // tempToken -> provider, providerId 추출(e.g. "KAKAO:1234")
        String[] parts = registerRequest.tempToken().split(":", 2);
        Provider provider = Provider.valueOf(parts[0]);
        String providerId = parts[1];

        // profileImage 조회
        ProfileImage profileImage = profileImageRepository.findById(registerRequest.profileImageId())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 프로필 이미지"));

        // db에 user 저장
        User user = User.builder()
                .nickname(registerRequest.nickname())
                .profileImage(profileImage)
                .email(emailToSave)
                .provider(provider)
                .providerId(providerId)
                .build();
        // uuid가 생성되기 위해서는 @Transactional로는 안됨. save() 필요
        userRepository.save(user);

        // 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());
        // db refreshToken 업데이트
        user.updateRefreshToken(refreshToken);

        // redis temp키 삭제
        redisTemplate.delete(redisKey);

        return new SocialLoginResponse("SUCCESS",accessToken,refreshToken,null);
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
