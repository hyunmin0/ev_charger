package ev_charger.be.service;

import ev_charger.be.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    // 회원가입, 로그인
    public Map<String, String> socialLogin(String accessToken, String provider) {
        /**
        1. 카카오/구글 api 호출 - 정보 조회
        2. providerId로 db에 있는 지 확인
        3. 없으면 회원가입
        4. jwt refresh token을 db에 저장
        5. jwtRefreshToken, jwtAccessToken 반환
         => 나중에 jwtaccesstoken이 만기되면 reissue()를 실행하여 새로운 token을 반환..
         **/
    }

    // 로그아웃
    public void logout() {
        // refreshToken을 null로
    }

    // 닉네임 수정
    public void updateNickname(String newName) {
    }

    // 사진 수정 ****=> 수정하기*******
    public void updateProfile(String newProfile) {
    }

    // 사진 삭제
    public void deleteProfile() {
    }

    public Map<String, String> reissue(String refreshToken) {
        // refreshToken와 db 검증
        // new jwtaccesstoken, new jwtrefreshtoken 생성
        // db에 둘 다 갱신
        // 둘 다 반환

}
