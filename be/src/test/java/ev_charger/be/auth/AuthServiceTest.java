package ev_charger.be.auth;

import ev_charger.be.auth.client.OAuthApiClient;
import ev_charger.be.auth.dto.request.RegisterRequest;
import ev_charger.be.auth.dto.response.ReissueResponse;
import ev_charger.be.auth.dto.response.SocialLoginResponse;
import ev_charger.be.auth.dto.response.TempUserInfo;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.auth.redis.RedisKeys;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.profileImage.ProfileImage;
import ev_charger.be.user.profileImage.ProfileImageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// 선언하면 테스트에 필요한 가짜 객체들을 자동으로 세팅
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private OAuthApiClient oAuthApiClient;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ProfileImageRepository profileImageRepository;
    @Mock
    private ObjectMapper objectMapper;

    // 테스트하려는 대상
    private AuthService authService;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        authService = new AuthService(
                List.of(oAuthApiClient),
                jwtProvider,
                userRepository,
                redisTemplate,
                profileImageRepository,
                objectMapper
        );
    }

    @Test
    void 기존_유저면_로그인_성공() {
        // given
        Provider provider = Provider.GOOGLE;
        String accessToken = "accessToken";
        UserInfo userInfo = new UserInfo("google-sub", "test@gmail.com");
        User existingUser = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(provider)
                .providerId("google-sub")
                .build();

        given(oAuthApiClient.getProvider()).willReturn(provider);
        given(oAuthApiClient.getUserInfo(accessToken)).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderId(provider,userInfo.id())).willReturn(Optional.of(existingUser));
        given(jwtProvider.generateAccessToken(any())).willReturn("new-access-token");
        given(jwtProvider.generateRefreshToken(any())).willReturn("new-refresh-token");

        // when
        SocialLoginResponse response = authService.socialLogin(accessToken, provider);

        // then
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(existingUser.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 신규_유저면_프로필_설정이_필요() {
        // given
        Provider provider = Provider.GOOGLE;
        String accessToken = "accessToken";
        UserInfo userInfo = new UserInfo("google-sub", "test@gmail.com");
        TempUserInfo temp = new TempUserInfo(provider, userInfo.id(), userInfo.email());
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(oAuthApiClient.getProvider()).willReturn(provider);
        given(oAuthApiClient.getUserInfo(accessToken)).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderId(provider,userInfo.id())).willReturn(Optional.empty());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(objectMapper.writeValueAsString(temp)).willReturn("{\"provider\":\"GOOGLE\",\"providerId\":\"google-sub\",\"email\":\"test@gmail.com\"}");

        // when
        SocialLoginResponse response = authService.socialLogin(accessToken, provider);

        // then
        assertThat(response.status()).isEqualTo("NEED_PROFILE_SELECT");
        assertThat(response.accessToken()).isNull();
        assertThat(response.refreshToken()).isNull();
        assertThat(response.tempToken()).isNotNull();
    }

    @Test
    void 정상_회원가입이면_성공() {
        // given
        String tempToken = UUID.randomUUID().toString();
        RegisterRequest request = new RegisterRequest(
                tempToken,
                "nick",
                1
        );
        Provider provider = Provider.GOOGLE;
        UserInfo userInfo = new UserInfo("google-sub", "test@gmail.com");
        TempUserInfo temp = new TempUserInfo(provider, userInfo.id(), userInfo.email());
        ProfileImage profileImage = ProfileImage.builder()
                .imageUrl("http://example.com/img.png")
                .name("기본이미지")
                .build();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(RedisKeys.tempUser(tempToken))).willReturn("{\"provider\":\"GOOGLE\",\"providerId\":\"google-sub\",\"email\":\"test@gmail.com\"}");
        given(objectMapper.readValue(anyString(), eq(TempUserInfo.class))).willReturn(temp);
        given(userRepository.existsByProviderAndProviderId(temp.provider(),temp.providerId())).willReturn(false);
        given(profileImageRepository.findById(request.profileImageId())).willReturn(Optional.of(profileImage));
        given(jwtProvider.generateRefreshToken(any())).willReturn("new-refresh-token");
        given(jwtProvider.generateAccessToken(any())).willReturn("new-access-token");

        // when
        SocialLoginResponse response = authService.register(request);

        // then
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void tempToken이_유효하지_않으면_예외_발생() {
        // given
        String tempToken = UUID.randomUUID().toString();
        RegisterRequest request = new RegisterRequest(
                tempToken,
                "nick",
                1
        );
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when & then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 tempToken");
    }

    @Test
    @SuppressWarnings("unchecked")
    void tempToken_데이터가_손상되면_예외_발생() {
        // given
        String tempToken = UUID.randomUUID().toString();
        RegisterRequest request = new RegisterRequest(
                tempToken,
                "nick",
                1
        );
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(RedisKeys.tempUser(tempToken))).willReturn("broken-json");
        given(objectMapper.readValue(anyString(), eq(TempUserInfo.class))).willThrow(new RuntimeException("파싱 실패"));

        // when & then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("tempToken 데이터가 손상되었습니다.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 이미_가입된_유저면_예외_발생() {
        // given
        String tempToken = UUID.randomUUID().toString();
        RegisterRequest request = new RegisterRequest(
                tempToken,
                "nick",
                1
        );
        Provider provider = Provider.GOOGLE;
        UserInfo userInfo = new UserInfo("google-sub", "test@gmail.com");
        TempUserInfo temp = new TempUserInfo(provider, userInfo.id(), userInfo.email());
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(RedisKeys.tempUser(tempToken))).willReturn("{\"provider\":\"GOOGLE\",\"providerId\":\"google-sub\",\"email\":\"test@gmail.com\"}");
        given(objectMapper.readValue(anyString(), eq(TempUserInfo.class))).willReturn(temp);
        given(userRepository.existsByProviderAndProviderId(temp.provider(),temp.providerId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 가입된 사용자 입니다.");
    }

    @Test
    void 존재하지_않는_프로필이미지면_예외_발생() {
        // given
        String tempToken = UUID.randomUUID().toString();
        RegisterRequest request = new RegisterRequest(
                tempToken,
                "nick",
                1
        );
        Provider provider = Provider.GOOGLE;
        UserInfo userInfo = new UserInfo("google-sub", "test@gmail.com");
        TempUserInfo temp = new TempUserInfo(provider, userInfo.id(), userInfo.email());

        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(RedisKeys.tempUser(tempToken))).willReturn("{\"provider\":\"GOOGLE\",\"providerId\":\"google-sub\",\"email\":\"test@gmail.com\"}");
        given(objectMapper.readValue(anyString(), eq(TempUserInfo.class))).willReturn(temp);
        given(userRepository.existsByProviderAndProviderId(temp.provider(),temp.providerId())).willReturn(false);
        given(profileImageRepository.findById(request.profileImageId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 프로필 이미지");

    }

    @Test
    void 로그아웃하면_refreshToken이_null로_갱신되고_블랙리스트에_등록() {
        // given
        long remaining = 60000L;
        String refreshToken = UUID.randomUUID().toString();
        String accessToken = UUID.randomUUID().toString();
        User user = User.builder()
                .refreshToken(refreshToken)
                .build();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        given(userRepository.findByRefreshToken(refreshToken)).willReturn(Optional.of(user));
        given(jwtProvider.getAccessExpiration(accessToken)).willReturn(remaining);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.logout(accessToken, refreshToken);

        // then
        assertThat(user.getRefreshToken()).isNull();
        verify(valueOperations).set(
                RedisKeys.blacklist(accessToken),
                "logout",
                remaining,
                TimeUnit.MILLISECONDS);
    }

    @Test
    void 로그아웃_도중_유효하지_않은_refreshToken이면_예외_발생() {
        // given
        String refreshToken = UUID.randomUUID().toString();
        String accessToken = UUID.randomUUID().toString();

        // when & then
        assertThatThrownBy(() -> authService.logout(accessToken, refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 refresh token");
    }

    @Test
    void 토큰_재발급_성공() {
        // given
        String refreshToken = UUID.randomUUID().toString();
        User user = User.builder()
                        .refreshToken(refreshToken)
                        .build();

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(userRepository.findByRefreshToken(refreshToken)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(any())).willReturn("new-access-token");
        given(jwtProvider.generateRefreshToken(any())).willReturn("new-refresh-token");

        // when
        ReissueResponse response = authService.reissue(refreshToken);

        // then
        assertThat(response.newAccessToken()).isNotNull();
        assertThat(response.newRefreshToken()).isNotEqualTo(refreshToken);

    }

    @Test
    void 토큰_재발급_중_유효하지_않은_토큰이면_예외_발생() {
        // given
        String refreshToken = UUID.randomUUID().toString();

        given(jwtProvider.validateToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 refresh token");
    }

    @Test
    void 토큰_재발급_중_refreshToken으로_유저를_못_찾으면_예외_발생() {
        // given
        String refreshToken = UUID.randomUUID().toString();

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(userRepository.findByRefreshToken(refreshToken)).willReturn((Optional.empty()));

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 refresh token");
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과 시간: " + (System.currentTimeMillis()-startTime) + "ms");
    }
}
