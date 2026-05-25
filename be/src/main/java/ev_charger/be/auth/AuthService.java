package ev_charger.be.auth;

import ev_charger.be.auth.dto.response.SocialLoginResponse;
import ev_charger.be.user.Provider;
import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static ev_charger.be.user.Provider.KAKAO;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KakaoApiClient kakaoApiClient; // 카카오 API 호출
    private final JwtProvider jwtProvider; // JWT 토큰 생성 및 검증
    private final UserRepository userRepository; // DB에서 유저 찾기

    // 회원가입, 로그인
    public SocialLoginResponse socialLogin(String accessToken, Provider provider) {
        KakaoApiClient.KakaoUserInfo kakaoUser = kakaoApiClient.getUserInfo(accessToken); // 카카오에서 유저 정보 받아오기 id, email, nickname
        Optional<User>existing = userRepository.findByProviderAndProviderId(provider, KAKAO, kakaoUser.id()) //우리 DB에 이 사람이 이미 있는지 확인 provider랑 카카오에서 받은 id 조합으로 찾음
        if (existing.isPresent()){ // DB에 있으면 기존유저 없으면 신규유저
            //기존 유저
            User user = existing.get();
            String newAccessToken = jwtProvider.generateAccessToken(user.getUserId()); //JWT 토큰 새로 발급
            String newRefreshToken = jwtProvider.generateRefreshToken();
            
            user.updateRefreshToken(newRefreshToken); // DB에 새 refreshToken 저장
            userRepository.save(user); // DB에 저장 

            return SocialLoginResponse.builder() //sucess랑 토큰 2개 반환
                    .status("success")
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } else {
            // 신규유저
            // 닉네임을 아직 설정 안했으니까 프로필 설정이 필요하다고 알려주고 
            // 임시 토큰으로 카카오id를 그냥 사용
            return SocialLoginResponse.builder()
                    .status("NEED_PROFILE_SELECT")
                    .tempToken(kakaoUser.id())
                    .build();
        }
        /**
         1. 카카오/구글 api 호출 - 정보 조회
         2. providerId로 db에 있는지 확인
         3. 있으면 jwt 발급
         반환: success, jwtaccesstoken, jwtrefreshtoken
         => 나중에 jwtaccesstoken이 만기되면 reissue()를 실행하여 새로운 token을 반환..
         4. 없으면 임시 토큰 발급해서 유저 정보를 redis에 임시 서장(만료 시간 10분)
         반환: need_profile_select, temptoken

         **/
    }

    // 로그아웃
    public void logout() {
        // refreshToken을 null로
    }

    public Map<String, String> reissue(String refreshToken) {
        // refreshToken와 db 검증
        // new jwtaccesstoken, new jwtrefreshtoken 생성
        // db에 둘 다 갱신
        // 둘 다 반환
}
