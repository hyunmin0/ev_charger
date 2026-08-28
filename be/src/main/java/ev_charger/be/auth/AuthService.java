package ev_charger.be.auth;

import ev_charger.be.auth.client.OAuthApiClient;
import ev_charger.be.auth.dto.request.RegisterRequest;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import ev_charger.be.auth.dto.response.TempUserInfo;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.auth.redis.RedisKeys;
import ev_charger.be.auth.redis.RedisTtl;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import ev_charger.be.user.profileImage.ProfileImage;
import ev_charger.be.user.profileImage.ProfileImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true) // 조회를 기본값으로(메모리 사용 감소 등 최적화)
public class AuthService {
    private final List<OAuthApiClient> apiClients; // API 호출
    private final JwtProvider jwtProvider; // JWT 토큰 생성 및 검증
    private final UserRepository userRepository; // DB에서 유저 찾기
    private final RedisTemplate<String, String> redisTemplate;
    private final ProfileImageRepository profileImageRepository;
    private final ObjectMapper objectMapper;

    @Value("${kakao.token-url}")
    private String kakaoTokenUrl;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;


    /**
     * 카카오 인가 코드로 로그인 (프론트에서 code만 보내면 백엔드가 토큰 교환)
     * @param code 카카오 인가 코드
     */
    @Transactional
    public SocialLoginResponse kakaoCodeLogin(String code) {
        // 1) 카카오에 code → access token 교환
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoRestApiKey);
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", code);

        Map<String, Object> tokenResponse = restTemplate.postForObject(
                kakaoTokenUrl, new HttpEntity<>(body, headers), Map.class);

        String accessToken = (String) tokenResponse.get("access_token");

        // 2) 기존 로그인 로직 재사용
        return socialLogin(accessToken, Provider.KAKAO);
    }

    /**
     * 회원가입 및 로그인
     * @param accessToken 로그인할 토큰
     * @param provider 카카오/구글
     */
    @Transactional
    public SocialLoginResponse socialLogin(String accessToken, Provider provider) {

        // Google/Kakao 확인
        OAuthApiClient client = apiClients.stream() // list 순회
                .filter(c -> c.getProvider() == provider)
                .findFirst() // 첫 번쨰 일치하는 클라이언트 반환(Optional)
                .orElseThrow(() ->  new IllegalArgumentException("지원하지 않는 provider")); // 없으면 예외처리

        UserInfo userInfo = client.getUserInfo(accessToken); // 유저 정보 받아오기 id, email

        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, userInfo.id()); //우리 DB에 이 사람이 이미 있는지 확인 provider랑 id 조합으로 찾음
        if (existing.isPresent()){ // DB에 있으면 기존유저 없으면 신규유저
            //기존 유저
            User user = existing.get();
            String newAccessToken = jwtProvider.generateAccessToken(user.getUserId()); //JWT 토큰 새로 발급
            String newRefreshToken = jwtProvider.generateRefreshToken(user.getUserId());

            user.updateRefreshToken(newRefreshToken); // DB에 새 refreshToken 저장

            // SUCCESS랑 토큰 2개 반환
            return new SocialLoginResponse("SUCCESS",
                    newAccessToken,
                    newRefreshToken,
                    null);

        } else {
            // 신규유저
            // 닉네임을 아직 설정 안했으니까 프로필 설정이 필요하다고 알려줌

            // 임시 토큰: UUID 생성
            String tempToken = UUID.randomUUID().toString();

            TempUserInfo temp = new TempUserInfo(provider, userInfo.id(), userInfo.email());

            // redis에 임시 저장
            // 키: tempToken
            // 값: temp(provider, providerId, email)
            // TTL: TEMP_USER_MINUTES(=30분)
            redisTemplate.opsForValue().set(
                    RedisKeys.tempUser(tempToken),
                    objectMapper.writeValueAsString(temp),
                    RedisTtl.TEMP_USER_MINUTES,
                    TimeUnit.MINUTES);

            return new SocialLoginResponse("NEED_PROFILE_SELECT", null, null, tempToken);
        }
    }

    /**
     * 회원가입(db에 저장)
     * @param registerRequest tempToken, nickname, profileImageId
     * @return success, accessToken, refreshToken
     */
    @Transactional
    public SocialLoginResponse register(RegisterRequest registerRequest) {
        // tempUser의 key
        String redisKey = RedisKeys.tempUser(registerRequest.tempToken());

        // redis에서 값 추출
        String json = Optional.ofNullable(redisTemplate.opsForValue().get(redisKey))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 tempToken")); // 없으면 예외처리

        // TempUserInfo 타입으로 변환
        TempUserInfo temp;
        try {
            temp = objectMapper.readValue(json, TempUserInfo.class);
        } catch (Exception e) {
            throw new IllegalStateException("tempToken 데이터가 손상되었습니다.");
        }

        // email 값 추출(없으면 null)
        String email = (temp.email() == null || temp.email().isBlank()) ? null : temp.email();

        // 중복 확인
        if (userRepository.existsByProviderAndProviderId(temp.provider(),temp.providerId())) {
            throw new IllegalStateException("이미 가입된 사용자 입니다.");
        }

        // profileImage 조회
        ProfileImage profileImage = profileImageRepository.findById(registerRequest.profileImageId())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 프로필 이미지"));

        // db에 user 저장
        User user = User.builder()
                .nickname(registerRequest.nickname())
                .profileImage(profileImage)
                .email(email)
                .provider(temp.provider())
                .providerId(temp.providerId())
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
                    .set(RedisKeys.blacklist(accessToken),
                            "logout",
                            remaining,
                            TimeUnit.MILLISECONDS);
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