package ev_charger.be.auth;

import ev_charger.be.auth.dto.response.SocialLoginResponse;

import java.util.Map;

public class AuthService {

    // 회원가입, 로그인
    public SocialLoginResponse socialLogin(String accessToken, String provider) {
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
